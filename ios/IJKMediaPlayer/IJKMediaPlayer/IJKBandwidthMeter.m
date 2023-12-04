//
//  IJKBandwidthMeter.m
//  IJKMediaPlayer
//
//  Created by Gibbs on 2023/10/25.
//  Copyright © 2023 valoroso. All rights reserved.
//

#import "IJKBandwidthMeter.h"
#import "IJKSlidingPercentile.h"

#define DEFAULT_INITIAL_BITRATE_ESTIMATE   1000000

#define DEFAULT_SLIDING_WINDOW_MAX_WEIGHT  4000

#define DEFAULT_SLIDING_WINDOW_PERCENTILE  0.5f

#define ELAPSED_MILLIS_FOR_ESTIMATE        2000

#define BYTES_TRANSFERRED_FOR_ESTIMATE     524288


@implementation IJKDefaultBandwidthMeter {
    IJKSlidingPercentile* _slidingPercentile;
    float _percentile;
    long _totalElapsedTimeMs;
    long _totalBytesTransferred;
    long _bitrateEstimate;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        self->_bitrateEstimate = DEFAULT_INITIAL_BITRATE_ESTIMATE;
        self->_slidingPercentile = [[IJKSlidingPercentile alloc] initWithMaxWeight:DEFAULT_SLIDING_WINDOW_MAX_WEIGHT];
        self->_percentile = DEFAULT_SLIDING_WINDOW_PERCENTILE;
    }
    return self;
}

- (id)initWithBitrate:(long)bitrate maxWeight:(int)maxWeight percentile:(float)percentile {
    self = [super init];
    if (self) {
        self->_bitrateEstimate = bitrate;
        self->_slidingPercentile = [[IJKSlidingPercentile alloc] initWithMaxWeight:maxWeight];
        self->_percentile = percentile;
    }
    return self;
}

- (long long)getBitrateEstimate {
    return (long long) (_bitrateEstimate * 0.7f);
}

- (void)addSample:(long long)sampleBytesTransferred sampleElapsedTimeMs:(long long)sampleElapsedTimeMs {
    _totalElapsedTimeMs += sampleElapsedTimeMs;
    _totalBytesTransferred += sampleBytesTransferred;
    if (sampleElapsedTimeMs > 0) {
        float bitsPerSecond = (sampleBytesTransferred * 8000.0f) / sampleElapsedTimeMs;
        [_slidingPercentile addSample:(int) sqrt(sampleBytesTransferred) value:bitsPerSecond];
        if (_totalElapsedTimeMs >= ELAPSED_MILLIS_FOR_ESTIMATE
            || _totalBytesTransferred >= BYTES_TRANSFERRED_FOR_ESTIMATE) {
            _bitrateEstimate = [_slidingPercentile getPercentile:_percentile];
            NSLog(@"DefaultBandwidthMeter onTransferEnd bitrateEstimate changed to %.2fMbps", (_bitrateEstimate / 1024.0f / 1024.0f));
        }
    }
}

@end
