//
//  IJKSlidingPercentile.h
//  IJKMediaPlayer
//
//  Created by Gibbs on 2023/10/25.
//  Copyright © 2023 valoroso. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface IJKSlidingPercentile : NSObject

- (id)initWithMaxWeight:(int)maxWeight;

- (void)reset;

- (void)addSample:(int)weight value:(float)value;

- (float)getPercentile:(float)percentile;

@end

@interface Sample : NSObject

@property(nonatomic) int index;

@property(nonatomic) int weight;

@property(nonatomic) float value;

@end


NS_ASSUME_NONNULL_END
