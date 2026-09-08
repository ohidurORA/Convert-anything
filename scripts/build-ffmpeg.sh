#!/bin/bash
# Configures + builds static FFmpeg (with external codecs) per ABI.
# Usage: build-ffmpeg.sh [abi]   (default: arm64-v8a x86_64)
set -u
ROOT=/home/c/ffmpeg-android
SRC=$ROOT/src/ffmpeg-7.1.2
NDK=/home/c/Android/Sdk/ndk/28.2.13676358
TC=$NDK/toolchains/llvm/prebuilt/linux-x86_64
SYS=$TC/sysroot
API=24
JOBS=2

abi_env() {
  case "$1" in
    arm64-v8a) TRIP=aarch64-linux-android; ARCH=aarch64; CPU=armv8-a; ASMOPT="" ;;
    x86_64)    TRIP=x86_64-linux-android;  ARCH=x86_64;  CPU=x86-64;  ASMOPT="--disable-x86asm" ;;
    *) echo "unknown abi $1"; exit 1 ;;
  esac
  W=$ROOT/wrappers/$1
  export PATH="$W:$PATH"
  export CC="$TC/bin/${TRIP}${API}-clang" CXX="$TC/bin/${TRIP}${API}-clang++"
}

write_pcs() {
  # lame + x265 ship no usable .pc for cross builds; provide minimal ones.
  local ABI=$1
  mkdir -p "$ROOT/out/pkgconfig/$ABI"
  cat > "$ROOT/out/pkgconfig/$ABI/mp3lame.pc" <<EOF
prefix=$ROOT/out/lame/$ABI
libdir=\${prefix}/lib
includedir=\${prefix}/include
Name: mp3lame
Description: LAME MP3 encoder
Version: 3.100
Libs: -L\${libdir} -lmp3lame -lm
Cflags: -I\${includedir}
EOF
  cat > "$ROOT/out/pkgconfig/$ABI/x265.pc" <<EOF
prefix=$ROOT/out/x265/$ABI
libdir=\${prefix}/lib
includedir=\${prefix}/include
Name: x265
Description: H.265/HEVC encoder
Version: 4.1
Libs: -L\${libdir} -lx265 -lstdc++ -lm -ldl
Cflags: -I\${includedir}
EOF
}

build_one() {
  local ABI=$1; abi_env "$ABI"
  local W=$ROOT/wrappers/$ABI
  local B=$ROOT/build/ffmpeg-$ABI
  write_pcs "$ABI"
  local INCS="" LIBS=""
  for L in x264 x265 vpx lame webp aom; do
    INCS="$INCS -I$ROOT/out/$L/$ABI/include"
    LIBS="$LIBS -L$ROOT/out/$L/$ABI/lib"
  done
  export PKG_CONFIG_PATH="$ROOT/out/pkgconfig/$ABI:$ROOT/out/x264/$ABI/lib/pkgconfig:$ROOT/out/vpx/$ABI/lib/pkgconfig:$ROOT/out/webp/$ABI/lib/pkgconfig:$ROOT/out/aom/$ABI/lib/pkgconfig"
  rm -rf "$B"; mkdir -p "$B"; cd "$B"
  "$SRC/configure" \
    --target-os=android --arch=$ARCH --cpu=$CPU --enable-cross-compile \
    --cross-prefix="$W/${TRIP}-" --sysroot="$SYS" \
    --cc="$CC" --cxx="$CXX" \
    --ar="$TC/bin/llvm-ar" --nm="$TC/bin/llvm-nm" --ranlib="$TC/bin/llvm-ranlib" --strip="$TC/bin/llvm-strip" \
    --extra-cflags="-O2 -fPIC -DPIC $INCS" \
    --extra-ldflags="$LIBS -L$ROOT/stubs" \
    --pkg-config=/usr/bin/pkg-config \
    --pkg-config-flags="--static" \
    --disable-shared --enable-static --enable-pic --enable-gpl \
    --enable-libx264 --enable-libx265 --enable-libvpx --enable-libmp3lame \
    --enable-libwebp --enable-libaom --enable-jni \
    --disable-doc --disable-htmlpages --disable-manpages --disable-podpages --disable-txtpages \
    --disable-ffplay --disable-ffprobe --disable-avdevice --disable-postproc \
    --disable-debug $ASMOPT \
    >"$ROOT/logs/ffmpeg-$ABI.log" 2>&1 || { echo "ffmpeg $ABI CONFIG FAIL"; tail -20 "$ROOT/logs/ffmpeg-$ABI.log"; return 1; }
  grep -E "libx264|libx265|libvpx|libmp3lame|libwebp|libaom" "$B/config.h" | grep -c "define 1"
  make -j$JOBS >>"$ROOT/logs/ffmpeg-$ABI.log" 2>&1 || { echo "ffmpeg $ABI BUILD FAIL"; return 1; }
  echo "ffmpeg $ABI OK"
}

for ABI in ${1:-arm64-v8a x86_64}; do build_one "$ABI"; done
