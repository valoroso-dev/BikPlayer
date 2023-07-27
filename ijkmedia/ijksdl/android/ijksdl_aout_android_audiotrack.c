/*****************************************************************************
 * ijksdl_aout_android_audiotrack.c
 *****************************************************************************
 *
 * Copyright (c) 2013 Bilibili
 * copyright (c) 2013 Zhang Rui <bbcallen@gmail.com>
 *
 * This file is part of ijkPlayer.
 *
 * ijkPlayer is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * ijkPlayer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ijkPlayer; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include "ijksdl_aout_android_audiotrack.h"

#include <stdbool.h>
#include <assert.h>
#include <jni.h>
#include "../ijksdl_inc_internal.h"
#include "../ijksdl_thread.h"
#include "../ijksdl_aout_internal.h"
#include "ijksdl_android_jni.h"
#include "android_audiotrack.h"

#ifdef SDLTRACE
#undef SDLTRACE
#define SDLTRACE(...)
//#define SDLTRACE ALOGE
#endif

static SDL_Class g_audiotrack_class = {
    .name = "AudioTrack",
};

typedef struct SDL_Aout_Opaque {
    SDL_cond *wakeup_cond;
    SDL_mutex *wakeup_mutex;

    SDL_AudioSpec spec;
    SDL_Android_AudioTrack* atrack;
    uint8_t *buffer;
    int buffer_size;

    volatile bool need_flush;
    volatile bool pause_on;
    volatile bool abort_request;

    volatile bool need_set_volume;
    volatile float left_volume;
    volatile float right_volume;

    SDL_Thread *audio_tid;
    SDL_Thread _audio_tid;

    int audio_session_id;

    volatile float speed;
    volatile bool speed_changed;
    int write_policy;
} SDL_Aout_Opaque;

int sliding_window(long value)
{
    static long buff[10] = {0};
    static int headpos = 0;
    static int tailpos = 0;
    tailpos = tailpos == 9 ? 0 : tailpos+1;
    buff[tailpos] = value;
    if(tailpos == headpos) {
        headpos = headpos == 9 ? 0 :headpos+1;
    }
    if(buff[tailpos] && buff[headpos] && buff[tailpos]-buff[headpos]<60*1000) {
        return 1;
    }
    return 0;
}
static int aout_thread_n(JNIEnv *env, SDL_Aout *aout)
{
    SDL_Aout_Opaque *opaque = aout->opaque;
    SDL_Android_AudioTrack *atrack = opaque->atrack;
    SDL_AudioCallback audio_cblk = opaque->spec.callback;
    void *userdata = opaque->spec.userdata;
    uint8_t *buffer = opaque->buffer;
    int copy_size = 256;
    SDL_cond *write_con = SDL_CreateCond();
    SDL_mutex *write_mutex = SDL_CreateMutex();

    // original timeout
    int write_timeout = opaque->write_policy>=10 ? opaque->write_policy : 30;
    assert(atrack);
    assert(buffer);

    SDL_SetThreadPriority(SDL_THREAD_PRIORITY_HIGH);

    if (!opaque->abort_request && !opaque->pause_on)
        SDL_Android_AudioTrack_play(env, atrack);

    while (!opaque->abort_request) {
        long cycle_begin = (long)SDL_GetTickHR();
        SDL_LockMutex(opaque->wakeup_mutex);
        if (!opaque->abort_request && opaque->pause_on) {
            SDL_Android_AudioTrack_pause(env, atrack);
            while (!opaque->abort_request && opaque->pause_on) {
                ALOGE("Rapid aout_thread_n pause_on %d", opaque->pause_on);
                SDL_CondWaitTimeout(opaque->wakeup_cond, opaque->wakeup_mutex, 1000);
            }
            if (!opaque->abort_request && !opaque->pause_on) {
                if (opaque->need_flush) {
                    opaque->need_flush = 0;
                    SDL_Android_AudioTrack_flush(env, atrack);
                }
                SDL_Android_AudioTrack_play(env, atrack);
            }
        }
        if (opaque->need_flush) {
            opaque->need_flush = 0;
            SDL_Android_AudioTrack_flush(env, atrack);
        }
        if (opaque->need_set_volume) {
            opaque->need_set_volume = 0;
            SDL_Android_AudioTrack_set_volume(env, atrack, opaque->left_volume, opaque->right_volume);
        }
        if (opaque->speed_changed) {
            opaque->speed_changed = 0;
            SDL_Android_AudioTrack_setSpeed(env, atrack, opaque->speed);
        }
        SDL_UnlockMutex(opaque->wakeup_mutex);

        audio_cblk(userdata, buffer, copy_size);
        if (opaque->need_flush) {
            SDL_Android_AudioTrack_flush(env, atrack);
            opaque->need_flush = false;
        }

        if (opaque->need_flush) {
            opaque->need_flush = 0;
            SDL_Android_AudioTrack_flush(env, atrack);
        } else {
            long write_begin = (long)SDL_GetTickHR();
            int written = 0;
            // write data according to policy
            /*
            policy value range [0,100]
            meaning:
            0:blocking mode,ref to Andorid doc
            1:non-blocking mode,cyclic write until the data package is written.
            2:non-blocking with timeout ,cyclic write with timeout ,the data package will be written or discard
              bytime out. the timeout has a 30ms initial value. And if timeout occurs 10 times a minute, the value
              will enlarge 5ms everytime.
            3~9: reversed.
            10~100: Specified timeout, ms.
            */
            // blocking
            if (opaque->write_policy == 0) {
                written = SDL_Android_AudioTrack_write(env, atrack, buffer, copy_size, 0);
            } else { //non-blocking
                while (written != copy_size)
                {
                    int ret = SDL_Android_AudioTrack_write(env, atrack, buffer + written, copy_size - written, 1);
                    if (ret < 0) {
                        ALOGW("AudioTrack: written fail  %d/", ret);
                        break;
                    }
                    if (opaque->abort_request) {
                        ALOGW("AudioTrack: discard %d for abort request", copy_size - written);
                        break;
                    }
                    written += ret;
                    if (written == copy_size) {
                        break;
                    }
                    if (opaque->write_policy == 1) {
                        // write until finish or error
                    } else {
                        long currenttick = SDL_GetTickHR();
                        if (currenttick - write_begin > write_timeout) {
                            ALOGW("AudioTrack: discard %d for time out", copy_size - written);
                            if(opaque->write_policy == 2 && sliding_window(currenttick)) {
                                ALOGW("AudioTrack: change time out from %d ms to %d ms", write_timeout, write_timeout + 5);
                                write_timeout+=5;
                            }
                            break;
                        }
                    }
                    SDL_CondWaitTimeout(write_con, write_mutex, 10);
                    continue;
                }
            }
            if (written != copy_size) {
                ALOGD("AudioTrack: write particial data %d spend %ld ", written, SDL_GetTickHR() - cycle_begin);
            }
        }

        // TODO: 1 if callback return -1 or 0
    }

    SDL_Android_AudioTrack_free(env, atrack);
    return 0;
}

static int aout_thread(void *arg)
{
    SDL_Aout *aout = arg;
    // SDL_Aout_Opaque *opaque = aout->opaque;
    JNIEnv *env = NULL;

    if (JNI_OK != SDL_JNI_SetupThreadEnv(&env)) {
        ALOGE("aout_thread: SDL_AndroidJni_SetupEnv: failed");
        return -1;
    }

    return aout_thread_n(env, aout);
}

static int aout_open_audio_n(JNIEnv *env, SDL_Aout *aout, const SDL_AudioSpec *desired, SDL_AudioSpec *obtained)
{
    assert(desired);
    SDL_Aout_Opaque *opaque = aout->opaque;

    opaque->spec = *desired;
    opaque->atrack = SDL_Android_AudioTrack_new_from_sdl_spec(env, desired);
    if (!opaque->atrack) {
        ALOGE("aout_open_audio_n: failed to new AudioTrcak()");
        return -1;
    }

    opaque->buffer_size = SDL_Android_AudioTrack_get_min_buffer_size(opaque->atrack);
    if (opaque->buffer_size <= 0) {
        ALOGE("aout_open_audio_n: failed to getMinBufferSize()");
        SDL_Android_AudioTrack_free(env, opaque->atrack);
        opaque->atrack = NULL;
        return -1;
    }

    opaque->buffer = malloc(opaque->buffer_size);
    if (!opaque->buffer) {
        ALOGE("aout_open_audio_n: failed to allocate buffer");
        SDL_Android_AudioTrack_free(env, opaque->atrack);
        opaque->atrack = NULL;
        return -1;
    }

    if (obtained) {
        SDL_Android_AudioTrack_get_target_spec(opaque->atrack, obtained);
        SDLTRACE("audio target format fmt:0x%x, channel:0x%x", (int)obtained->format, (int)obtained->channels);
    }

    opaque->audio_session_id = SDL_Android_AudioTrack_getAudioSessionId(env, opaque->atrack);
    ALOGI("audio_session_id = %d\n", opaque->audio_session_id);

    opaque->pause_on = 1;
    opaque->abort_request = 0;
    opaque->audio_tid = SDL_CreateThreadEx(&opaque->_audio_tid, aout_thread, aout, "ff_aout_android");
    if (!opaque->audio_tid) {
        ALOGE("aout_open_audio_n: failed to create audio thread");
        SDL_Android_AudioTrack_free(env, opaque->atrack);
        opaque->atrack = NULL;
        return -1;
    }

    return 0;
}

static int aout_open_audio(SDL_Aout *aout, const SDL_AudioSpec *desired, SDL_AudioSpec *obtained)
{
    // SDL_Aout_Opaque *opaque = aout->opaque;
    JNIEnv *env = NULL;
    if (JNI_OK != SDL_JNI_SetupThreadEnv(&env)) {
        ALOGE("aout_open_audio: AttachCurrentThread: failed");
        return -1;
    }

    return aout_open_audio_n(env, aout, desired, obtained);
}

static void aout_pause_audio(SDL_Aout *aout, int pause_on)
{
    SDL_Aout_Opaque *opaque = aout->opaque;

    SDL_LockMutex(opaque->wakeup_mutex);
    SDLTRACE("Rapid aout_pause_audio(%d)", pause_on);
    opaque->pause_on = pause_on;
    if (!pause_on)
        SDL_CondSignal(opaque->wakeup_cond);
    SDL_UnlockMutex(opaque->wakeup_mutex);
}

static void aout_flush_audio(SDL_Aout *aout)
{
    SDL_Aout_Opaque *opaque = aout->opaque;
    SDL_LockMutex(opaque->wakeup_mutex);
    SDLTRACE("aout_flush_audio()");
    opaque->need_flush = 1;
    SDL_CondSignal(opaque->wakeup_cond);
    SDL_UnlockMutex(opaque->wakeup_mutex);
}

static void aout_set_volume(SDL_Aout *aout, float left_volume, float right_volume)
{
    SDL_Aout_Opaque *opaque = aout->opaque;
    SDL_LockMutex(opaque->wakeup_mutex);
    SDLTRACE("aout_flush_audio()");
    opaque->left_volume = left_volume;
    opaque->right_volume = right_volume;
    opaque->need_set_volume = 1;
    SDL_CondSignal(opaque->wakeup_cond);
    SDL_UnlockMutex(opaque->wakeup_mutex);
}

static void aout_close_audio(SDL_Aout *aout)
{
    SDL_Aout_Opaque *opaque = aout->opaque;

    SDL_LockMutex(opaque->wakeup_mutex);
    opaque->abort_request = true;
    SDL_CondSignal(opaque->wakeup_cond);
    SDL_UnlockMutex(opaque->wakeup_mutex);

    SDL_WaitThread(opaque->audio_tid, NULL);

    opaque->audio_tid = NULL;
}

static int aout_get_audio_session_id(SDL_Aout *aout)
{
    SDL_Aout_Opaque *opaque = aout->opaque;

    return opaque->audio_session_id;
}

static void aout_free_l(SDL_Aout *aout)
{
    if (!aout)
        return;

    aout_close_audio(aout);

    SDL_Aout_Opaque *opaque = aout->opaque;
    if (opaque) {
        free(opaque->buffer);
        opaque->buffer = NULL;
        opaque->buffer_size = 0;

        SDL_DestroyCond(opaque->wakeup_cond);
        SDL_DestroyMutex(opaque->wakeup_mutex);
    }

    SDL_Aout_FreeInternal(aout);
}

static void func_set_playback_rate(SDL_Aout *aout, float speed)
{
    if (!aout)
        return;

    SDL_Aout_Opaque *opaque = aout->opaque;
    SDL_LockMutex(opaque->wakeup_mutex);
    SDLTRACE("%s %f", __func__, (double)speed);
    opaque->speed = speed;
    opaque->speed_changed = 1;
    SDL_CondSignal(opaque->wakeup_cond);
    SDL_UnlockMutex(opaque->wakeup_mutex);
}
void aout_setpolicy(SDL_Aout *aout, int policy)
{
    SDL_Aout_Opaque *opaque = aout->opaque;
    if (opaque) {
        opaque->write_policy = policy;
    }
}

SDL_Aout *SDL_AoutAndroid_CreateForAudioTrack()
{
    SDL_Aout *aout = SDL_Aout_CreateInternal(sizeof(SDL_Aout_Opaque));
    if (!aout)
        return NULL;

    SDL_Aout_Opaque *opaque = aout->opaque;
    opaque->wakeup_cond  = SDL_CreateCond();
    opaque->wakeup_mutex = SDL_CreateMutex();
    opaque->speed        = 1.0f;

    aout->opaque_class = &g_audiotrack_class;
    aout->free_l       = aout_free_l;
    aout->open_audio   = aout_open_audio;
    aout->pause_audio  = aout_pause_audio;
    aout->flush_audio  = aout_flush_audio;
    aout->set_volume   = aout_set_volume;
    aout->close_audio  = aout_close_audio;
    aout->set_policy = aout_setpolicy;
    aout->func_get_audio_session_id = aout_get_audio_session_id;
    aout->func_set_playback_rate    = func_set_playback_rate;

    return aout;
}

bool SDL_AoutAndroid_IsObjectOfAudioTrack(SDL_Aout *aout)
{
    if (aout)
        return false;

    return aout->opaque_class == &g_audiotrack_class;
}

void SDL_Init_AoutAndroid(JNIEnv *env)
{

}
