/*
 * ffpipeline_android.c
 *
 * Copyright (c) 2014 Bilibili
 * Copyright (c) 2014 Zhang Rui <bbcallen@gmail.com>
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

#include "ffpipeline_android.h"
#include <jni.h>
#include "ffpipenode_android_mediacodec_vdec.h"
#include "../../pipeline/ffpipenode_ffplay_vdec.h"
#include "../../ff_ffplay.h"
#include "ijksdl/android/ijksdl_android_jni.h"
#include "ijksdl/android/ijksdl_android.h"

static SDL_Class g_pipeline_class = {
    .name = "ffpipeline_android_media",
};

typedef struct IJKFF_Pipeline_Opaque {
    FFPlayer      *ffp;
    SDL_mutex     *surface_mutex;
    jobject        jsurface;
    volatile bool  is_surface_need_reconfigure;

    bool         (*mediacodec_select_callback)(void *opaque, ijkmp_mediacodecinfo_context *mcc);
    void          *mediacodec_select_callback_opaque;

    int          (*on_drm_init_info_updated_callback)(void *opaque, char* drm_info, int len, int flag);
    jobject      (*get_media_crypto_callback)(void *opaque, int type);
    int          (*get_drm_session_state_callback)(void *opaque, int type, int flag);

    SDL_Vout      *weak_vout;

    float          left_volume;
    float          right_volume;
} IJKFF_Pipeline_Opaque;

static void func_destroy(IJKFF_Pipeline *pipeline)
{
    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    JNIEnv *env = NULL;

    SDL_DestroyMutexP(&opaque->surface_mutex);

    if (JNI_OK != SDL_JNI_SetupThreadEnv(&env)) {
        ALOGE("amediacodec-pipeline:destroy: SetupThreadEnv failed\n");
        goto fail;
    }

    SDL_JNI_DeleteGlobalRefP(env, &opaque->jsurface);
fail:
    return;
}

static IJKFF_Pipenode *func_open_video_decoder(IJKFF_Pipeline *pipeline, FFPlayer *ffp)
{
    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    IJKFF_Pipenode        *node = NULL;

    if (ffp->mediacodec_all_videos || ffp->mediacodec_avc || ffp->mediacodec_hevc || ffp->mediacodec_mpeg2)
        node = ffpipenode_create_video_decoder_from_android_mediacodec(ffp, pipeline, opaque->weak_vout);
    if (!node) {
        node = ffpipenode_create_video_decoder_from_ffplay(ffp);
        ALOGD("Rapid func_open_video_decoder mediacodec fial --------------\n");
    }

    return node;
}

static SDL_Aout *func_open_audio_output(IJKFF_Pipeline *pipeline, FFPlayer *ffp)
{
    SDL_Aout *aout = NULL;
    if (ffp->opensles) {
        aout = SDL_AoutAndroid_CreateForOpenSLES();
    } else {
        aout = SDL_AoutAndroid_CreateForAudioTrack();
    }
    if (aout) {
        SDL_AoutSetStereoVolume(aout, pipeline->opaque->left_volume, pipeline->opaque->right_volume);
        SDL_AoutSetPolicy(aout, ffp->audio_write_policy);
    }
    return aout;
}

static IJKFF_Pipenode *func_init_video_decoder(IJKFF_Pipeline *pipeline, FFPlayer *ffp)
{
    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    IJKFF_Pipenode        *node = NULL;

    if (ffp->mediacodec_all_videos || ffp->mediacodec_avc || ffp->mediacodec_hevc || ffp->mediacodec_mpeg2)
        node = ffpipenode_init_decoder_from_android_mediacodec(ffp, pipeline, opaque->weak_vout);

    return node;
}

static int func_config_video_decoder(IJKFF_Pipeline *pipeline, FFPlayer *ffp)
{
    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    int                       ret = NULL;

    if (ffp->node_vdec) {
        ret = ffpipenode_config_from_android_mediacodec(ffp, pipeline, opaque->weak_vout, ffp->node_vdec);
    }

    return ret;
}

static int func_check_support_drm(IJKFF_Pipeline *pipeline, FFPlayer *ffp)
{
    char *drm_info = ffp->drm_info;
    JNIEnv *env = NULL;

    if (JNI_OK != SDL_JNI_SetupThreadEnv(&env)) {
        ALOGE("amediacodec-pipeline:check support drm: SetupThreadEnv failed\n");
        goto fail;
    }

    if (!strstr(drm_info, ",cenc,") && !strstr(drm_info, ",cbcs,")) {
        ALOGE("amediacodec-pipeline:check support drm:only support cenc and cbcs.\n");
        return 0;
    }

    if (J4A_GetSystemAndroidApiLevel(env) < 18) {
        ALOGE("amediacodec-pipeline:check support drm:the min level supported drm is 18.\n");
        return 0;
    }

    if (strstr(drm_info, "video,") && !(ffp->mediacodec_all_videos || ffp->mediacodec_avc || ffp->mediacodec_hevc || ffp->mediacodec_mpeg2)) {
        ALOGE("amediacodec-pipeline:check support drm:must use mediacodec for drm decoding.\n");
        return 0;
    }

    if ((strstr(drm_info, "audio,cbcs,") || strstr(drm_info, "video,cbcs,")) && J4A_GetSystemAndroidApiLevel(env) < 25) {
        ALOGE("amediacodec-pipeline:check support drm:the min level supported aes cbcs is 25.\n");
        return 0;
    }

    return 1;

fail:
    return 0;
}

static int func_update_drm_init_info(IJKFF_Pipeline *pipeline, FFPlayer *ffp)
{
    char *drm_info = ffp->drm_info;
    return ffpipeline_on_drm_init_info_updated_l(pipeline, drm_info, strlen(drm_info) + 1, ffp->drm_flag);
}

static int func_get_drm_session_state(IJKFF_Pipeline *pipeline, FFPlayer *ffp, int type, int flag)
{
    return ffpipeline_get_drm_session_state_l(pipeline, type, flag);
}


inline static bool check_ffpipeline(IJKFF_Pipeline* pipeline, const char *func_name)
{
    if (!pipeline || !pipeline->opaque || !pipeline->opaque_class) {
        ALOGE("%s.%s: invalid pipeline\n", pipeline->opaque_class->name, func_name);
        return false;
    }

    if (pipeline->opaque_class != &g_pipeline_class) {
        ALOGE("%s.%s: unsupported method\n", pipeline->opaque_class->name, func_name);
        return false;
    }

    return true;
}

IJKFF_Pipeline *ffpipeline_create_from_android(FFPlayer *ffp)
{
    ALOGD("ffpipeline_create_from_android()\n");
    IJKFF_Pipeline *pipeline = ffpipeline_alloc(&g_pipeline_class, sizeof(IJKFF_Pipeline_Opaque));
    if (!pipeline)
        return pipeline;

    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    opaque->ffp                   = ffp;
    opaque->surface_mutex         = SDL_CreateMutex();
    opaque->left_volume           = 1.0f;
    opaque->right_volume          = 1.0f;
    if (!opaque->surface_mutex) {
        ALOGE("ffpipeline-android:create SDL_CreateMutex failed\n");
        goto fail;
    }

    pipeline->func_destroy              = func_destroy;
    pipeline->func_open_video_decoder   = func_open_video_decoder;
    pipeline->func_open_audio_output    = func_open_audio_output;
    pipeline->func_init_video_decoder   = func_init_video_decoder;
    pipeline->func_config_video_decoder = func_config_video_decoder;
    pipeline->func_check_support_drm    = func_check_support_drm;
    pipeline->func_update_drm_init_info = func_update_drm_init_info;
    pipeline->func_get_drm_session_state= func_get_drm_session_state;

    return pipeline;
fail:
    ffpipeline_free_p(&pipeline);
    return NULL;
}

int ffpipeline_lock_surface(IJKFF_Pipeline* pipeline)
{
    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    return SDL_LockMutex(opaque->surface_mutex);
}

int ffpipeline_unlock_surface(IJKFF_Pipeline* pipeline)
{
    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    return SDL_UnlockMutex(opaque->surface_mutex);
}

jobject ffpipeline_get_surface_as_global_ref_l(JNIEnv *env, IJKFF_Pipeline* pipeline)
{
    if (!check_ffpipeline(pipeline, __func__))
        return NULL;

    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    if (!opaque->surface_mutex)
        return NULL;

    jobject global_ref = NULL;
    if (opaque->jsurface)
        global_ref = (*env)->NewGlobalRef(env, opaque->jsurface);

    return global_ref;
}

jobject ffpipeline_get_surface_as_global_ref(JNIEnv *env, IJKFF_Pipeline* pipeline)
{
    ffpipeline_lock_surface(pipeline);
    jobject new_surface = ffpipeline_get_surface_as_global_ref_l(env, pipeline);
    ffpipeline_unlock_surface(pipeline);
    return new_surface;
}

void ffpipeline_set_vout(IJKFF_Pipeline* pipeline, SDL_Vout *vout)
{
    if (!check_ffpipeline(pipeline, __func__))
        return;

    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    opaque->weak_vout = vout;
}

int ffpipeline_set_surface(JNIEnv *env, IJKFF_Pipeline* pipeline, jobject surface)
{
    ALOGD("%s()\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return -1;

    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    if (!opaque->surface_mutex)
        return -1;

    ffpipeline_lock_surface(pipeline);
    {
        jobject prev_surface = opaque->jsurface;

        if ((surface == prev_surface) ||
            (surface && prev_surface && (*env)->IsSameObject(env, surface, prev_surface))) {
            // same object, no need to reconfigure
        } else {
            SDL_VoutAndroid_setAMediaCodec(opaque->weak_vout, NULL);
            if (surface) {
                opaque->jsurface = (*env)->NewGlobalRef(env, surface);
            } else {
                opaque->jsurface = NULL;
            }
            opaque->is_surface_need_reconfigure = true;

            if (prev_surface != NULL) {
                SDL_JNI_DeleteGlobalRefP(env, &prev_surface);
            }
        }
    }
    ffpipeline_unlock_surface(pipeline);

    return 0;
}

bool ffpipeline_is_surface_need_reconfigure_l(IJKFF_Pipeline* pipeline)
{
    if (!check_ffpipeline(pipeline, __func__))
        return false;

    return pipeline->opaque->is_surface_need_reconfigure;
}

void ffpipeline_set_surface_need_reconfigure_l(IJKFF_Pipeline* pipeline, bool need_reconfigure)
{
    ALOGD("%s(%d)\n", __func__, (int)need_reconfigure);
    if (!check_ffpipeline(pipeline, __func__))
        return;

    pipeline->opaque->is_surface_need_reconfigure = need_reconfigure;
}

void ffpipeline_set_mediacodec_select_callback(IJKFF_Pipeline* pipeline, bool (*callback)(void *opaque, ijkmp_mediacodecinfo_context *mcc), void *opaque)
{
    ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return;

    pipeline->opaque->mediacodec_select_callback        = callback;
    pipeline->opaque->mediacodec_select_callback_opaque = opaque;
}

bool ffpipeline_select_mediacodec_l(IJKFF_Pipeline* pipeline, ijkmp_mediacodecinfo_context *mcc)
{
    ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return false;

    if (!mcc || !pipeline->opaque->mediacodec_select_callback)
        return false;

    return pipeline->opaque->mediacodec_select_callback(pipeline->opaque->mediacodec_select_callback_opaque, mcc);
}

void ffpipeline_set_on_drm_init_info_updated_callback(IJKFF_Pipeline* pipeline, int (*callback)(void *opaque, char* drm_info, int len, int flag))
{
    ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return;

    pipeline->opaque->on_drm_init_info_updated_callback = callback;
}

int ffpipeline_on_drm_init_info_updated_l(IJKFF_Pipeline* pipeline, char* drm_info, int len, int flag)
{
    // ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return 0;

    if (!pipeline->opaque->on_drm_init_info_updated_callback)
        return 0;

    return pipeline->opaque->on_drm_init_info_updated_callback(pipeline->opaque->mediacodec_select_callback_opaque, drm_info, len, flag);
}

void ffpipeline_set_get_media_crypto_callback(IJKFF_Pipeline* pipeline, jobject (*callback)(void *opaque, int type))
{
    ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return;

    pipeline->opaque->get_media_crypto_callback = callback;
}

jobject ffpipeline_get_media_crypto_l(IJKFF_Pipeline* pipeline, int type)
{
    ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return false;

    if (!pipeline->opaque->get_media_crypto_callback)
        return false;

    return pipeline->opaque->get_media_crypto_callback(pipeline->opaque->mediacodec_select_callback_opaque, type);
}

jobject ffpipeline_get_crypto_as_global_ref(IJKFF_Pipeline* pipeline, jobject crypto)
{
    JNIEnv *env = NULL;
    jobject global_crypto = NULL;

    ALOGD("%s\n", __func__);
    if (JNI_OK != SDL_JNI_SetupThreadEnv(&env)) {
        ALOGE("amediacodec-pipeline:get global crypto: SetupThreadEnv failed\n");
        return global_crypto;
    }
    if (!check_ffpipeline(pipeline, __func__)) {
        ALOGE("amediacodec-pipeline:get global crypto: check ffpipeline failed\n");
        return global_crypto;
    }

    if (crypto) {
        global_crypto = (*env)->NewGlobalRef(env, crypto);
    }
    return global_crypto;
}

void ffpipeline_delete_crypto_global_ref(IJKFF_Pipeline* pipeline, jobject crypto)
{
    JNIEnv *env = NULL;

    ALOGD("%s\n", __func__);
    if (JNI_OK != SDL_JNI_SetupThreadEnv(&env)) {
        ALOGE("amediacodec-pipeline:delete global crypto: SetupThreadEnv failed\n");
        return;
    }
    if (!check_ffpipeline(pipeline, __func__)) {
        ALOGE("amediacodec-pipeline:delete global crypto: check ffpipeline failed\n");
        return;
    }

    ALOGI("%s crypto: %p\n", __func__, crypto);
    SDL_JNI_DeleteGlobalRefP(env, &crypto);
}

void ffpipeline_set_get_drm_session_state_callback(IJKFF_Pipeline* pipeline, int (*callback)(void *opaque, int type, int flag))
{
    ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return;

    pipeline->opaque->get_drm_session_state_callback = callback;
}

int ffpipeline_get_drm_session_state_l(IJKFF_Pipeline* pipeline, int type, int flag)
{
    if (!check_ffpipeline(pipeline, __func__))
        return -1;

    if (!pipeline->opaque->get_drm_session_state_callback)
        return -1;

    return pipeline->opaque->get_drm_session_state_callback(pipeline->opaque->mediacodec_select_callback_opaque, type, flag);
}

void ffpipeline_set_volume(IJKFF_Pipeline* pipeline, float left, float right)
{
    ALOGD("%s\n", __func__);
    if (!check_ffpipeline(pipeline, __func__))
        return;

    IJKFF_Pipeline_Opaque *opaque = pipeline->opaque;
    opaque->left_volume  = left;
    opaque->right_volume = right;

    if (opaque->ffp && opaque->ffp->aout) {
        SDL_AoutSetStereoVolume(opaque->ffp->aout, left, right);
    }
}
