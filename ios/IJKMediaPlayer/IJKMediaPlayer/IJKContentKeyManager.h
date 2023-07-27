/*
 * IJKAVMoviePlayerController.h
 *
 * Copyright (c) 2014 Bilibili
 * Copyright (c) 2022 Valoroso
 *
 * This file is part of ijkPlayer.
 *
 * ijkPlayer is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * ijkPlayer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ijkPlayer; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

/*
 File: IJKAVMoviePlayerController.h
 Abstract: managing a AVContentKeySession related to fps.
 Version: 1.0

 Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple
 Inc. ("Apple") in consideration of your agreement to the following
 terms, and your use, installation, modification or redistribution of
 this Apple software constitutes acceptance of these terms.  If you do
 not agree with these terms, please do not use, install, modify or
 redistribute this Apple software.

 In consideration of your agreement to abide by the following terms, and
 subject to these terms, Apple grants you a personal, non-exclusive
 license, under Apple's copyrights in this original Apple software (the
 "Apple Software"), to use, reproduce, modify and redistribute the Apple
 Software, with or without modifications, in source and/or binary forms;
 provided that if you redistribute the Apple Software in its entirety and
 without modifications, you must retain this notice and the following
 text and disclaimers in all such redistributions of the Apple Software.
 Neither the name, trademarks, service marks or logos of Apple Inc. may
 be used to endorse or promote products derived from the Apple Software
 without specific prior written permission from Apple.  Except as
 expressly stated in this notice, no other rights or licenses, express or
 implied, are granted by Apple herein, including but not limited to any
 patent rights that may be infringed by your derivative works or by other
 works in which the Apple Software may be incorporated.

 The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
 MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
 OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.

 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
 MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
 AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
 STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.

 Copyright (C) 2017 Apple Inc. All Rights Reserved.

 */

#import <AVFoundation/AVContentKeySession.h>

NS_ASSUME_NONNULL_BEGIN

#define FPS_SESSION_STATE_IDLE                       0
#define FPS_SESSION_STATE_INIT                       1
#define FPS_SESSION_STATE_START                      2
#define FPS_SESSION_STATE_REQUESTING_CERTIFIER       3
#define FPS_SESSION_STATE_INSTALLED_CERTIFIER        4
#define FPS_SESSION_STATE_REQUESTING_CKC             5
#define FPS_SESSION_STATE_READY                      6
#define FPS_SESSION_STATE_ERROR                      10

static char* getSessionStateName(int state)
{
    switch (state) {
        case FPS_SESSION_STATE_IDLE:
            return "IDLE";
        case FPS_SESSION_STATE_INIT:
            return "INIT";
        case FPS_SESSION_STATE_START:
            return "START";
        case FPS_SESSION_STATE_REQUESTING_CERTIFIER:
            return "REQUESTING_CERTIFIER";
        case FPS_SESSION_STATE_INSTALLED_CERTIFIER:
            return "INSTALLED_CERTIFIER";
        case FPS_SESSION_STATE_REQUESTING_CKC:
            return "REQUESTING_CKC";
        case FPS_SESSION_STATE_READY:
            return "READY";
        case FPS_SESSION_STATE_ERROR:
            return "ERROR";

        default:
            break;
    }
    return "UNKNOWN";
}

@interface IJKContentKeyManager : NSObject<AVContentKeySessionDelegate, AVContentKeyRecipient>

- (id)setFairPlayCertificate:(NSString*)fpsCertificateUrl
                licensingUrl:(NSString*)fpsLicensingServiceUrl
                      params:(NSDictionary*)fpsParams;

- (void)setContentKeyRecipient:(id<AVContentKeyRecipient>)recipient;

- (void)requestKey:(id)identifier
initializationData:(nullable NSData *)initializationData
           options:(nullable NSDictionary<NSString *,id> *)options;

- (void)handleKeyRequest:(AVContentKeyRequest *)keyRequest;

- (void)onSPCResponse:(AVContentKeyRequest *)keyRequest
                  spc:(NSData *)spc
                error:(NSError *)error;

- (void)requestPersistableKey:(id)identifier
           initializationData:(nullable NSData *)initializationData
                      options:(nullable NSDictionary<NSString *,id> *)options;

- (void)handlePersistableKeyRequest:(AVPersistableContentKeyRequest *)keyRequest;

- (void)onPersistableSPCResponse:(AVContentKeyRequest *)keyRequest
                             spc:(NSData *)spc
                           error:(NSError *)error;

- (BOOL)requestApplicationCertificate;

- (NSData *)requestContentKeyFromKeySecurityModule:(NSData *)spc;

- (void)updateSessionState:(int)state;

@property(atomic) int sessionState;

@property(nonatomic, strong) NSString *fpsCertificateUrl;
@property(nonatomic, strong) NSString *fpsLicensingServiceUrl;
@property(nonatomic, strong) NSMutableDictionary *fpsParams;
@property(nonatomic)         BOOL fromEngine;
@property(nonatomic, strong) NSString *fpsIdentifier;

@property(nonatomic, strong) AVContentKeySession *keySession;
@property(nonatomic, strong) NSData *fpsCertificate;

@property(nonatomic, weak) id<AVContentKeyRecipient> recipient;

@property(atomic) BOOL needRequestPersistableKey;

@end

NS_ASSUME_NONNULL_END
