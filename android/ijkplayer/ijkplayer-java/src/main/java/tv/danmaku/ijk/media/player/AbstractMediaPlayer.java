/*
 * Copyright (C) 2013-2014 Bilibili
 * Copyright (C) 2013-2014 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.danmaku.ijk.media.player;

import java.util.Map;
import java.util.UUID;

import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMediaPlayer implements IMediaPlayer {
    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnTimedTextListener mOnTimedTextListener;

    private int mDrmType = IMediaPlayer.DRM_TYPE_NULL;
    private boolean mDrmMultiSession = false;
    // stream type暂时不使用
    private int mStreamType = IMediaPlayer.STREAM_UNKNOWN;
    private String mDrmLicenseServerUrl;
    private Map<String, String> mDrmReqHeaders;
    private String mDrmReqMethod;

    public static final UUID COMMON_PSSH_UUID = new UUID(0x1077EFECC0B24D02L, 0xACE33C1E52E2FB4BL);
    public static final UUID CLEARKEY_UUID = new UUID(0xE2719D58A985B3C9L, 0x781AB030AF78D30EL);
    public static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
    public static final UUID PLAYREADY_UUID = new UUID(0x9A04F07998404286L, 0xAB92E65BE0885F95L);
    public static final UUID UUID_NIL = new UUID(0L, 0L);
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_PLAYBACK_COMPLETED = 5;
    public static final int STATE_BUFFERING = 6;

    public final void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public final void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public final void setOnBufferingUpdateListener(
            OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public final void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    public final void setOnVideoSizeChangedListener(
            OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    public final void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public final void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    public final void setOnTimedTextListener(OnTimedTextListener listener) {
        mOnTimedTextListener = listener;
    }

    public void setDrmInfo(int drmType, boolean multiSession, String licenseServerUrl, Map<String, String> headers, String reqMethod) {
        mDrmType = drmType;
        mDrmMultiSession = multiSession;
        mDrmLicenseServerUrl = licenseServerUrl;
        mDrmReqHeaders = headers;
        mDrmReqMethod = reqMethod;
    }

    public final int getDrmType() {
        return mDrmType;
    }

    public final boolean isDrmMultiSession() {
        return mDrmMultiSession;
    }

    public final int getStreamType() {
        return mStreamType;
    }

    public final String getDrmLicenseServerUrl() {
        return mDrmLicenseServerUrl;
    }

    public final Map<String, String> getReqHeaders() {
        return mDrmReqHeaders;
    }

    public final String getDrmReqMethod() {
        return mDrmReqMethod;
    }

    /*public final String getMimeType() {
        return mStreamType == IMediaPlayer.STREAM_DASH ? "/dash-xml" : "/x-mpegURL";
    }*/

    public final UUID getDrmUUID() {
        if (mDrmType == IMediaPlayer.DRM_TYPE_WIDEVINE)
            return WIDEVINE_UUID;
        if (mDrmType == IMediaPlayer.DRM_TYPE_PLAYREADY)
            return PLAYREADY_UUID;
        return UUID_NIL;
    }

    public void resetListeners() {
        mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnVideoSizeChangedListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnTimedTextListener = null;
    }

    protected final void notifyOnPrepared() {
        if (mOnPreparedListener != null)
            mOnPreparedListener.onPrepared(this);
    }

    protected final void notifyOnCompletion() {
        if (mOnCompletionListener != null)
            mOnCompletionListener.onCompletion(this);
    }

    protected final void notifyOnBufferingUpdate(int percent) {
        if (mOnBufferingUpdateListener != null)
            mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
    }

    protected final void notifyOnSeekComplete() {
        if (mOnSeekCompleteListener != null)
            mOnSeekCompleteListener.onSeekComplete(this);
    }

    protected final void notifyOnVideoSizeChanged(int width, int height,
                                                  int sarNum, int sarDen) {
        if (mOnVideoSizeChangedListener != null)
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height,
                    sarNum, sarDen);
    }

    protected final boolean notifyOnError(int what, int extra) {
        return mOnErrorListener != null && mOnErrorListener.onError(this, what, extra);
    }

    protected final boolean notifyOnInfo(int what, int extra, String info) {
        return mOnInfoListener != null && mOnInfoListener.onInfo(this, what, extra, info);
    }

    protected final void notifyOnTimedText(IjkTimedText text) {
        if (mOnTimedTextListener != null)
            mOnTimedTextListener.onTimedText(this, text);
    }

    public void setDataSource(IMediaDataSource mediaDataSource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVideoCodecName() {
        return "unknown";
    }
}
