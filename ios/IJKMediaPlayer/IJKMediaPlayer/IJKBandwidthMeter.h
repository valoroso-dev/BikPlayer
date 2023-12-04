//
//  IJKBandwidthMeter.h
//  IJKMediaPlayer
//
//  Created by Gibbs on 2023/10/25.
//  Copyright © 2023 valoroso. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@protocol IJKBandwidthMeter <NSObject>

- (long long)getBitrateEstimate;

- (void)addSample:(long long)sampleBytesTransferred sampleElapsedTimeMs:(long long)sampleElapsedTimeMs;

@end

@interface IJKDefaultBandwidthMeter : NSObject <IJKBandwidthMeter>

- (id)initWithBitrate:(long)bitrate maxWeight:(int)maxWeight percentile:(float)percentile;

@end

NS_ASSUME_NONNULL_END
