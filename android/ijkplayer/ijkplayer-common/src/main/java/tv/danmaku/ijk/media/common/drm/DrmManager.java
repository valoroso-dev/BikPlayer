package tv.danmaku.ijk.media.common.drm;

import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.RequiresApi;

import static tv.danmaku.ijk.media.common.drm.DrmConstant.EXTERNAL_DRM_MANAGER_CLASS;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public interface DrmManager {
    class Factory {
        public DrmManager create(String drmLicenceUrl, Map<String, String> httpRequestHeaders, UUID uuid, boolean multiSession) {
            return create(drmLicenceUrl, httpRequestHeaders, uuid, multiSession, true, 2);
        }

        public DrmManager create(String drmLicenceUrl, Map<String, String> httpRequestHeaders, UUID uuid, boolean multiSession, boolean useOkhttp, int level) {
            DrmManager drmManager = null;
            try {
                drmManager = createExternalDrmManager(drmLicenceUrl, httpRequestHeaders, uuid, multiSession, useOkhttp, level);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (drmManager == null) {
                    drmManager = new DummyDrmManager();
                }
            }

            return drmManager;
        }

        private DrmManager createExternalDrmManager(String drmLicenceUrl, Map<String, String> httpRequestHeaders, UUID uuid, boolean multiSession, boolean useOkhttp, int level)
                throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
            Class<?> c = Class.forName(EXTERNAL_DRM_MANAGER_CLASS);
            Constructor<?> constructor = c.getDeclaredConstructor(String.class, Map.class, UUID.class, boolean.class, boolean.class, int.class);
            return (DrmManager) constructor.newInstance(drmLicenceUrl, httpRequestHeaders, uuid, multiSession, useOkhttp, level);
        }
    }

    class DummyDrmManager implements DrmManager {
        @Override
        public void prepare() {

        }

        @Override
        public void release() {

        }

        @Override
        public DrmConstant.DrmSessionState acquireSession(DrmInitInfo drmInitInfo, int flag) {
            return DrmConstant.DrmSessionState.STATE_LOADED;
        }

        @Override
        public MediaCrypto getMediaCrypto(int type) {
            return null;
        }

        @Override
        public DrmConstant.DrmSessionState getDrmSessionState(int type, int flag) {
            return DrmConstant.DrmSessionState.STATE_LOADED;
        }

        @Override
        public void setOnDrmErrorListener(OnDrmErrorListener listener) {

        }

        @Override
        public void setOfflineLicenseKeySetId(byte[] offlineLicenseKeySetId) {

        }
    }

    /**
     * called when the player start preparing
     */
    void prepare();

    /**
     * called when the player released
     */
    void release();

    /**
     * upload a DrmInitInfo. The method will be called when a new AVPacket with drm init info queued in ff_ffplay.c
     *
     * @param drmInitInfo drm init info
     * @param flag        ACQUIRE_SESSION_FLAG_*
     * @return the drm session state STATE_*
     */
    DrmConstant.DrmSessionState acquireSession(final DrmInitInfo drmInitInfo, final int flag);

    /**
     * get {@link MediaCrypto} for {@link android.media.MediaCodec#configure(MediaFormat, Surface, MediaCrypto, int)}
     *
     * @param type must be AUDIO_INDEX or VIDEO_INDEX
     * @return {@link MediaCrypto}
     */
    MediaCrypto getMediaCrypto(final int type);

    /**
     * get current drm session state
     *
     * @param type must be AUDIO_INDEX or VIDEO_INDEX
     * @param flag ACQUIRE_SESSION_FLAG_*
     * @return the drm session state STATE_*
     */
    DrmConstant.DrmSessionState getDrmSessionState(final int type, final int flag);

    /**
     * set {@link OnDrmErrorListener }
     *
     * @param listener listener
     */
    void setOnDrmErrorListener(OnDrmErrorListener listener);

    /**
     * if an offlineLicenseKeySetId is set, the drm session will using it.
     *
     * @param offlineLicenseKeySetId The offline license key set identifier
     */
    void setOfflineLicenseKeySetId(byte[] offlineLicenseKeySetId);
}
