package tv.danmaku.ijk.media.player;

import android.media.MediaCrypto;
import android.os.Bundle;

@SimpleCClassName
public class IjkMediaPlayer {
    private long mNativeMediaPlayer;
    private long mNativeMediaDataSource;
    private long mNativeAndroidIO;

    private static void postEventFromNative(Object weakThiz, int what, int arg1, int arg2, Object obj);
    private static String onSelectCodec(Object weakThiz, String mimeType, int profile, int level);
    private static boolean onNativeInvoke(Object weakThiz, int what, Bundle args);
    private static int onDrmInitInfoUpdated(Object weakThiz, String stringObj, byte[] bytesObj, int flag);
    private static MediaCrypto getMediaCrypto(Object weakThiz, int type);
    private static int getDrmSessionState(Object weakThiz, int type, int flag);
}
