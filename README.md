# BikPlayer
BikPlayer, derived from bilibili's [ijkplayer](https://github.com/bilibili/ijkplayer) open source project, provide more outstanding features and continuous issue & technical support.
BikPlayer provide perfect performance both in Android and iOS platform, support hardware decoding using with MediaCodec (Android) /VideoToolbox(iOS), help to achieve perfect performance in varieties of hardware include mobile phones and STBs.
BikPlayer support big range of media file formats, include almost all existing audio and video formats; As alternative option, BikPlayer support replaceable back-end players, include native player or Exo, or BikPlayer to adapt different application scenarios.  
One big improvement in BikPlayer is quick-to-play supporting. In lab testing, BikPlayer can be nearly two-times faster compared with other traditional player.
You can experience quick-to-play, media file formats support by the demo APK provided later, and check how to integrate it with your own product.

### Why this project

We used ijkplayer in our products several years ago, and of course, we encountered many problems, some of them solved with support from open source project, but more solved by ourselves without external help, and we also developed some new features. Now we want to share all these changes with more users, and continue supporting it to be more powerful and wide-used. So, if you have any issues about this player, or suggestions, please contact us and discuss with us. :smiley:

### Features
- Android 
  - platform: 19~latest
  - cpu: ARMv7a, ARM64v8a, x86 (ARMv5 is not tested on real devices)
  - api: MediaPlayer-like
  - integrate other players, MediaPlayer and Exoplayer
  - video-output: NativeWindow, OpenGL ES 2.0
  - audio-output: AudioTrack, OpenSL ES
  - hw-decoder: MediaCodec
  - alternative-backend: android.media.MediaPlayer, ExoPlayer
- iOS
  - platform: 7.0~latest
  - api: MediaPlayer.framework-like
  - video-output: OpenGL ES 2.0
  - audio-output: AudioQueue, AudioUnit
  - hw-decoder: VideoToolbox (iOS 8+)
  - alternative-backend: AVFoundation.Framework.AVPlayer, MediaPlayer.Framework.MPMoviePlayerController (obsolete since iOS 8)
- Others
  - Optimized startup cost time for ts format (h.264&h.265 encoding)
  - Update backend Exoplayer to v2.17.1

### How to build
  - [build for Android](doc/build_android.md) (include FFmpeg / BikPlayer)
  - build for Android by CMake using Android Studio
  - [build for iOS](doc/build_iOS.md)


### Try Android Demo
-  [Android Apk](https://github.com/valoroso-dev/BikPlayer/blob/master/demo/BikPlayer.apk)

![android demo](doc/android_demo.png)

### Next Plan
- Update FFmpeg to latest version
- Support subtitle
- Support playback speed from 1/8 ~ 2 times of normal speed
- Support ADR
- Sooner we will have the first release, expected in February 2023

### Support
- Welcome to submit issue for discussion and we will respond ASAP.  It will be very helpful to solve the problem by providing the scene where the problem occurs, the phenomenon, the operation method and the media file or url used in the test.

### License

This project is licensed under the LGPLv2.1 or later

Features are based on or derives from projects below:
- LGPL
  - [FFmpeg](http://git.videolan.org/?p=ffmpeg.git)
  - [libVLC](http://git.videolan.org/?p=vlc.git)
  - [kxmovie](https://github.com/kolyvan/kxmovie)
  - [soundtouch](http://www.surina.net/soundtouch/sourcecode.html)
- zlib license
  - [SDL](http://www.libsdl.org)
- BSD-style license
  - [libyuv](https://code.google.com/p/libyuv/)
- ISC license
  - [libyuv/source/x86inc.asm](https://code.google.com/p/libyuv/source/browse/trunk/source/x86inc.asm)

android/ijkplayer-exo is based on or derives from projects below:
- Apache License 2.0
  - [ExoPlayer](https://github.com/google/ExoPlayer)

android/example is based on or derives from projects below:
- GPL
  - [android-ndk-profiler](https://github.com/richq/android-ndk-profiler) (not included by default)

ios/IJKMediaDemo is based on or derives from projects below:
- Unknown license
  - [iOS7-BarcodeScanner](https://github.com/jpwiddy/iOS7-BarcodeScanner)

build scripts are based on or derives from projects below:
- [gas-preprocessor](http://git.libav.org/?p=gas-preprocessor.git)
- [VideoLAN](http://git.videolan.org)
- [yixia/FFmpeg-Android](https://github.com/yixia/FFmpeg-Android)
- [kewlbear/FFmpeg-iOS-build-script](https://github.com/kewlbear/FFmpeg-iOS-build-script) 

### Some technical articles 
 - coming soon...
