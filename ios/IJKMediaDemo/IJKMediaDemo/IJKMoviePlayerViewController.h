/*
 * Copyright (C) 2013-2015 Bilibili
 * Copyright (C) 2013-2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <UIKit/UIKit.h>
#import <IJKMediaFramework/IJKMediaFramework.h>
@class IJKMediaControl;

@interface IJKVideoViewController : UIViewController

@property(atomic,strong) NSURL *url;
@property(strong,nonatomic) NSString* manifest;
@property(atomic,strong) NSString *fpsCertificateUrl;
@property(atomic,strong) NSString *fpsLicensingServiceUrl;
@property(atomic,strong) NSString *fpsLicensingToken;
@property(atomic, retain) id<IJKMediaPlayback> player;

- (id)initWithURL:(NSURL *)url;
- (id)initWithManifest:(NSString*)manifest_string;

- (id)initWithFPS:(NSURL *)url
fpsCertificateUrl:(NSString *)certificateUrl
fpsLicensingServiceUrl:(NSString *)licensingServiceUrl
fpsLicensingToken:(NSString *)licensingToken;

+ (void)presentFromViewController:(UIViewController *)viewController withTitle:(NSString *)title URL:(NSURL *)url completion:(void(^)())completion;
+ (void)presentFromViewController:(UIViewController *)viewController withTitle:(NSString *)title URL:(NSURL *)url fpsCertificateUrl:(NSString *)certificateUrl fpsLicensingServiceUrl:(NSString *)licensingServiceUrl fpsLicensingToken:(NSString *)licensingToken completion:(void(^)())completion;

- (IBAction)onClickMediaControl:(id)sender;
- (IBAction)onClickOverlay:(id)sender;
- (IBAction)onClickDone:(id)sender;
- (IBAction)onClickPlay:(id)sender;
- (IBAction)onClickPause:(id)sender;

- (IBAction)didSliderTouchDown;
- (IBAction)didSliderTouchCancel;
- (IBAction)didSliderTouchUpOutside;
- (IBAction)didSliderTouchUpInside;
- (IBAction)didSliderValueChanged;

@property(nonatomic,strong) IBOutlet IJKMediaControl *mediaControl;

@end
