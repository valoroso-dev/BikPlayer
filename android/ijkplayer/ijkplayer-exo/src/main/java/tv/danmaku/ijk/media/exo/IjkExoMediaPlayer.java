/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
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

package tv.danmaku.ijk.media.exo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.TracksInfo;
import com.google.android.exoplayer2.TracksInfo.TrackGroupInfo;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides.TrackSelectionOverride;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;

import java.io.FileDescriptor;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import tv.danmaku.ijk.media.player.AbstractMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaMeta;
import tv.danmaku.ijk.media.player.MediaInfo;
import tv.danmaku.ijk.media.player.misc.IMediaFormat;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;

public class IjkExoMediaPlayer extends AbstractMediaPlayer {
    private static final String TAG = "IjkExoMediaPlayer";
    private Context mAppContext;
    private ExoPlayer mInternalPlayer;
    private Uri mDataSource;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int playerState = STATE_IDLE;
    private SurfaceHolder mSurfaceHolder;
    private Surface mSurface;
    private DefaultTrackSelector mTrackSelector;
    private Handler mHandler;
    private TracksInfo mTracksInfo;
    private ArrayList<IjkTrackInfo> mTrackList;
    private ReentrantLock mLock;
    private int mSelectTrack[];
    private EventLogger mEventLogger;
    private PlayerEventListener mEventListener;
    private Looper mLooper;
    private boolean mScreenOnWhilePlaying;
    private boolean mLooping;
    private String mVideoCodecName = "unknown";

    private long mBufferingTime = 0;
    private long mDuration = 0;
    private long mCurrentPos = 0;
    // private long mPrevPos = 0;
    static final int INVOKE_SETSURFACE = 0;
    static final int INVOKE_SETDATASOURCE = 1;
    static final int INVOKE_PREPAREASYNC = 2;
    static final int INVOKE_START = 3;
    static final int INVOKE_STOP = 4;
    static final int INVOKE_PAUSE = 5;
    static final int INVOKE_GETDURATION = 6;
    static final int INVOKE_GETPOSITION = 7;
    static final int INVOKE_SEEKTO = 8;
    // gap by obsolete invoke type
    static final int INVOKE_SELECTTRACK = 10;
    static final int INVOKE_SETVOLUME = 11;
    static final int INVOKE_SELECTTRACK2 = 12;
    static final int INVOKE_RESET = 19;
    static final int INVOKE_RELEASE = 20;

    static final String[] IJK_STATE_DESC = {"STATE_ERROR", "STATE_IDLE", "STATE_PREPARING", "STATE_PREPARED", "STATE_PLAYING", "STATE_PAUSED", "STATE_PLAYBACK_COMPLETED", "STATE_BUFFERING"};
    static final String[] EXO_STATE_DESC = {"STATE_UNKNOWN", "STATE_IDLE", "STATE_BUFFERING", "STATE_READY", "STATE_ENDED"};
    static final String[] EXO_EVENT_DESC = {"STATE_UNKNOWN", "STATE_IDLE", "STATE_BUFFERING", "STATE_READY", "STATE_ENDED"};

    public IjkExoMediaPlayer(Context context) {
        this(context, null);
    }

    public IjkExoMediaPlayer(Context context, BandwidthMeter bandwidthMeter) {
        mAppContext = context.getApplicationContext();
        mLock = new ReentrantLock();
        mSelectTrack = new int[4];
        Arrays.fill(mSelectTrack, -1);
        mTrackList = new ArrayList<>();
        mLooper = Util.getCurrentOrMainLooper();
        mHandler = new EnsureSameThreadHandler(mLooper, this);
        mEventListener = new PlayerEventListener();
        mTrackSelector = new DefaultTrackSelector(mAppContext);
        mEventLogger = new EventLogger(mTrackSelector) {
            @Override
            public void onVideoDecoderInitialized(EventTime eventTime, String decoderName, long initializationDurationMs) {
                super.onVideoDecoderInitialized(eventTime, decoderName, initializationDurationMs);
                mVideoCodecName = decoderName;
            }
        };
        mScreenOnWhilePlaying = true;
        mLooping = false;
        createInternalPlayer(bandwidthMeter);
    }

    private void createInternalPlayer(BandwidthMeter bandwidthMeter) {
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(mAppContext, new DefaultHttpDataSource.Factory());
        ExoPlayer.Builder builder = new ExoPlayer.Builder(mAppContext, new DefaultMediaSourceFactory(dataSourceFactory))
                .setTrackSelector(mTrackSelector);
        if (bandwidthMeter != null) {
            builder.setBandwidthMeter(bandwidthMeter);
        }
        mInternalPlayer = builder.build();
        mInternalPlayer.addListener(mEventListener);
        mInternalPlayer.addAnalyticsListener(mEventLogger);
        mInternalPlayer.setPlayWhenReady(true);
        updateWakeMode();
        updateLooping();
    }


    private void forwardToWorkThread(int what, Object obj) {
        Message msg = Message.obtain(mHandler, what, obj);
        msg.sendToTarget();
    }

    private void forwardToWorkThread(int what, int arg1, int arg2, Object obj) {
        Message msg = Message.obtain(mHandler, what, arg1, arg2, obj);
        msg.sendToTarget();
    }


    @Override
    public void setDisplay(SurfaceHolder sh) {
        Log.d(TAG, "holder is " + sh);
        mSurfaceHolder = sh;
        setSurface(sh == null ? null : sh.getSurface());
    }

    @Override
    public void setSurface(Surface surface) {
        if (inPlayerThread()) {
            setSurfaceInner(surface);
        } else {
            forwardToWorkThread(INVOKE_SETSURFACE, surface);
        }
    }

    private void setSurfaceInner(Surface surface) {
        if (mInternalPlayer != null)
            mInternalPlayer.setVideoSurface(surface);
    }

    @Override
    public void setDataSource(Context context, Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        if (inPlayerThread()) {
            setDataSourceInner(uri);
        } else {
            forwardToWorkThread(INVOKE_SETDATASOURCE, uri);
        }
    }

    private void setDataSourceInner(Uri uri) {
        mDataSource = uri;
        //create media item
        int streamType = getStreamType();
        int drmType = getDrmType();
        String drmLicenseUrl = getDrmLicenseServerUrl();
        Log.d(TAG, String.format("setDataSource uri: %s, drm type: %d, stream type: %d, license url: %s", mDataSource, drmType, streamType, drmLicenseUrl));
        MediaItem.Builder builder = new MediaItem.Builder();
        builder.setUri(uri);
        if (drmType != IMediaPlayer.DRM_TYPE_NULL) {
            MediaItem.DrmConfiguration.Builder drmCfgBuilder = new MediaItem.DrmConfiguration.Builder(getDrmUUID());
            drmCfgBuilder.setMultiSession(isDrmMultiSession())
                    .setLicenseUri(getDrmLicenseServerUrl())
                    .setLicenseRequestHeaders(getReqHeaders());
            builder.setDrmConfiguration(drmCfgBuilder.build());
        }
        mInternalPlayer.setMediaItem(builder.build());
        updateWakeMode();
        updateLooping();
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) {
        // TODO: handle headers
        setDataSource(context, uri);
    }

    @Override
    public void setTrack(int trackType, int trackId) {
        int par = (trackType << 16) | trackId;
        forwardToWorkThread(INVOKE_SELECTTRACK, par);
    }

    // TODO: @Override
    public void selectTrack(int track) {
        final ITrackInfo[] trackInfoArray = getTrackInfo();
        if (track < 0 || track >= trackInfoArray.length) {
            Log.e(TAG, "selectTrack error: invalid track " + track);
            return;
        }
        ITrackInfo trackInfo = trackInfoArray[track];
        String trackId = trackInfo.getFormat().getString(IjkMediaMeta.IJKM_KEY_TRACK_ID);
        int trackType;
        int targetRenderIndex = -1;
        switch (trackInfo.getTrackType()) {
            case ITrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                trackType = C.TRACK_TYPE_AUDIO;
                break;
            case ITrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                trackType = C.TRACK_TYPE_VIDEO;
                break;
            case ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT:
                trackType = C.TRACK_TYPE_TEXT;
                break;
            default:
                Log.e(TAG, "selectTrack error: invalid trackType " + trackInfo.getTrackType());
                return;
        }
        MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            Log.e(TAG, "selectTrack error: invalid mappedTrackInfo");
            return;
        }
        for (int renderIndex = 0; renderIndex < mappedTrackInfo.getRendererCount(); renderIndex++) {
            if (mappedTrackInfo.getRendererType(renderIndex) == trackType) {
                targetRenderIndex = renderIndex;
                break;
            }
        }
        Log.i(TAG, "selectTrack renderIndex " + targetRenderIndex + " and trackId " + trackId);
        if (!TextUtils.isEmpty(trackId) && targetRenderIndex >= 0) {
            forwardToWorkThread(INVOKE_SELECTTRACK2, targetRenderIndex, 0, trackId);
        }
    }

    public void selectTrackInner(int trackType, int trackId) {
        selectTrackInner(trackType, Integer.toString(trackId));
    }

    public void selectTrackInner(int trackType, String trackId) {
        Log.d(TAG, "track type " + trackType + " id " + trackId);
        //TracksInfo trackInfo = mInternalPlayer.getCurrentTracksInfo();
        MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            // get track group by track type and track id
            TrackGroupArray groups = mappedTrackInfo.getTrackGroups(trackType);
            TrackGroup targetTrackGroup = null;
            int targetTrackIndex = 0;
            for (int i = 0; i < groups.length; i++) {
                if (targetTrackGroup != null) {
                    break;
                }
                TrackGroup trackGroup = groups.get(i);
                for (int j = 0; j < trackGroup.length; j++) {
                    Format trackFormat = trackGroup.getFormat(j);
                    if (TextUtils.equals(trackFormat.id, trackId)) {
                        targetTrackGroup = trackGroup;
                        targetTrackIndex = j;
                        Log.d(TAG, "selectTrackInner trackFormat " + trackFormat);
                        break;
                    }
                }
            }
            if (targetTrackGroup == null) {
                Log.d(TAG, "Can't find target track " + trackId);
                return;
            }
            List<Integer> trackIndices = new ArrayList<>();
            trackIndices.add(targetTrackIndex);
            TrackSelectionOverrides overrides = new TrackSelectionOverrides.Builder()
                    .setOverrideForType(new TrackSelectionOverride(targetTrackGroup, trackIndices))
                    .build();
            mInternalPlayer.setTrackSelectionParameters(mInternalPlayer.getTrackSelectionParameters()
                    .buildUpon().setTrackSelectionOverrides(overrides).build());
        } else {
            Log.e(TAG, "no track info for " + trackType + "," + trackId);
        }
    }

    @Override
    public int getCurrentTrack(int trackType) {
        ITrackInfo[] trackInfoArray = getTrackInfo();
        if (trackInfoArray == null) {
            return mSelectTrack[trackType];
        }
        for (int i = 0; i < trackInfoArray.length; i++) {
            ITrackInfo trackInfo = trackInfoArray[i];
            IMediaFormat format = trackInfo.getFormat();
            if (format == null) {
                continue;
            }
            int selected = format.getInteger(IjkMediaMeta.IJKM_KEY_SELECTED);
            if (selected == 1 && trackInfo.getTrackType() == trackType) {
                if (trackType == ITrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                    int width = format.getInteger(IjkMediaMeta.IJKM_KEY_WIDTH);
                    if (mVideoWidth == width) {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }
        return mSelectTrack[trackType];
    }

    @Override
    public void setDataSource(String path) {
        setDataSource(mAppContext, Uri.parse(path));
    }

    @Override
    public void setDataSource(FileDescriptor fd) {
        // TODO: no support
        throw new UnsupportedOperationException("no support");
    }

    @Override
    public String getDataSource() {
        return mDataSource.toString();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if (inPlayerThread()) {
            prepareAsyncInner();
        } else {
            forwardToWorkThread(INVOKE_PREPAREASYNC, null);
        }
    }

    private void prepareAsyncInner() throws IllegalStateException {
        setPlayerState(STATE_PREPARING);
        mInternalPlayer.prepare();
    }

    @Override
    public void start() throws IllegalStateException {
        if (inPlayerThread()) {
            startInner();
        } else {
            forwardToWorkThread(INVOKE_START, null);
        }
    }

    private void startInner() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        // mInternalPlayer.setPlayWhenReady(true);
        mInternalPlayer.play();
    }

    @Override
    public void stop() throws IllegalStateException {
        if (inPlayerThread()) {
            stopInner();
        } else {
            forwardToWorkThread(INVOKE_STOP, null);
        }
    }

    private void stopInner() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        Log.d(TAG, "app call stop on exoplayer");
        mInternalPlayer.stop();
        // after call stop STATE_IDLE will be received from exo player
    }

    @Override
    public void pause() throws IllegalStateException {
        if (inPlayerThread()) {
            pauseInner();
        } else {
            forwardToWorkThread(INVOKE_PAUSE, null);
        }
    }

    public void pauseInner() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        // mInternalPlayer.setPlayWhenReady(false);
        mInternalPlayer.pause();
        setPlayerState(STATE_PAUSED);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        throw new UnsupportedOperationException("setWakeMode is not supported use setScreenOnWhilePlaying");
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        mScreenOnWhilePlaying = screenOn;
        updateWakeMode();
    }

    private void updateWakeMode() {
        if (mInternalPlayer == null) return;
        if (mScreenOnWhilePlaying) {
            mInternalPlayer.setWakeMode(C.WAKE_MODE_NONE);
        } else {
            mInternalPlayer.setWakeMode(C.WAKE_MODE_NETWORK);
        }
    }

    @Override
    public ITrackInfo[] getTrackInfo() {
        ITrackInfo[] ijkTrackInfo = null;
        mLock.lock();
        ijkTrackInfo = mTrackList.toArray(new ITrackInfo[mTrackList.size()]);
        mLock.unlock();
        return ijkTrackInfo;
    }

    private void getTrackInfoInner() {
        /*if (mTracksInfo != null) {
            return;
        }*/
        mLock.lock();
        mTrackList.clear();
        mTracksInfo = mInternalPlayer.getCurrentTracksInfo();
        ArrayList<Bundle> streams = new ArrayList<Bundle>();
        for (TrackGroupInfo groupInfo : mTracksInfo.getTrackGroupInfos()) {
            // Group level information.
            int trackType = groupInfo.getTrackType();
            Log.d(TAG, "groupInfo type " + trackType + " info: " + groupInfo.toString());
            boolean trackInGroupIsSelected = groupInfo.isSelected();
            boolean trackInGroupIsSupported = groupInfo.isSupported();
            TrackGroup group = groupInfo.getTrackGroup();
            for (int i = 0; i < group.length; i++) {
                // Individual track information.
                boolean isSupported = groupInfo.isTrackSupported(i);
                boolean isSelected = groupInfo.isTrackSelected(i);
                Format trackFormat = group.getFormat(i);
                Log.d(TAG, "trackFormat : " + trackFormat.toString());
                if (trackFormat.id != null && isSupported) {
                    Bundle b = new Bundle();
                    b.putString(IjkMediaMeta.IJKM_KEY_TYPE, IjkMediaMeta.IJKM_VAL_TYPE__UNKNOWN);
                    b.putString(IjkMediaMeta.IJKM_KEY_LANGUAGE, trackFormat.language);
                    b.putString(IjkMediaMeta.IJKM_KEY_CODEC_NAME, trackFormat.codecs);
                    b.putString(IjkMediaMeta.IJKM_KEY_BITRATE, String.valueOf(trackFormat.averageBitrate));
                    b.putString(IjkMediaMeta.IJKM_KEY_FORMAT, trackFormat.sampleMimeType);
                    b.putString(IjkMediaMeta.IJKM_KEY_TRACK_ID, trackFormat.id);
                    b.putString(IjkMediaMeta.IJKM_KEY_WIDTH, String.valueOf(trackFormat.width));
                    b.putString(IjkMediaMeta.IJKM_KEY_HEIGHT, String.valueOf(trackFormat.height));
                    b.putString(IjkMediaMeta.IJKM_KEY_SELECTED, String.valueOf(isSelected ? 1 : 0));
                    if (trackType == C.TRACK_TYPE_AUDIO) {
                        b.putString(IjkMediaMeta.IJKM_KEY_TYPE, IjkMediaMeta.IJKM_VAL_TYPE__AUDIO);
                    } else if (trackType == C.TRACK_TYPE_VIDEO) {
                        b.putString(IjkMediaMeta.IJKM_KEY_TYPE, IjkMediaMeta.IJKM_VAL_TYPE__VIDEO);
                    } else if (trackType == C.TRACK_TYPE_TEXT) {
                        b.putString(IjkMediaMeta.IJKM_KEY_TYPE, IjkMediaMeta.IJKM_VAL_TYPE__TIMEDTEXT);
                    } else {
                        // ignore non-audio/video/subtitles
                        continue;
                    }
                    Log.d(TAG, "Trackinfo : " + b.toString());
                    streams.add(b);
                }
            }
        }
        Bundle warpper = new Bundle();
        warpper.putParcelableArrayList("streams", streams);
        IjkMediaMeta mediaMeta = IjkMediaMeta.parse(warpper);
        for (IjkMediaMeta.IjkStreamMeta streamMeta : mediaMeta.mStreams) {
            IjkTrackInfo trackInfo = new IjkTrackInfo(streamMeta);
            if (streamMeta.mType.startsWith(IjkMediaMeta.IJKM_VAL_TYPE__VIDEO)) {
                trackInfo.setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
            } else if (streamMeta.mType.startsWith(IjkMediaMeta.IJKM_VAL_TYPE__AUDIO)) {
                trackInfo.setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);
            } else if (streamMeta.mType.startsWith(IjkMediaMeta.IJKM_VAL_TYPE__TIMEDTEXT)) {
                trackInfo.setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
            }
            mTrackList.add(trackInfo);
        }
        mLock.unlock();
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public int getVideoSarNum() {
        return mVideoSarNum;
    }

    @Override
    public int getVideoSarDen() {
        return mVideoSarDen;
    }

    @Override
    public boolean isPlaying() {
        if (mInternalPlayer == null)
            return false;
        if (mInternalPlayer.getPlaybackState() == Player.STATE_READY) {
            return mInternalPlayer.getPlayWhenReady();
        }
        return false;
    }

    @Override
    public void seekTo(long msec) {
        forwardToWorkThread(INVOKE_SEEKTO, msec);
        // currentPos = msec;
        // prevPos = msec;
    }

    public void seekToInner(long msec) {
        if (mInternalPlayer == null)
            return;
        int mediaIndex = mInternalPlayer.getCurrentMediaItemIndex();
        mInternalPlayer.seekTo(mediaIndex, msec);
    }


    @Override
    public long getCurrentPosition() {
        // if don't get newer pos from inner thread , using a fake instead to avoid block UI thread
        if (mInternalPlayer == null) return 0;
        if (inPlayerThread()) {
            mCurrentPos = getCurrentPositionInner(null);
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            forwardToWorkThread(INVOKE_GETPOSITION, latch);
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "wait getCurrentPositionInner error", e);
            }
        }
        return mCurrentPos;
    }

    private long getCurrentPositionInner(CountDownLatch latch) {
        try {
            mCurrentPos = mInternalPlayer.getCurrentPosition();
        } catch (Exception e) {
            Log.d(TAG, "get position exception " + e.getMessage());
        } finally {
            if (latch != null)
                latch.countDown();
        }
        Log.d(TAG, "current position is " + mCurrentPos);
        return mCurrentPos;
    }

    @Override
    public long getDuration() {
        if (mInternalPlayer == null) return 0;
        if (inPlayerThread()) {
            mDuration = getDurationInner(null);
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            forwardToWorkThread(INVOKE_GETDURATION, latch);
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "wait getDurationInner error", e);
            }
        }
        return mDuration;
    }

    private long getDurationInner(CountDownLatch latch) {
        try {
            mDuration = mInternalPlayer.getDuration();
        } finally {
            if (latch != null) latch.countDown();
        }
        return mDuration;
    }

    @Override
    public void reset() {
        if (mInternalPlayer == null) return;
        if (inPlayerThread()) {
            resetInner();
        } else {
            forwardToWorkThread(INVOKE_RESET, null);
        }
    }

    private void resetInner() {
        // this tow line same as stop(true)
        mInternalPlayer.stop();
        mInternalPlayer.clearMediaItems();
        // after stop STATE_IDLE will e received from exo player
    }

    @Override
    public void release() {
        if (mInternalPlayer == null) return;
        if (inPlayerThread()) {
            releaseInner();
        } else {
            forwardToWorkThread(INVOKE_RELEASE, null);
        }
    }

    public void releaseInner() {
        mInternalPlayer.removeListener(mEventListener);
        mInternalPlayer.removeAnalyticsListener(mEventLogger);
        mInternalPlayer.release();
        mInternalPlayer = null;
        mSurface = null;
        mDataSource = null;
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    @Override
    public void setLooping(boolean looping) {
        mLooping = looping;
        updateLooping();
    }

    private void updateLooping() {
        if (mInternalPlayer == null) return;
        if (mLooping) {
            mInternalPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        } else {
            mInternalPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        }
    }

    @Override
    public boolean isLooping() {
        // TODO: no support
        return false;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        float vol = (leftVolume + rightVolume) / 2;
        forwardToWorkThread(INVOKE_SETVOLUME, vol);
    }

    private void setVolumeInner(float vol) {
        mInternalPlayer.setVolume(vol);
    }

    @Override
    public int getAudioSessionId() {
        // TODO: no support
        return 0;
    }

    @Override
    public MediaInfo getMediaInfo() {
        // TODO: no support
        return null;
    }

    @Override
    public void setLogEnabled(boolean enable) {
        // do nothing
    }

    @Override
    public boolean isPlayable() {
        return true;
    }

    @Override
    public void setAudioStreamType(int streamType) {
        // do nothing
    }

    @Override
    public void setKeepInBackground(boolean keepInBackground) {
        // do nothing
    }

    @Override
    public String getVideoCodecName() {
        return mVideoCodecName;
    }

    public int getBufferedPercentage() {
        if (mInternalPlayer == null)
            return 0;
        return mInternalPlayer.getBufferedPercentage();
    }

    public ExoPlayer getInnerPlayer() {
        return mInternalPlayer;
    }

    public int getPlayerState() {
        return playerState;
    }

    public void setPlayerState(int state) {
        Log.d(TAG, "set player state: " + IJK_STATE_DESC[state + 1]);
        playerState = state;
    }

    /**
     * Makes a best guess to infer the type from a media {@link Uri}
     *
     * @param uri The {@link Uri} of the media.
     * @return The inferred type.
     */
    private static int inferContentType(Uri uri) {
        String lastPathSegment = uri.getLastPathSegment();
        return Util.inferContentType(lastPathSegment);
    }

    private ExoPlayer getInternalPlayer() {
        return mInternalPlayer;
    }

    private class PlayerEventListener implements Player.Listener {
        @Override
        public final void onPlaybackStateChanged(@Player.State int playbackState) {
            /* exo player has 4 state
                int STATE_IDLE = 1;
                int STATE_BUFFERING = 2;
                The player will be playing if  #getPlayWhenReady() is true, and paused otherwise.
                int STATE_READY = 3;
                int STATE_ENDED = 4;
            */
            int currentPlayerState = getPlayerState();
            Log.d(TAG, "exoplayer state change: " + EXO_STATE_DESC[playbackState] + " current ijk state: " + IJK_STATE_DESC[currentPlayerState + 1]);
            long now = SystemClock.elapsedRealtime();
            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    if (currentPlayerState == STATE_IDLE || currentPlayerState == STATE_PREPARING) {
                        // current state should not be idle
                        // current state is preparing and receive buffering do nothing
                    } else if (currentPlayerState != STATE_BUFFERING) {
                        mBufferingTime = now;
                        setPlayerState(STATE_BUFFERING);
                        notifyOnInfo(MEDIA_INFO_BUFFERING_START, mInternalPlayer.getBufferedPercentage(), "");
                    } else { // already in buffering state, notify buffering update
                        notifyOnBufferingUpdate(mInternalPlayer.getBufferedPercentage());
                    }
                    break;
                case Player.STATE_READY:
                    if (currentPlayerState == STATE_IDLE || currentPlayerState == STATE_PREPARING) {
                        // first time ready notify prepared to app
                        setPlayerState(STATE_PREPARED);
                        notifyOnPrepared();
                        // notify the rendering event
                        if (mTracksInfo != null && mTracksInfo.isTypeSelected(C.TRACK_TYPE_AUDIO)) {
                            notifyOnInfo(MEDIA_INFO_AUDIO_RENDERING_START, 0, "");
                        }
                        if (mTracksInfo != null && mTracksInfo.isTypeSelected(C.TRACK_TYPE_VIDEO)) {
                            notifyOnInfo(MEDIA_INFO_VIDEO_RENDERING_START, 0, "");
                        }
                    } else if (currentPlayerState == STATE_BUFFERING) {
                        // buffering -> ready notify buffering_end to app
                        notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_END, mInternalPlayer.getBufferedPercentage(), "");
                        Log.d(TAG, "exo buffering end: " + (now - mBufferingTime));
                        if (mInternalPlayer.getPlayWhenReady()) {
                            setPlayerState(STATE_PLAYING);
                        } else {
                            setPlayerState(STATE_PAUSED);
                        }
                    } else {
                        // other sate just change the state
                        if (mInternalPlayer.getPlayWhenReady()) {
                            setPlayerState(STATE_PLAYING);
                        } else {
                            setPlayerState(STATE_PAUSED);
                        }
                    }
                    break;
                case Player.STATE_ENDED:
                    setPlayerState(STATE_PLAYBACK_COMPLETED);
                    notifyOnCompletion();
                    break;
                default:
                    ;
            }
        }

        @Override
        public final void onPlayerError(PlaybackException error) {
            Log.d(TAG, "get exoplayer error " + error.errorCode + " " + error.getErrorCodeName());
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                Log.d(TAG, "seek to default position and retry " + mInternalPlayer.getCurrentMediaItemIndex());
                // internalPlayer.seekToDefaultPosition(internalPlayer.getCurrentMediaItemIndex());
                mInternalPlayer.seekToDefaultPosition();
                mInternalPlayer.prepare();
                return;
            }
            setPlayerState(STATE_ERROR);
            notifyOnError(-2, error.errorCode);
        }

        @Override
        public final void onVideoSizeChanged(VideoSize videoSize) {
            Log.d(TAG, "onVideoSizeChanged " + videoSize.pixelWidthHeightRatio);
            mVideoWidth = videoSize.width;
            mVideoHeight = videoSize.height;
            mVideoSarNum = (int) (videoSize.pixelWidthHeightRatio * 100);
            mVideoSarDen = 100;
            notifyOnVideoSizeChanged(mVideoWidth, mVideoHeight, mVideoSarNum, mVideoSarDen);
        }

        @Override
        public final void onEvents(Player player, Player.Events events) {
            StringBuilder builder = new StringBuilder();
            builder.append("exo player events:");
            for (int e = Player.EVENT_TIMELINE_CHANGED; e <= Player.EVENT_DEVICE_VOLUME_CHANGED; e++) {
                if (events.contains(e)) {
                    builder.append(" ").append(getEventString(e));
                }
            }
            Log.d(TAG, builder.toString());
        }

        @Override
        public void onTracksInfoChanged(TracksInfo tracksInfo) {
            Log.d(TAG, "exo onTracksInfoChanged");
            getTrackInfoInner();
        }

        @Override
        public void onTrackSelectionParametersChanged(TrackSelectionParameters parameters) {
            Log.d(TAG, "exo onTrackSelectionParametersChanged " + parameters);
        }

        @Override
        public void onRenderedFirstFrame() {
        }
    }

    private boolean inPlayerThread() {
        return Thread.currentThread() == mLooper.getThread();
    }

    static class EnsureSameThreadHandler extends Handler {
        WeakReference<IjkExoMediaPlayer> playerWeakReference;

        EnsureSameThreadHandler(Looper looper, IjkExoMediaPlayer ijkExoMediaPlayer) {
            super(looper);
            playerWeakReference = new WeakReference<>(ijkExoMediaPlayer);
        }

        @Override
        public void handleMessage(Message msg) {
            // process incoming messages here
            IjkExoMediaPlayer player = playerWeakReference.get();
            int what = msg.what;
            if (player == null) {
                Log.d(TAG, "receive message " + what + " but player reference is null");
                return;
            }
            switch (msg.what) {
                case INVOKE_SETSURFACE:
                    if (msg.obj == null) {
                        player.setSurfaceInner(null);
                    } else if (msg.obj instanceof Surface) {
                        player.setSurface((Surface) msg.obj);
                    } else {
                        // ignore, can't be here!
                    }
                    break;
                case INVOKE_SETDATASOURCE:
                    player.setDataSourceInner((Uri) msg.obj);
                    break;
                case INVOKE_PREPAREASYNC:
                    player.prepareAsyncInner();
                    break;
                case INVOKE_START:
                    player.startInner();
                    break;
                case INVOKE_STOP:
                    player.stopInner();
                    break;
                case INVOKE_PAUSE:
                    player.pauseInner();
                    break;
                case INVOKE_GETDURATION:
                    player.getDurationInner((CountDownLatch) msg.obj);
                    break;
                case INVOKE_GETPOSITION:
                    player.getCurrentPositionInner((CountDownLatch) msg.obj);
                    break;
                case INVOKE_RESET:
                    player.resetInner();
                    break;
                case INVOKE_RELEASE:
                    player.releaseInner();
                    break;
                case INVOKE_SEEKTO: {
                    long target = (long) msg.obj;
                    player.seekToInner(target);
                }
                break;
                case INVOKE_SELECTTRACK: {
                    int target = (int) msg.obj;
                    int trackType = (int) target >> 16;
                    int trackId = (int) target & 0xFFFF;
                    player.selectTrackInner(trackType, trackId);
                }
                break;
                case INVOKE_SELECTTRACK2: {
                    int trackType = msg.arg1;
                    String trackId = (String) msg.obj;
                    player.selectTrackInner(trackType, trackId);
                }
                break;
                case INVOKE_SETVOLUME: {
                    float vol = (float) msg.obj;
                    player.setVolumeInner(vol);
                }
                break;
                default:
                    Log.d(TAG, "receive unexpected msg : " + msg.what);
            }
        }
    }

    private static String getEventString(int event) {
        switch (event) {
            case Player.EVENT_TIMELINE_CHANGED:
                return "timeline_changed";
            case Player.EVENT_MEDIA_ITEM_TRANSITION:
                return "media_item_trans";
            case Player.EVENT_TRACKS_CHANGED:
                return "tracks_changed";
            case Player.EVENT_IS_LOADING_CHANGED:
                return "loading_changed";
            case Player.EVENT_PLAYBACK_STATE_CHANGED:
                return "play_state_changed";
            case Player.EVENT_PLAY_WHEN_READY_CHANGED:
                return "when_ready_changed";
            case Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED:
                return "supp_reason_changed";
            case Player.EVENT_IS_PLAYING_CHANGED:
                return "playing_changed";
            case Player.EVENT_REPEAT_MODE_CHANGED:
                return "repeat_mode_changed";
            case Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED:
                return "shuffle_mode_changed";
            case Player.EVENT_PLAYER_ERROR:
                return "play_error";
            case Player.EVENT_POSITION_DISCONTINUITY:
                return "position_discontinuity";
            case Player.EVENT_PLAYBACK_PARAMETERS_CHANGED:
                return "play_param_changed";
            case Player.EVENT_AVAILABLE_COMMANDS_CHANGED:
                return "available_cmd_changed";
            case Player.EVENT_MEDIA_METADATA_CHANGED:
                return "media_metadata_changed";
            case Player.EVENT_PLAYLIST_METADATA_CHANGED:
                return "playlist_metadata_changed";
            case Player.EVENT_SEEK_BACK_INCREMENT_CHANGED:
                return "seek_back_increment_changed";
            case Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED:
                return "seek_forward_increment_changed";
            case Player.EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED:
                return "max_seek_changed";
            case Player.EVENT_TRACK_SELECTION_PARAMETERS_CHANGED:
                return "track_selection_param_changed";
            case Player.EVENT_AUDIO_ATTRIBUTES_CHANGED:
                return "audio_attr_changed";
            case Player.EVENT_AUDIO_SESSION_ID:
                return "audio_session_id";
            case Player.EVENT_VOLUME_CHANGED:
                return "volume_changed";
            case Player.EVENT_SKIP_SILENCE_ENABLED_CHANGED:
                return "skip_silence_changed";
            case Player.EVENT_SURFACE_SIZE_CHANGED:
                return "surface_size_changed";
            case Player.EVENT_VIDEO_SIZE_CHANGED:
                return "video_size_changed";
            case Player.EVENT_RENDERED_FIRST_FRAME:
                return "rendered_first_frame";
            case Player.EVENT_CUES:
                return "cues";
            case Player.EVENT_METADATA:
                return "metadata";
            case Player.EVENT_DEVICE_INFO_CHANGED:
                return "device_info_changed";
            case Player.EVENT_DEVICE_VOLUME_CHANGED:
                return "device_volume_changed";
        }
        return "unknown";
    }
}
