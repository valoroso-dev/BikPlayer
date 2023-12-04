package tv.danmaku.ijk.media.common.abr;

import android.util.Log;

public class DefaultBandwidthMeter implements BandwidthMeter {
    /**
     * Default initial bitrate estimate used when the device is offline or the network type cannot be
     * determined, in bits per second.
     */
    public static final long DEFAULT_INITIAL_BITRATE_ESTIMATE = 1_000_000;

    /**
     * Default maximum weight for the sliding window.
     */
    public static final int DEFAULT_SLIDING_WINDOW_MAX_WEIGHT = 4000;

    /**
     * Default percentile for the sliding window.
     */
    public static final float DEFAULT_SLIDING_WINDOW_PERCENTILE = 0.5f;
    /**
     * Default percentile for the bitrate estimate.
     */
    public static final float DEFAULT_BITRATE_ESTIMATE = 0.7f;

    private static final int ELAPSED_MILLIS_FOR_ESTIMATE = 2000;
    private static final int BYTES_TRANSFERRED_FOR_ESTIMATE = 512 * 1024;

    private final SlidingPercentile slidingPercentile;
    private final float slidingPercentileValue;
    private final float bitratePercentileValue;

    private long totalElapsedTimeMs;
    private long totalBytesTransferred;
    private long bitrateEstimate;

    public DefaultBandwidthMeter() {
        this(DEFAULT_INITIAL_BITRATE_ESTIMATE, DEFAULT_SLIDING_WINDOW_MAX_WEIGHT, DEFAULT_SLIDING_WINDOW_PERCENTILE, DEFAULT_BITRATE_ESTIMATE);
    }

    public DefaultBandwidthMeter(long bitrate, int maxWeight, float slidingPercentile, float bitratePercentile) {
        this.bitrateEstimate = bitrate;
        this.slidingPercentile = new SlidingPercentile(maxWeight);
        this.slidingPercentileValue = slidingPercentile;
        this.bitratePercentileValue = bitratePercentile;
        Log.d("DefaultBandwidthMeter", "bitrateEstimate=" + bitrate + ", maxWeight=" + maxWeight + ", siding percentile=" + slidingPercentile + ", bitrate percentile=" + bitratePercentile);
    }

    @Override
    public synchronized long getBitrateEstimate() {
        return (long) (bitrateEstimate * bitratePercentileValue);
    }

    @Override
    public void addSample(long sampleBytesTransferred, long sampleElapsedTimeMs) {
        totalElapsedTimeMs += sampleElapsedTimeMs;
        totalBytesTransferred += sampleBytesTransferred;
        if (sampleElapsedTimeMs > 0) {
            float bitsPerSecond = (sampleBytesTransferred * 8000f) / sampleElapsedTimeMs;
            slidingPercentile.addSample((int) Math.sqrt(sampleBytesTransferred), bitsPerSecond);
            if (totalElapsedTimeMs >= ELAPSED_MILLIS_FOR_ESTIMATE
                    || totalBytesTransferred >= BYTES_TRANSFERRED_FOR_ESTIMATE) {
                bitrateEstimate = (long) slidingPercentile.getPercentile(slidingPercentileValue);
                Log.d("DefaultBandwidthMeter", String.format("onTransferEnd bitrateEstimate changed to %.2fMbps", (bitrateEstimate / 1024f / 1024f)));
            }
        }
    }
}
