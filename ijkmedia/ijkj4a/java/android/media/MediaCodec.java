package android.media;

import java.nio.ByteBuffer;
import android.view.Surface;

@SimpleCClassName
@MinApi(16)
public class MediaCodec {
    public static class BufferInfo {
        public int  flags;
        public int  offset;
        public long presentationTimeUs;
        public int  size;

        public BufferInfo();
    }

    public static class CryptoInfo {
        public byte[] iv;
        public byte[] key;
        public int mode;
        public int[] numBytesOfClearData;
        public int[] numBytesOfEncryptedData;
        public int numSubSamples;

        public CryptoInfo();

        public void set(int newNumSubSamples, int[] newNumBytesOfClearData, int[] newNumBytesOfEncryptedData, byte[] newKey, byte[] newIV, int newMode);

        public String toString();

	@MinApi(24)
        public static class Pattern {
            public Pattern(int blocksToEncrypt, int blocksToSkip);

            public void set(int blocksToEncrypt, int blocksToSkip);
        }

	@MinApi(24)
        public void setPattern(Pattern newPattern);
    }

    public static MediaCodec createByCodecName(String name);

    public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags);

    public final MediaFormat getOutputFormat();

    public ByteBuffer[] getInputBuffers();

    public final int  dequeueInputBuffer(long timeoutUs);
    public final void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags);
    public final void queueSecureInputBuffer(int index, int offset, MediaCodec.CryptoInfo info, long presentationTimeUs, int flags);

    public final int  dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs);
    public final void releaseOutputBuffer(int index, boolean render);

    public final void start();
    public final void stop();
    public final void flush();
    public final void release();
}
