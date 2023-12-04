//
//  IJKSlidingPercentile.m
//  IJKMediaPlayer
//
//  Created by Gibbs on 2023/10/25.
//  Copyright © 2023 valoroso. All rights reserved.
//

#import "IJKSlidingPercentile.h"


#define SORT_ORDER_NONE    -1
#define SORT_ORDER_BY_VALUE 0
#define SORT_ORDER_BY_INDEX 1

@implementation Sample

- (NSString *)description
{
    return [NSString stringWithFormat:@"(%d,%f)", _weight, _value];
}

@end


@implementation IJKSlidingPercentile {
    int _maxWeight;
    NSMutableArray* _samples;
    int _currentSortOrder;
    int _nextSampleIndex;
    int _totalWeight;
}

- (id)initWithMaxWeight:(int)maxWeight {
    self = [super init];
    if (self) {
        self->_maxWeight = maxWeight;
        self->_samples = [[NSMutableArray alloc] init];
        self->_currentSortOrder = SORT_ORDER_NONE;
        self->_nextSampleIndex = 0;
        self->_totalWeight = 0;
    }
    return self;
}

- (void)reset {
    [_samples removeAllObjects];
    _currentSortOrder = SORT_ORDER_NONE;
    _nextSampleIndex = 0;
    _totalWeight = 0;
}

- (void)addSample:(int)weight value:(float)value {
    NSLog(@"SlidingPercentile addSample weight=%d, speed=%.2fMbps", weight, (value / 1024 / 1024));
    [self ensureSortedByIndex];
    Sample *newSample = [[Sample alloc] init];
    newSample.index = _nextSampleIndex++;
    newSample.weight = weight;
    newSample.value = value;
    [_samples addObject:newSample];
    _totalWeight += weight;

    while (_totalWeight > _maxWeight) {
        int excessWeight = _totalWeight - _maxWeight;
        Sample *oldestSample = [_samples objectAtIndex:0];
        if (oldestSample.weight <= excessWeight) {
            _totalWeight -= oldestSample.weight;
            [_samples removeObjectAtIndex:0];
        } else {
            oldestSample.weight -= excessWeight;
            _totalWeight -= excessWeight;
        }
    }
}

- (float)getPercentile:(float)percentile {
    [self ensureSortedByValue];
    float desiredWeight = percentile * _totalWeight;
    int accumulatedWeight = 0;
//    NSLog(@"SlidingPercentile getPercentile from samples %@", _samples);
    for (int i = 0; i < _samples.count; i++) {
        Sample *currentSample = [_samples objectAtIndex:i];
        accumulatedWeight += currentSample.weight;
        if (accumulatedWeight >= desiredWeight) {
            return currentSample.value;
        }
    }
    // Clamp to maximum value or NaN if no values.
    if (_samples.count == 0) {
        return NAN;
    } else {
        Sample *lastSample = [_samples objectAtIndex:_samples.count - 1];
        return lastSample.value;
    }
}

/** Sorts the samples by index. */
- (void)ensureSortedByIndex {
    if (_currentSortOrder != SORT_ORDER_BY_INDEX) {
        [_samples sortUsingComparator:^NSComparisonResult(Sample *d1, Sample *d2) {
            if (d1.index < d2.index)
                return NSOrderedAscending;
            if (d1.index > d2.index)
                return NSOrderedDescending;
            return NSOrderedSame;
        }];
        _currentSortOrder = SORT_ORDER_BY_INDEX;
    }
}

/** Sorts the samples by value. */
- (void)ensureSortedByValue {
    if (_currentSortOrder != SORT_ORDER_BY_VALUE) {
        [_samples sortUsingComparator:^NSComparisonResult(Sample *d1, Sample *d2) {
            if (d1.value < d2.value)
                return NSOrderedAscending;
            if (d1.value > d2.value)
                return NSOrderedDescending;
            return NSOrderedSame;
        }];
        _currentSortOrder = SORT_ORDER_BY_VALUE;
    }
}


@end
