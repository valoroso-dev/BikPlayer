package tv.danmaku.ijk.media.common.abr;

public interface BandwidthMeter {
    long getBitrateEstimate();

    void addSample(long sampleBytesTransferred, long sampleElapsedTimeMs);
}
