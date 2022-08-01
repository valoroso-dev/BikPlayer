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
import android.view.Surface;
import android.view.SurfaceHolder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;

import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaItem.Builder;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.TracksInfo;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.DebugTextViewHelper;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.TracksInfo.TrackGroupInfo;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides.TrackSelectionOverride;

import android.util.Log;

import java.io.FileDescriptor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.*;
import java.util.ArrayList;
import java.util.Arrays;

import tv.danmaku.ijk.media.player.AbstractMediaPlayer;
import tv.danmaku.ijk.media.player.AbstractMediaPlayer.*;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.MediaInfo;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;
import tv.danmaku.ijk.media.player.IjkMediaMeta;
import tv.danmaku.ijk.media.player.IjkMediaMeta.IjkStreamMeta;

public class IjkExoMediaPlayer extends AbstractMediaPlayer implements Runnable {
    String TAG = "IjkExoMediaPlayer";
    private Context mAppContext;
    private ExoPlayer mInternalPlayer;
    private EventLogger mEventLogger;
    private String mDataSource;
    private MediaItem mediaItem;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int playerstate = STATE_IDLE;
    private Surface mSurface;
    private DefaultTrackSelector trackSelector;
    protected StyledPlayerView playerView;
    private DataSource.Factory dataSourceFactory;
    private Handler mHandler;
    DrmSessionManager drmSessionManager;
    MediaSource mediaSource;
    private TracksInfo tracksInfo;
    private ArrayList<IjkTrackInfo> trackList;
    private ReentrantLock lock;
    int selectTrack[]; 
    static IjkExoMediaPlayer singleton;
    
    long bufferingtime = 0;
    long duration = 0;
    long currentPos = 0;
    long prevPos = 0;
    static final String statedesc[] = {"STATE_UNKNOW", "STATE_IDLE","STATE_BUFFERING","STATE_READY","STATE_ENDED"};
    static final int INVOKE_SETSURFACE = 0;
    static final int INVOKE_SETDATASOURCE = 1;
    static final int INVOKE_PREPAREASYNC = 2;
    static final int INVOKE_START = 3;
    static final int INVOKE_STOP = 4;
    static final int INVOKE_PAUSE = 5;
    static final int INVOKE_GETDURATION = 6;
    static final int INVOKE_GETPOSITION = 7;
    static final int INVOKE_SEEKTO = 8;
    static final int INVOKE_GETTRACKINFO = 9;
    static final int INVOKE_SELECTTRACK = 10;
    static final int INVOKE_RESET = 19;
    static final int INVOKE_RELEASE = 20;

    public IjkExoMediaPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        lock = new ReentrantLock();
        selectTrack = new int[4];
        Arrays.fill(selectTrack, -1); 
        trackList = new ArrayList<IjkTrackInfo>();
        singleton = this;
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
            // process incoming messages here
            switch(msg.what) {
                case INVOKE_SETSURFACE: singleton.setSurfaceInner(mSurface);break;
                case INVOKE_SETDATASOURCE: singleton.setDataSourceInner(mAppContext,(Uri)msg.obj);break;
                case INVOKE_PREPAREASYNC: singleton.prepareAsyncInner();break;
                case INVOKE_START: singleton.startInner();break;
                case INVOKE_STOP: singleton.stopInner();break;
                case INVOKE_PAUSE: singleton.pauseInner();break;
                case INVOKE_GETDURATION: singleton.getDurationInner();break;
                case INVOKE_GETPOSITION: singleton.getCurrentPositionInner();break;
                case INVOKE_RESET: singleton.resetInner();break;
                case INVOKE_RELEASE: singleton.releaseInner();break;
                case INVOKE_SEEKTO: {
                    long target = (long) msg.obj;
                    singleton.seekToInner(target);
                }                
                break;
                case INVOKE_GETTRACKINFO: singleton.getTrackInfoInner();break;
                case INVOKE_SELECTTRACK: {
                    int target = (int) msg.obj;
                    int trackType = (int)target>>16;
                    int trackId = (int)target&0xFFFF;
                    singleton.selectTrackInner(trackType, trackId);
                }
                break;
                default: Log.d(TAG, "receive unexpected msg : " + msg.what);
                }
            }
        };
        eventListener = new PlayerEventListener(this);
        trackSelector = new DefaultTrackSelector(mAppContext);
        mEventLogger = new EventLogger(trackSelector);
        Thread workThread = new Thread(this);
        workThread.start();
    }

    @Override
    public void run() {
        Log.d(TAG, "thread fun " + System.currentTimeMillis());
    }
    public void forwardtoWorkThread(int what, Object obj) {
        String endPoint = "";
        Object par = obj == null ? endPoint : obj;
        Message msg = Message.obtain(mHandler, what, par);
        msg.sendToTarget();
    }


    @Override
    public void setDisplay(SurfaceHolder sh) {
        Log.d(TAG, "holder is " + sh);
        setSurface(sh == null ? null : sh.getSurface());
    }

    @Override
    public void setSurface(Surface surface) {
        mSurface = surface;
        forwardtoWorkThread(INVOKE_SETSURFACE,null);
    }

    public void setSurfaceInner(Surface surface) {
        if (mInternalPlayer != null)
            mInternalPlayer.setVideoSurface(surface);
    }

    @Override
    public void setDataSource(Context context, Uri uri) {
        forwardtoWorkThread(INVOKE_SETDATASOURCE,uri);
    }
    public void setDataSourceInner(Context context, Uri uri) {
        mDataSource = uri.toString();
        //create mediaitem
        Log.d(TAG, "call setDataSource with drmurl is " + getDrmlicenseServerUrl());
        if (getDrmType() != IMediaPlayer.DRM_TYPE_NON) {
            MediaItem.Builder builder = new MediaItem.Builder();
            builder.setUri(mDataSource);
            //builder.setMediaMetadata(new MediaMetadata.Builder().setTitle(getMediaMetadata()).build());
            builder.setMimeType(getMimetype());
            MediaItem.DrmConfiguration.Builder configurationBuilder = new MediaItem.DrmConfiguration.Builder(getUUID());
            builder.setDrmConfiguration(configurationBuilder.setLicenseUri(getDrmlicenseServerUrl()).build());
            Map<String,String> headers = getReqHeaders();
            mediaItem = builder.build();
            String drmLicenseUrl = getDrmlicenseServerUrl();
            HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSource.Factory();
            HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(drmLicenseUrl, licenseDataSourceFactory);
            for (Map.Entry<String, String> iter : headers.entrySet()) {
                drmCallback.setKeyRequestProperty(iter.getKey(), iter.getValue());
            }
            drmSessionManager = new DefaultDrmSessionManager.Builder()
                               .setUuidAndExoMediaDrmProvider(getUUID(), FrameworkMediaDrm.DEFAULT_PROVIDER)
                               .build(drmCallback);
        } else {
            mediaItem = MediaItem.fromUri(mDataSource);
            drmSessionManager = DrmSessionManager.DRM_UNSUPPORTED;
        }
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(mAppContext);
        if(getStreamType() == IMediaPlayer.STREAM_DASH) {
            mediaSource = new DashMediaSource.Factory(dataSourceFactory)
                          .setDrmSessionManager(drmSessionManager)
                          .createMediaSource(mediaItem);
        } else if(getStreamType() == IMediaPlayer.STREAM_HLS){
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                          .setDrmSessionManager(drmSessionManager)
                          .createMediaSource(mediaItem);
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                          .setDrmSessionManager(drmSessionManager)
                          .createMediaSource(mediaItem);
        }
    }
    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) {
        // TODO: handle headers
        setDataSource(context, uri);
    }
    @Override
    public void setTrack(int trackType, int trackId) {
        int par = (trackType<<16) |trackId;
        forwardtoWorkThread(INVOKE_SELECTTRACK, par);
    };
    public void selectTrackInner(int trackType, int trackId) {
        Log.d(TAG, "track type " + trackType + " id " + trackId);
        //TracksInfo trackInfo = mInternalPlayer.getCurrentTracksInfo();
        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            // get track group by tracktype and trackid
            TrackGroupArray groups = mappedTrackInfo.getTrackGroups​(trackType);
            TrackGroup targettrackgroup = null;
            for(int i = 0; i<groups.length;i++) {
                if (targettrackgroup != null) {
                    break;
                }
                TrackGroup trackgroup = groups.get(i);
                for(int j = 0;j<trackgroup.length;j++) {
                    Format trackFormat = trackgroup.getFormat(j);
                    if (trackFormat.id == Integer.toString(trackId)) {
                        targettrackgroup = trackgroup;
                        break;
                    }
                }
            }
            if (targettrackgroup == null) {
                Log.d(TAG, "Can't find target track " + trackId);
                return;
            }
            TrackSelectionOverrides overrides = new TrackSelectionOverrides.Builder()
                .setOverrideForType(new TrackSelectionOverride(targettrackgroup))
                .build();
            mInternalPlayer.setTrackSelectionParameters(mInternalPlayer.getTrackSelectionParameters()
                .buildUpon().setTrackSelectionOverrides(overrides).build());
        } else {
            Log.d(TAG, "no mapped trackinfo");
        }

    };
    @Override
    public int getCurrentTrack(int trackType) {
        return selectTrack[trackType];
    };

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
        return mDataSource;
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        forwardtoWorkThread(INVOKE_PREPAREASYNC, null);
    }
    public void prepareAsyncInner() throws IllegalStateException {
        if (mInternalPlayer != null)
            throw new IllegalStateException("can't prepare a prepared player");
        mInternalPlayer = new ExoPlayer.Builder(mAppContext).setTrackSelector(trackSelector).
        build();
        mInternalPlayer.addListener(eventListener);
        mInternalPlayer.addAnalyticsListener(mEventLogger);

        if (mSurface != null)
            mInternalPlayer.setVideoSurface(mSurface);
        mInternalPlayer.setMediaSource(mediaSource);
        mInternalPlayer.prepare();
        setPlayerState(STATE_PREPARING);
    }

    @Override
    public void start() throws IllegalStateException {
        forwardtoWorkThread(INVOKE_START, null);
    }
    public void startInner() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.setPlayWhenReady(true);
    }

    @Override
    public void stop() throws IllegalStateException {
        forwardtoWorkThread(INVOKE_STOP, null);
    }
    public void stopInner() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        Log.d(TAG, "app call stop on exoplayer");
        mInternalPlayer.release();
        mInternalPlayer = null;
    }

    @Override
    public void pause() throws IllegalStateException {
        forwardtoWorkThread(INVOKE_PAUSE, null);
    }
    public void pauseInner() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.setPlayWhenReady(false);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        // FIXME: implement
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        // TODO: do nothing
    }

    @Override
    public ITrackInfo[] getTrackInfo() {
        ITrackInfo[] ijktrackinfo = null;
        lock.lock();
        ijktrackinfo = trackList.toArray(new ITrackInfo[trackList.size()]);
        lock.unlock();
        return ijktrackinfo;
    }
    public void getTrackInfoInner() {
        lock.lock();
        if(tracksInfo != null) {
            lock.unlock();
            return;
        }
        tracksInfo = mInternalPlayer.getCurrentTracksInfo();
        ArrayList<Bundle> streams = new ArrayList<Bundle>();
        for (TrackGroupInfo groupInfo : tracksInfo.getTrackGroupInfos()) {
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
              if(trackFormat.id != null && isSupported) {
                Bundle b = new Bundle();
                b.putString(IjkMediaMeta.IJKM_KEY_TYPE, IjkMediaMeta.IJKM_VAL_TYPE__UNKNOWN);
                b.putString(IjkMediaMeta.IJKM_KEY_LANGUAGE, trackFormat.language);
                b.putString(IjkMediaMeta.IJKM_KEY_CODEC_NAME, trackFormat.codecs);
                b.putString(IjkMediaMeta.IJKM_KEY_BITRATE, String.valueOf(trackFormat.averageBitrate));
                b.putString(IjkMediaMeta.IJKM_KEY_FORMAT, trackFormat.sampleMimeType);
                b.putString(IjkMediaMeta.IJKM_KEY_TYPE, trackFormat.sampleMimeType);
                b.putString(IjkMediaMeta.IJKM_KEY_TRACK_ID, trackFormat.id);
                if (trackType == C.TRACK_TYPE_AUDIO || trackType == C.TRACK_TYPE_VIDEO 
                    || trackType == C.TRACK_TYPE_TEXT ) {
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
        warpper.putParcelableArrayList("streams",streams);
        IjkMediaMeta mediaMeta = IjkMediaMeta.parse(warpper);
        for (IjkMediaMeta.IjkStreamMeta streamMeta: mediaMeta.mStreams) {
            IjkTrackInfo trackInfo = new IjkTrackInfo(streamMeta);
            if (streamMeta.mType.startsWith(IjkMediaMeta.IJKM_VAL_TYPE__VIDEO)) {
                trackInfo.setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
            } else if (streamMeta.mType.startsWith(IjkMediaMeta.IJKM_VAL_TYPE__AUDIO)) {
                trackInfo.setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);
            } else if (streamMeta.mType.startsWith(IjkMediaMeta.IJKM_VAL_TYPE__TIMEDTEXT)) {
                trackInfo.setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
            }
            trackList.add(trackInfo);
        }
        lock.unlock();
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
        forwardtoWorkThread(INVOKE_SEEKTO, msec);
        currentPos = msec;
        prevPos = msec;
    }
    public void seekToInner(long msec) {
        if (mInternalPlayer == null)
            return;
        int mediaIndex = mInternalPlayer.getCurrentMediaItemIndex();
        mInternalPlayer.seekTo(mediaIndex, msec);
    }


    @Override
    public long getCurrentPosition() {
        forwardtoWorkThread(INVOKE_GETPOSITION, null);
        // if don't get newer pos from innner thread , using a fake instead to avoid block UI thread
        if(prevPos == currentPos) {
            if(playerstate == STATE_PLAYING) {
                prevPos += 10;
            }
            currentPos = prevPos;
            return prevPos;
        }
        prevPos = currentPos;
        return currentPos;
    }
    public long getCurrentPositionInner() {
        try {
            currentPos  = mInternalPlayer.getCurrentPosition();
        } catch(Exception e) {
            Log.d(TAG, "get position exception " + e);
        }
        Log.d(TAG, "current position is " + currentPos);
        return currentPos;
    }

    @Override
    public long getDuration() {
        forwardtoWorkThread(INVOKE_GETDURATION, null);
        return duration;
    }
    public long getDurationInner() {
        if (mInternalPlayer == null)
            return 0;
        duration =  mInternalPlayer.getDuration();
        return duration;
    }

    @Override
    public void reset() {
        forwardtoWorkThread(INVOKE_RESET, null);
    }
    public void resetInner() {
        if (mInternalPlayer != null) {
            mInternalPlayer.removeListener(eventListener);
            mInternalPlayer.removeAnalyticsListener(mEventLogger);
            mInternalPlayer.release();
            mInternalPlayer = null;
        }
        mSurface = null;
        mDataSource = null;
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    @Override
    public void setLooping(boolean looping) {
        // TODO: no support
        throw new UnsupportedOperationException("no support");
    }

    @Override
    public boolean isLooping() {
        // TODO: no support
        return false;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        // TODO: no support
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
    public void setAudioStreamType(int streamtype) {
        // do nothing
    }

    @Override
    public void setKeepInBackground(boolean keepInBackground) {
        // do nothing
    }

    @Override
    public void release() {
        forwardtoWorkThread(INVOKE_RELEASE, null);
    }
    public void releaseInner() {
        if (mInternalPlayer != null) {
            reset();
            eventListener = null;
            mEventLogger = null;
        }
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
        return playerstate;
    }
    public void setPlayerState(int state) {
        if (playerstate == state) {
            return;
        }
        Log.d(TAG, "enter setstate with " + state);
        playerstate = state;
        //notify app
        switch(playerstate) {
            case STATE_ERROR: notifyOnError(IMediaPlayer.MEDIA_ERROR_UNKNOWN, IMediaPlayer.MEDIA_ERROR_UNKNOWN);break;
            case STATE_IDLE: notifyOnCompletion();break;
            case STATE_PREPARING: break;
            case STATE_PREPARED: notifyOnPrepared();break;
            case STATE_PLAYING: break;
            case STATE_PAUSED: break;
            case STATE_PLAYBACK_COMPLETED: break;
            case STATE_BUFFERING: break;
            default: break;
        }

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
        private boolean mIsPrepareing = false;
        private boolean mDidPrepare = false;
        private boolean mIsBuffering = false;
        private IjkExoMediaPlayer player;

        PlayerEventListener(IjkExoMediaPlayer p) {
            player = p;
        }
        @Override
        public final void onPlaybackStateChanged(@Player.State int playbackState) {
            /* exo player has 4 state
                int STATE_IDLE = 1;
                int STATE_BUFFERING = 2;
                The player will be playing if  #getPlayWhenReady() is true, and paused otherwise.
                int STATE_READY = 3;
                int STATE_ENDED = 4;
            */
            Log.d(TAG, "exoplayer state change to " + IjkExoMediaPlayer.statedesc[playbackState]);
            int oldPlayerState = player.getPlayerState();
            int targetPlayerState = player.getPlayerState();
            /*
            if (oldPlayerState == STATE_BUFFERING || oldPlayerState == STATE_PREPARED) {
                switch (playbackState) {
                    case Player.STATE_ENDED:
                    case Player.STATE_READY:
                        notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_END, mInternalPlayer.getBufferedPercentage());
                        targetPlayerState = player.getInnerPlayer().getPlayWhenReady() ? STATE_PLAYING : STATE_PAUSED;
                        Log.d(TAG, "here target is " + targetPlayerState);
                        break;
                }
            }
            */
            // get track before notify prepared to app
            if (playbackState == Player.STATE_READY) {
                player.getTrackInfoInner();
            }
            long currenttime = System.currentTimeMillis();
            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    //notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_START, mInternalPlayer.getBufferedPercentage());
                    bufferingtime = currenttime;
                    targetPlayerState = STATE_BUFFERING;
                break;
                case Player.STATE_READY:
                    setPlayerState(STATE_PREPARED); // push app state change
                    notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_END, mInternalPlayer.getBufferedPercentage());
                    targetPlayerState = player.getInnerPlayer().getPlayWhenReady() ? STATE_PLAYING : STATE_PAUSED;
                break;
                case Player.STATE_ENDED:
                    targetPlayerState = STATE_IDLE;
                break;
                default: ;
            }
            player.setPlayerState(targetPlayerState);
        }
        @Override
        public final void onPlayerError(PlaybackException error) {
            Log.d(TAG, "get exoplayer error " + error.errorCode);
            ExoPlayer internalPlayer = player.getInternalPlayer();
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                Log.d(TAG, "seek to default pos and retry " + internalPlayer.getCurrentMediaItemIndex());
                internalPlayer.seekToDefaultPosition(internalPlayer.getCurrentMediaItemIndex());
                //internalPlayer.prepare();
                internalPlayer.play();
            }
            notifyOnError(IMediaPlayer.MEDIA_ERROR_UNKNOWN, IMediaPlayer.MEDIA_ERROR_UNKNOWN);
        }
        @Override
        public final void onVideoSizeChanged(VideoSize videoSize) {
            Log.d(TAG, "onVideoSizeChanged " + videoSize.pixelWidthHeightRatio);
            mVideoWidth = videoSize.width;
            mVideoHeight = videoSize.height;
            mVideoSarNum = (int)(videoSize.pixelWidthHeightRatio*100);
            mVideoSarDen = 100;
            notifyOnVideoSizeChanged(mVideoWidth, mVideoHeight, mVideoSarNum,mVideoSarDen);
        }
        @Override
        public final void onEvents(Player player, Player.Events events) {
            Log.d(TAG, "player events " + events.toString());
        }
    }
    private PlayerEventListener eventListener;
}
