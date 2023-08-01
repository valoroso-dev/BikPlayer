/*
 * Copyright (C) 2015 Gdier
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

#import "IJKDemoFPSViewController.h"
#import "IJKMoviePlayerViewController.h"

@interface IJKDemoFPSViewController ()

@end

@implementation IJKDemoFPSViewController

- (instancetype)init {
    self = [super init];
    if (self) {
        self.title = @"Play FPS";

        [self.navigationItem setRightBarButtonItem:[[UIBarButtonItem alloc] initWithTitle:@"Play" style:UIBarButtonItemStyleDone target:self action:@selector(onClickPlayButton)]];
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
}

- (void)onClickPlayButton {
    NSURL *url = [NSURL URLWithString:@""];
    NSString *certificateUrl = @"";
    NSString *licensingServiceUrl = @"";
    NSString *licensingToken = @"";

    [IJKVideoViewController presentFromViewController:self withTitle:[NSString stringWithFormat:@"URL: %@", url] URL:url fpsCertificateUrl:certificateUrl fpsLicensingServiceUrl:licensingServiceUrl fpsLicensingToken:licensingToken completion:^{
//            [self.navigationController popViewControllerAnimated:NO];
    }];
}

@end
