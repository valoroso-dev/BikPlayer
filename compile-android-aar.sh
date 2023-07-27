sh init-android.sh
echo "[****************************************]"
echo "[**********finish init android***********]"
echo "[****************************************]"
sleep 2s

sh init-android-libxml2.sh
echo "[****************************************]"
echo "[******finish init android libxml2*******]"
echo "[****************************************]"
sleep 2s

sh init-android-openssl.sh
echo "[****************************************]"
echo "[******finish init android openssl*******]"
echo "[****************************************]"
sleep 2s

cd android/contrib/
sh compile-libxml2.sh clean
sh compile-libxml2.sh all

if [ ! -f build/libxml2-armv7a/output/lib/libxml2.a ]; then
     echo "ERROR: compile libxml2 armv7a fail"
     exit
fi
if [ ! -f build/libxml2-arm64/output/lib/libxml2.a ]; then
     echo "ERROR: compile libxml2 arm64 fail"
     exit
fi
if [ ! -f build/libxml2-armv5/output/lib/libxml2.a ]; then
     echo "ERROR: compile libxml2 armv5 fail"
     exit
fi
if [ ! -f build/libxml2-x86/output/lib/libxml2.a ]; then
     echo "ERROR: compile libxml2 x86 fail"
     exit
fi
if [ ! -f build/libxml2-x86_64/output/lib/libxml2.a ]; then
     echo "ERROR: compile libxml2 x86_64 fail"
     exit
fi

echo "[****************************************]"
echo "[*****finish compile android libxml2*****]"
echo "[****************************************]"
pwd
sleep 2s

sh compile-openssl.sh clean
sh compile-openssl.sh all

if [ ! -f build/openssl-armv7a/output/lib/libssl.a ]; then
     echo "ERROR: compile openssl armv7a fail"
     exit
fi
if [ ! -f build/openssl-arm64/output/lib/libssl.a ]; then
     echo "ERROR: compile openssl arm64 fail"
     exit
fi
if [ ! -f build/openssl-armv5/output/lib/libssl.a ]; then
     echo "ERROR: compile openssl armv5 fail"
     exit
fi
if [ ! -f build/openssl-x86/output/lib/libssl.a ]; then
     echo "ERROR: compile openssl x86 fail"
     exit
fi
if [ ! -f build/openssl-x86_64/output/lib/libssl.a ]; then
     echo "ERROR: compile openssl x86_64 fail"
     exit
fi

echo "[****************************************]"
echo "[*****finish compile android openssl*****]"
echo "[****************************************]"
pwd
sleep 2s

sh compile-ffmpeg.sh clean
sh compile-ffmpeg.sh all

if [ ! -f build/ffmpeg-armv7a/output/lib/libswscale.a ]; then
     echo "ERROR: compile ffmpeg armv7a fail"
     exit
fi
if [ ! -f build/ffmpeg-arm64/output/lib/libswscale.a ]; then
     echo "ERROR: compile ffmpeg arm64 fail"
     exit
fi
if [ ! -f build/ffmpeg-armv5/output/lib/libswscale.a ]; then
     echo "ERROR: compile ffmpeg armv5 fail"
     exit
fi
if [ ! -f build/ffmpeg-x86/output/lib/libswscale.a ]; then
     echo "ERROR: compile ffmpeg x86 fail"
     exit
fi
if [ ! -f build/ffmpeg-x86_64/output/lib/libswscale.a ]; then
     echo "ERROR: compile ffmpeg x86_64 fail"
     exit
fi

echo "[****************************************]"
echo "[*****finish compile android ffmpeg******]"
echo "[****************************************]"
pwd
sleep 2s

cd ..
sh compile-ijk.sh clean
sh compile-ijk.sh all

if [ ! -f ijkplayer/ijkplayer-armv7a/src/main/libs/armeabi-v7a/libijkplayer.so ]; then
     echo "ERROR: compile ijkplayer armv7a fail"
     exit
fi
if [ ! -f ijkplayer/ijkplayer-arm64/src/main/libs/arm64-v8a/libijkplayer.so ]; then
     echo "ERROR: compile ijkplayer arm64 fail"
     exit
fi
if [ ! -f ijkplayer/ijkplayer-armv5/src/main/libs/armeabi/libijkplayer.so ]; then
     echo "ERROR: compile ijkplayer armv5 fail"
     exit
fi
if [ ! -f ijkplayer/ijkplayer-x86/src/main/libs/x86/libijkplayer.so ]; then
     echo "ERROR: compile ijkplayer x86 fail"
     exit
fi
if [ ! -f ijkplayer/ijkplayer-x86_64/src/main/libs/x86_64/libijkplayer.so ]; then
     echo "ERROR: compile ijkplayer x86_64 fail"
     exit
fi

echo "[****************************************]"
echo "[******finish compile android ijk so*****]"
echo "[****************************************]"
pwd
sleep 2s

cd ijkplayer
gradle :ijkplayer-x86:assembleRelease
gradle :ijkplayer-x86_64:assembleRelease
gradle :ijkplayer-armv5:assembleRelease
gradle :ijkplayer-arm64:assembleRelease
gradle :ijkplayer-armv7a:assembleRelease
gradle :ijkplayer-exo:assembleRelease
gradle :ijkplayer-common:assembleRelease
gradle :ijkplayer-java:assembleRelease
echo "[****************************************]"
echo "[*****finish compile android ijk aar*****]"
echo "[****************************************]"
pwd

