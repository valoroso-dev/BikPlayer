/*
 * IJKContentKeyManager.m
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
 File: IJKAVMoviePlayerController.m
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

#import <Foundation/Foundation.h>
#import <AVFoundation/AVContentKeySession.h>
#import "IJKContentKeyManager.h"

@implementation IJKContentKeyManager

- (id)init
{
    self = [super init];
    if (self == nil) {
        NSLog(@"IJKMEDIA ContentKeyManager fail to create");
        return self;
    }
    NSLog(@"IJKMEDIA ContentKeyManager is created");
    _recipient = nil;
    _keySession = [AVContentKeySession contentKeySessionWithKeySystem: AVContentKeySystemFairPlayStreaming storageDirectoryAtURL:[NSURL URLWithString:NSTemporaryDirectory()]];
    [_keySession setDelegate:self queue:dispatch_queue_create("IJKMEDIA.ContentKeyDelegateQueue", DISPATCH_QUEUE_CONCURRENT)];
    _mayRequireContentKeysForMediaDataProcessing = TRUE;
    [_keySession addContentKeyRecipient:self];
    self.sessionState = FPS_SESSION_STATE_IDLE;
    self.fromEngine = FALSE;
    return self;
}

@synthesize mayRequireContentKeysForMediaDataProcessing = _mayRequireContentKeysForMediaDataProcessing;

//- (void)contentKeySession:(AVContentKeySession *)contentKeySession didProvideContentKey:(AVContentKey *)contentKey
//API_AVAILABLE(ios(14.5)){
//    NSLog(@"IJKMEDIA ContentKeyManager contentKey is created");
//}

- (id)setFairPlayCertificate:(NSString *)fpsCertificateUrl licensingUrl:(NSString *)fpsLicensingServiceUrl params:(NSDictionary *)fpsParams
{
    NSLog(@"IJKMEDIA ContentKeyManager setFairPlayCertificate");
    if (_fpsCertificateUrl == nil || ![_fpsCertificateUrl isEqualToString:fpsCertificateUrl]) {
        _fpsCertificateUrl = [NSString stringWithString:fpsCertificateUrl];
        // need to reget fps certificate
        _fpsCertificate = nil;
    }
    _fpsLicensingServiceUrl = [NSString stringWithString:fpsLicensingServiceUrl];
    if (fpsParams) {
        _fpsParams = [NSMutableDictionary dictionaryWithDictionary:fpsParams];
    } else {
        _fpsParams = [[NSMutableDictionary alloc] init];
    }
    if ([[_fpsParams allKeys] containsObject:@"fromEngine"]) {
        _fromEngine = TRUE;
        [_fpsParams removeObjectForKey:@"fromEngine"];
        NSLog(@"it is a fps from engine");
    }
    [self updateSessionState:FPS_SESSION_STATE_INIT];
    return self;
}

- (void)setContentKeyRecipient:(id<AVContentKeyRecipient>)recipient
{
    if (_recipient != nil) {
        [_keySession removeContentKeyRecipient:_recipient];
    }
    _recipient = recipient;
    [_keySession addContentKeyRecipient:_recipient];
}

- (void)requestKey:(id)identifier initializationData:(nullable NSData *)initializationData options:(nullable NSDictionary<NSString *,id> *)options
{
    NSLog(@"IJKMEDIA ContentKeyManager requestKey");
    self.fpsIdentifier = [NSString stringWithString:identifier];
    self.needRequestPersistableKey = FALSE;
    [_keySession processContentKeyRequestWithIdentifier:identifier initializationData:initializationData options:options];
}

- (void)contentKeySession:(AVContentKeySession *)session didProvideContentKeyRequest:(AVContentKeyRequest *)keyRequest
{
    NSLog(@"IJKMEDIA ContentKeyManager delegate contentKeySession for didProvideContentKeyRequest");
    [self handleKeyRequest:keyRequest];
}

- (void)contentKeySession:(AVContentKeySession *)session didProvideRenewingContentKeyRequest:(AVContentKeyRequest *)keyRequest
{
    NSLog(@"IJKMEDIA ContentKeyManager delegate contentKeySession for didProvideRenewingContentKeyRequest");
    [self handleKeyRequest:keyRequest];
}

- (void)handleKeyRequest:(AVContentKeyRequest *)keyRequest
{
    NSLog(@"IJKMEDIA ContentKeyManager handleKeyRequest");
    [self updateSessionState:FPS_SESSION_STATE_START];
    @synchronized (self) {
        if (_fpsCertificate == nil) {
            [self updateSessionState:FPS_SESSION_STATE_REQUESTING_CERTIFIER];
            if ([self requestApplicationCertificate] == FALSE) {
                NSLog(@"IJKMEDIA ContentKeyManager cant get a valid certificate");
                [self updateSessionState:FPS_SESSION_STATE_ERROR];
                return;
            }
        }
    }
    @try {
        CFDataRef cfData = CFBridgingRetain(_fpsCertificate);
        SecCertificateRef certificate = SecCertificateCreateWithData(nil, cfData);
        if (certificate != nil) {
            CFStringRef cfRef = SecCertificateCopySubjectSummary(certificate);
            NSLog(@"IJKMEDIA ContentKeyManager FPS Certificate received, summary: %@", cfRef);
        }
    } @catch (NSException *exception) {
        NSLog(@"IJKMEDIA ContentKeyManager SecCertificateCopySubjectSummary error: %@", exception);
        [self updateSessionState:FPS_SESSION_STATE_ERROR];
        return;
    }
    [self updateSessionState:FPS_SESSION_STATE_INSTALLED_CERTIFIER];
    NSString *keyIdentifier = [[NSString alloc] initWithString:[keyRequest identifier]];
    NSLog(@"IJKMEDIA ContentKeyManager keyIdentifier is:\n%@", keyIdentifier);
    
    if (self.needRequestPersistableKey) {
        [keyRequest respondByRequestingPersistableContentKeyRequest];
        return;
    }

    NSString *contentIdentifier = [keyIdentifier stringByReplacingOccurrencesOfString:@"skd://" withString:@""];
    NSData *contentIdentifierData;
    if (_fromEngine) {
        if (contentIdentifier.length % 4 != 0) {
            contentIdentifier = [contentIdentifier stringByAppendingString:[@"===" substringToIndex:(4 - (contentIdentifier.length % 4))]];
        }
        NSData *contentIdentifierBase64Data = [[NSData alloc] initWithBase64EncodedString:contentIdentifier options:NSDataBase64DecodingIgnoreUnknownCharacters];
        NSString *contentIdentifierHexString = [[NSString alloc] initWithData:contentIdentifierBase64Data encoding:NSUTF8StringEncoding];
        contentIdentifierData = [self convertHexStrToData:contentIdentifierHexString];
    } else {
        contentIdentifierData = [contentIdentifier dataUsingEncoding:NSUTF8StringEncoding];
    }

    [self updateSessionState:FPS_SESSION_STATE_REQUESTING_CKC];
    [keyRequest makeStreamingContentKeyRequestDataForApp:_fpsCertificate contentIdentifier:contentIdentifierData options:nil completionHandler:^(NSData *spc, NSError *error){
        [self onSPCResponse:keyRequest spc:spc error:error];
    }];
}

- (NSData *)convertHexStrToData:(NSString *)str {
    if (!str || [str length] == 0) {
        return nil;
    }

    NSMutableData *hexData = [[NSMutableData alloc] initWithCapacity:20];
    NSRange range;
    if ([str length] % 2 == 0) {
        range = NSMakeRange(0, 2);
    } else {
        range = NSMakeRange(0, 1);
    }
    for (NSInteger i = range.location; i < [str length]; i += 2) {
        unsigned int anInt;
        NSString *hexCharStr = [str substringWithRange:range];
        NSScanner *scanner = [[NSScanner alloc] initWithString:hexCharStr];

        [scanner scanHexInt:&anInt];
        NSData *entity = [[NSData alloc] initWithBytes:&anInt length:1];
        [hexData appendData:entity];

        range.location += range.length;
        range.length = 2;
    }
    return hexData;
}

- (void)onSPCResponse:(AVContentKeyRequest *)keyRequest spc:(NSData *)spc error:(NSError *)error
{
    if (error != nil) {
        NSLog(@"IJKMEDIA ContentKeyManager onSPCResponse error %@", error);
        [keyRequest processContentKeyResponseError:error];
        if (self.fpsIdentifier != nil) {
            [self requestKey:self.fpsIdentifier initializationData:nil options:nil];
        }
        return;
    }
    NSLog(@"IJKMEDIA ContentKeyManager onSPCResponse spc %@", spc);
    NSData *ckc = [self requestContentKeyFromKeySecurityModule:spc];
    NSLog(@"IJKMEDIA ContentKeyManager ckc is: %@", ckc);
    if (ckc != nil) {
        @try {
            AVContentKeyResponse *keyResponse = [AVContentKeyResponse contentKeyResponseWithFairPlayStreamingKeyResponseData:ckc];
            [keyRequest processContentKeyResponse:keyResponse];
            [self updateSessionState:FPS_SESSION_STATE_READY];
            NSLog(@"IJKMEDIA ContentKeyManager processContentKeyResponse success");
            return;
        } @catch (NSException *exception) {
            NSLog(@"IJKMEDIA ContentKeyManager processContentKeyResponse error: %@", exception);
        }
    }
    [self updateSessionState:FPS_SESSION_STATE_ERROR];
}

- (void)requestPersistableKey:(id)identifier initializationData:(NSData *)initializationData options:(NSDictionary<NSString *,id> *)options
{
    NSLog(@"IJKMEDIA ContentKeyManager requestPersistableKey");
    self.fpsIdentifier = [NSString stringWithString:identifier];
    self.needRequestPersistableKey = TRUE;
    [_keySession processContentKeyRequestWithIdentifier:identifier initializationData:initializationData options:options];
}

- (void)contentKeySession:(AVContentKeySession *)session didProvidePersistableContentKeyRequest:(AVPersistableContentKeyRequest *)keyRequest
{
    NSLog(@"IJKMEDIA ContentKeyManager delegate contentKeySession for didProvidePersistableContentKeyRequest");
    [self handlePersistableKeyRequest:keyRequest];
}

- (void)handlePersistableKeyRequest:(AVPersistableContentKeyRequest *)keyRequest
{
    NSLog(@"IJKMEDIA ContentKeyManager handlePersistableKeyRequest");
    if (_fpsCertificate == nil) {
        if ([self requestApplicationCertificate] == FALSE) {
            NSLog(@"IJKMEDIA ContentKeyManager cant get a valid certificate");
            return;
        }
    }
    @try {
        CFDataRef cfData = CFBridgingRetain(_fpsCertificate);
        SecCertificateRef certificate = SecCertificateCreateWithData(nil, cfData);
        if (certificate != nil) {
            CFStringRef cfRef = SecCertificateCopySubjectSummary(certificate);
            NSLog(@"IJKMEDIA ContentKeyManager FPS Certificate received, summary: %@", cfRef);
        }
    } @catch (NSException *exception) {
        NSLog(@"IJKMEDIA ContentKeyManager SecCertificateCopySubjectSummary error: %@", exception);
        return;
    }

    NSString *keyIdentifier = [[NSString alloc] initWithString:[keyRequest identifier]];
    NSString *contentIdentifier = [keyIdentifier stringByReplacingOccurrencesOfString:@"skd://" withString:@""];

    NSData *contentIdentifierData =[contentIdentifier dataUsingEncoding:NSUTF8StringEncoding];

    [keyRequest makeStreamingContentKeyRequestDataForApp:_fpsCertificate contentIdentifier:contentIdentifierData options:nil completionHandler:^(NSData *spc, NSError *error){
        [self onPersistableSPCResponse:keyRequest spc:spc error:error];
    }];
}

- (void)onPersistableSPCResponse:(AVPersistableContentKeyRequest *)keyRequest spc:(NSData *)spc error:(NSError *)error
{
    if (error != nil) {
        NSLog(@"IJKMEDIA ContentKeyManager onPersistableSPCResponse error %@", error);
        [keyRequest processContentKeyResponseError:error];
        [self requestPersistableKey:self.fpsIdentifier initializationData:nil options:nil];
        return;
    }
    NSLog(@"IJKMEDIA ContentKeyManager onPersistableSPCResponse spc %@", spc);
    NSData *ckc = [self requestContentKeyFromKeySecurityModule:spc];
    NSLog(@"IJKMEDIA ContentKeyManager ckc is: %@", ckc);
    if (ckc != nil) {
        @try {
            AVContentKeyResponse *keyResponse = [AVContentKeyResponse contentKeyResponseWithFairPlayStreamingKeyResponseData:[keyRequest persistableContentKeyFromKeyVendorResponse:ckc options:nil error:nil]];
            [keyRequest processContentKeyResponse:keyResponse];
            NSLog(@"IJKMEDIA ContentKeyManager processContentKeyResponse end");
        } @catch (NSException *exception) {
            NSLog(@"IJKMEDIA ContentKeyManager onPersistableSPCResponse error: %@", exception);
        }
    }
}

- (BOOL)requestApplicationCertificate
{
    NSLog(@"IJKMEDIA ContentKeyManager requestApplicationCertificate %@", _fpsCertificateUrl);
    NSURLRequest* request = [NSURLRequest requestWithURL:[NSURL URLWithString:_fpsCertificateUrl] cachePolicy:NSURLRequestReloadIgnoringLocalCacheData  timeoutInterval:30];
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        NSLog(@"IJKMEDIA ContentKeyManager requestApplicationCertificate response:\n %@", response);
        if (error != nil) {
            NSLog(@"IJKMEDIA ContentKeyManager requestApplicationCertificate error:\n %@", error);
        } else if (data != nil) {
            NSLog(@"IJKMEDIA ContentKeyManager requestApplicationCertificate data:\n %@", data);
            self->_fpsCertificate = data;
        }
        dispatch_semaphore_signal(semaphore);
    }];
    [task resume];
    dispatch_semaphore_wait(semaphore,DISPATCH_TIME_FOREVER);

    return _fpsCertificate != nil;
}

- (NSData *)requestContentKeyFromKeySecurityModule:(NSData *)spc
{
    NSLog(@"IJKMEDIA ContentKeyManager requestContentKeyFromKeySecurityModule %@", _fpsLicensingServiceUrl);
    NSMutableURLRequest* request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:_fpsLicensingServiceUrl] cachePolicy:NSURLRequestReloadIgnoringLocalCacheData  timeoutInterval:30];
    request.HTTPMethod = @"POST";
    request.HTTPBody = spc;
    for (NSString *key in _fpsParams) {
        NSLog(@"IJKMEDIA ContentKeyManager fpsParams %@ : %@", key, _fpsParams[key]);
        [request setValue:_fpsParams[key] forHTTPHeaderField:key];
    }

    __block NSData *ckc;
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        NSLog(@"IJKMEDIA ContentKeyManager requestContentKeyFromKeySecurityModule response:\n %@", response);
        if (error != nil) {
            NSLog(@"IJKMEDIA ContentKeyManager requestContentKeyFromKeySecurityModule error:\n %@", error);
        } else if (data != nil) {
            NSLog(@"IJKMEDIA ContentKeyManager requestContentKeyFromKeySecurityModule data:\n %@", data);
            ckc = data;
        }
        dispatch_semaphore_signal(semaphore);
    }];
    [task resume];
    dispatch_semaphore_wait(semaphore,DISPATCH_TIME_FOREVER);

    return ckc;
}

- (void)updateSessionState:(int)state
{
    NSLog(@"IJKMEDIA ContentKeyManager updateSessionState from %s to %s", getSessionStateName(self.sessionState), getSessionStateName(state));
    if (self.sessionState != state) {
        self.sessionState = state;
    }
}

@end

