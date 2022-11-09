# BikPlayer
 About
 -------
BikPlayer is an open source video player based on bilibili's [ijkplayer](https://github.com/bilibili/ijkplayer), which support Android and iOS, with MediaCodec/VideoToolbox support for hardware acceleration.

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
-  [Android Apk](https://raw.githubusercontent.com/valoroso-dev/BikPlayer/master/demo/ijkplayer-example.apk)

![android demo](doc/android_demo.png)

### Next Plan
- update FFmpeg to latest version
- improve documentation for this player
- regular releases on a quarterly basis, and provide unscheduled releases if there are major changes or updates


### Support
- Welcome to submit issue for discussion and we will respond ASAP

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

### Why this project

We use ijkplayer in product. At the beginning, we just used without in-depth understanding and also encountered some playback problems. With accumulated experience, we update some new features too. The official issue response was slow, so a new BikPlayer was opened. Again, welcome to submit issue for discussion :smiley:

### Some technical articles 
 - coming soon...