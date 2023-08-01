package tv.danmaku.ijk.media.common.drm;

public class DrmConstant {
    public static final int AUDIO_INDEX = 0;
    public static final int VIDEO_INDEX = 1;

    public static final int ACQUIRE_SESSION_FLAG_SEEK = 0x01;
    public static final int ACQUIRE_SESSION_FLAG_INIT = 0x02;

    public enum DrmSessionState {
        STATE_UNKNOWN, STATE_WAITING, STATE_LOADING, STATE_LOADED
    }

    public static final String EXTERNAL_DRM_MANAGER_CLASS = "tv.danmaku.ijk.media.drm.wrapper.DefaultDrmManager";
}
