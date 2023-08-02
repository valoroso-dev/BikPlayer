/*
 * Copyright (c) 2022 Valoroso
 * Copyright (c) 2022 Gibbs.Qin
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

#include "libavformat/avformat.h"
#include "libavformat/url.h"
#include "libavutil/avstring.h"
#include "libavutil/opt.h"
#include "ijkavformat.h"

typedef struct {
    AVClass         *class;
    AVFormatContext *inner;
    char            *inner_url;

    /* options */
    AVDictionary    *open_opts;

    int             play_next;
} Context;

static int ijkplaylist_probe(AVProbeData *probe)
{
    if (av_strstart(probe->filename, "ijkplaylist:", NULL))
        return AVPROBE_SCORE_MAX;

    return 0;
}

static int ijkplaylist_read_close(AVFormatContext *avf)
{
    Context *c = avf->priv_data;

    avformat_close_input(&c->inner);
    av_freep(&c->inner_url);
    av_dict_free(&c->open_opts);
    return 0;
}

// FIXME: install libavformat/internal.h
int ff_alloc_extradata(AVCodecParameters *par, int size);
void ff_free_stream(AVFormatContext *s, AVStream *st);
void ff_read_frame_flush(AVFormatContext *s);

static int copy_stream_props(AVStream *st, AVStream *source_st)
{
    int ret;

    if (st->codecpar->codec_id || !source_st->codecpar->codec_id) {
        if (st->codecpar->extradata_size < source_st->codecpar->extradata_size) {
            if (st->codecpar->extradata) {
                av_freep(&st->codecpar->extradata);
                st->codecpar->extradata_size = 0;
            }
            ret = ff_alloc_extradata(st->codecpar,
                                     source_st->codecpar->extradata_size);
            if (ret < 0)
                return ret;
        }
        memcpy(st->codecpar->extradata, source_st->codecpar->extradata,
               source_st->codecpar->extradata_size);
        return 0;
    }
    if ((ret = avcodec_parameters_copy(st->codecpar, source_st->codecpar)) < 0)
        return ret;
    st->r_frame_rate        = source_st->r_frame_rate;
    st->avg_frame_rate      = source_st->avg_frame_rate;
    st->time_base           = source_st->time_base;
    st->sample_aspect_ratio = source_st->sample_aspect_ratio;

    av_dict_copy(&st->metadata, source_st->metadata, 0);
    return 0;
}

static int compare_stream_props(AVStream *st, AVStream *source_st)
{
    AVCodecParameters *parameters = st->codecpar;
    AVCodecParameters *source_parameters = source_st->codecpar;

    if (parameters->codec_type != source_parameters->codec_type ||
        parameters->codec_id != source_parameters->codec_id) {
        return 0;
    }

    if (parameters->codec_type == AVMEDIA_TYPE_VIDEO) {
        if (parameters->sample_aspect_ratio.num != source_parameters->sample_aspect_ratio.num ||
            parameters->sample_aspect_ratio.den != source_parameters->sample_aspect_ratio.den ||
            parameters->format != source_parameters->format) {
            return 0;
        }
    } else if (parameters->codec_type == AVMEDIA_TYPE_AUDIO) {
        if (parameters->channel_layout != source_parameters->channel_layout ||
            parameters->channels != source_parameters->channels ||
            parameters->sample_rate != source_parameters->sample_rate ||
            parameters->format != source_parameters->format) {
            return 0;
        }
    }
    
    return 1;
}

static int open_inner(AVFormatContext *avf)
{
    Context         *c          = avf->priv_data;
    AVDictionary    *tmp_opts   = NULL;
    AVFormatContext *new_avf    = NULL;
    AVStream *ist, *st;
    int stream_same = 1;
    int ret = -1;
    int i   = 0;

    av_log(avf, AV_LOG_INFO, "ijkplaylist: %s %s", __func__, c->inner_url);

    new_avf = avformat_alloc_context();
    if (!new_avf) {
        ret = AVERROR(ENOMEM);
        goto fail;
    }

    if (c->open_opts)
        av_dict_copy(&tmp_opts, c->open_opts, 0);

    new_avf->interrupt_callback = avf->interrupt_callback;
    new_avf->opaque = avf->opaque;
    ret = avformat_open_input(&new_avf, c->inner_url, NULL, &tmp_opts);
    if (ret < 0)
        goto fail;

    // ret = avformat_find_stream_info(new_avf, NULL);
    // if (ret < 0)
    //     goto fail;

    for (i = 0; i < avf->nb_streams; i++) {
        if (i >= new_avf->nb_streams) {
            stream_same = 0;
            break;
        }
        if (!compare_stream_props(avf->streams[i], new_avf->streams[i])) {
            stream_same = 0;
            break;
        }
    }

    if (avf->nb_streams == 0) {
        av_log(avf, AV_LOG_INFO, "ijkplaylist: realloc stream array from %d to %d\n", avf->nb_streams, new_avf->nb_streams);
        for (i = avf->nb_streams - 1; i >= 0; i--)
            ff_free_stream(avf, avf->streams[i]);
        for (i = 0; i < new_avf->nb_streams; i++) {
            ist = new_avf->streams[i];

            if (!(st = avformat_new_stream(avf, NULL))) {
                ret = AVERROR(ENOMEM);
                goto fail;
            }

            if ((ret = copy_stream_props(st, ist)) < 0)
                goto fail;
        }
    } else if (!stream_same) {
        av_log(avf, AV_LOG_ERROR, "ijkplaylist: the stream array is not same\n");
        ret = FFP_ERROR_PLAYLIST_STREAM;
        goto fail;
    }
    avf->duration = new_avf->duration;
    avf->start_time = new_avf->start_time;

    // av_dump_format(avf, 0, c->inner_url, 0);

    avformat_close_input(&c->inner);
    c->inner = new_avf;
    new_avf = NULL;
    ret = 0;
fail:
    av_dict_free(&tmp_opts);
    avformat_close_input(&new_avf);
    return ret;
}

static int ijkplaylist_read_header(AVFormatContext *avf, AVDictionary **options)
{
    Context    *c           = avf->priv_data;
    const char *inner_url   = NULL;
    int         ret         = -1;

    av_strstart(avf->filename, "ijkplaylist:", &inner_url);
    c->inner_url = av_strdup(inner_url);
    c->play_next = 0;

    if (options)
        av_dict_copy(&c->open_opts, *options, 0);

    ret = open_inner(avf);

    return ret;
}

static int ijkplaylist_read_packet(AVFormatContext *avf, AVPacket *pkt)
{
    Context *c   = avf->priv_data;
    int      ret = 0;

    if (c->play_next) {
        ret = open_inner(avf);
        c->play_next = 0;
    }
    if (ret < 0) {
        return ret;
    }
    if (c->inner) {
        ret = av_read_frame(c->inner, pkt);
    }

    return ret;
}

static int ijkplaylist_read_seek(AVFormatContext *avf, int stream_index, int64_t sample_time, int flags)
{
    Context *c = avf->priv_data;
    int64_t seek_pos_msec;
    int ret;

    if (flags == AVSEEK_FLAG_PLAYLIST_NEXT) {
        if (strlen(avf->filename) > 0) {
            av_free(c->inner_url);
            c->inner_url = av_strdup(avf->filename);
            c->play_next = 1;
            return 0;
        } else {
            return AVERROR_INVALIDDATA;
        }
    }

    seek_pos_msec = av_rescale_rnd(sample_time, 1000,
                                    c->inner->streams[stream_index]->time_base.den,
                                    flags & AVSEEK_FLAG_BACKWARD ? AV_ROUND_DOWN : AV_ROUND_UP);
    ff_read_frame_flush(c->inner);
    ret = av_seek_frame(c->inner, -1, seek_pos_msec * 1000, flags);
    return ret;
}

#define OFFSET(x) offsetof(Context, x)

static const AVOption options[] = {
    { NULL }
};

#undef OFFSET

static const AVClass ijkplaylist_class = {
    .class_name = "PlayList demuxer",
    .item_name  = av_default_item_name,
    .option     = options,
    .version    = LIBAVUTIL_VERSION_INT,
};

AVInputFormat ijkff_ijkplaylist_demuxer = {
    .name           = "ijkplaylist",
    .long_name      = "Playlist Controller",
    .flags          = AVFMT_NOFILE | AVFMT_TS_DISCONT | AVFMT_NO_BYTE_SEEK | AVFMT_IJK_PLAYLIST,
    .priv_data_size = sizeof(Context),
    .read_probe     = ijkplaylist_probe,
    .read_header2   = ijkplaylist_read_header,
    .read_packet    = ijkplaylist_read_packet,
    .read_close     = ijkplaylist_read_close,
    .read_seek      = ijkplaylist_read_seek,
    .priv_class     = &ijkplaylist_class,
};