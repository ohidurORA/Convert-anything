#!/bin/bash
# Builds x264/x265/vpx/lame/webp/aom for Android (arm64-v8a + x86_64).
# Usage: build-libs.sh [abi]  (default: all). Logs to build/logs/.
set -u
ROOT=/home/c/ffmpeg-android
SRC=$ROOT/src
OUT=$ROOT/out
NDK=/home/c/Android/Sdk/ndk/28.2.13676358
TC=$NDK/toolchains/llvm/prebuilt/linux-x86_64
SYS=$TC/sysroot
CMAKE=/home/c/Android/Sdk/cmake/3.22.1/bin/cmake
API=24
JOBS=2
mkdir -p "$OUT" "$ROOT/build" "$ROOT/logs"
[ -d "$SRC/ffmpeg-7.1.2" ] || tar -xf "$SRC/ffmpeg.tar.xz" -C "$SRC"
[ -d "$SRC/libaom-3.11.0" ] || tar -xzf "$SRC/aom.tar.gz" -C "$SRC"
[ -d "$SRC/x265_4.1" ] || tar -xzf "$SRC/x265.tar.gz" -C "$SRC"
[ -d "$SRC/lame-3.100" ] || tar -xzf "$SRC/lame.tar.gz" -C "$SRC"
[ -d "$SRC/libwebp-1.5.0" ] || tar -xzf "$SRC/webp.tar.gz" -C "$SRC"

setup_abi() {
  case "$1" in
    arm64-v8a) TRIP=aarch64-linux-android; ARCH64=64; CPUFLAGS="-march=armv8-a"; ASMFLAGS="" ;;
    x86_64)    TRIP=x86_64-linux-android;  ARCH64=64; CPUFLAGS="-march=x86-64"; ASMFLAGS="--disable-asm" ;;
    *) echo "unknown abi $1"; exit 1 ;;
  esac
  export CC="$TC/bin/${TRIP}${API}-clang"
  export CXX="$TC/bin/${TRIP}${API}-clang++"
  export LD="$CC" AS="$CC"
  export AR="$TC/bin/llvm-ar" RANLIB="$TC/bin/llvm-ranlib"
  export NM="$TC/bin/llvm-nm" STRIP="$TC/bin/llvm-strip"
  export CFLAGS="-O2 -fPIC -DANDROID -D__ANDROID_API__=$API $CPUFLAGS"
  export CXXFLAGS="$CFLAGS"
  export LDFLAGS="--sysroot=$SYS"
  # NDK ships llvm-* tools without GNU-prefixed aliases; some configure
  # scripts demand ${TRIP}-ar/-strings/etc. Provide them via symlinks.
  local W=$ROOT/wrappers/$1
  mkdir -p "$W"
  for t in ar ranlib nm strip strings addr2line objcopy objdump readelf; do
    [ -e "$W/${TRIP}-$t" ] || ln -sf "$TC/bin/llvm-$t" "$W/${TRIP}-$t"
  done
  # NDK triple-clang binaries are $0-relative scripts: symlinking them breaks
  # dirname resolution, so (re)write exec-script wrappers unconditionally.
  rm -f "$W/${TRIP}-gcc" "$W/${TRIP}-g++"
  printf '#!/bin/sh\nexec %s/%s%s-clang "$@"\n' \
    "$TC/bin" "$TRIP" "$API" > "$W/${TRIP}-gcc"
  printf '#!/bin/sh\nexec %s/%s%s-clang++ "$@"\n' \
    "$TC/bin" "$TRIP" "$API" > "$W/${TRIP}-g++"
  chmod +x "$W/${TRIP}-gcc" "$W/${TRIP}-g++"
  export PATH="$W:$PATH"
  # x264/vpx resolve ${cross}tool names literally; route them via wrappers.
  export CROSS_PREFIX="$W/${TRIP}-"
}

build_x264() {
  local ABI=$1; setup_abi "$ABI"; local P=$OUT/x264/$ABI
  [ -f "$P/lib/libx264.a" ] && { echo "x264 $ABI cached"; return; }
  cd "$SRC/x264"
  make distclean >/dev/null 2>&1 || true  # in-source tree shared across ABIs
  ./configure --prefix="$P" --cross-prefix="$CROSS_PREFIX" --sysroot="$SYS" \
    --host="${TRIP%%-*}-linux" --enable-static --enable-pic --disable-cli --disable-opencl \
    --extra-cflags="$CFLAGS" --extra-ldflags="$LDFLAGS" $ASMFLAGS \
    >"$ROOT/logs/x264-$ABI.log" 2>&1 || { echo "x264 $ABI CONFIG FAIL"; return 1; }
  make -j$JOBS >>"$ROOT/logs/x264-$ABI.log" 2>&1 && make install >>"$ROOT/logs/x264-$ABI.log" 2>&1
}

build_x265() {
  local ABI=$1; setup_abi "$ABI"; local P=$OUT/x265/$ABI
  unset CFLAGS CXXFLAGS LDFLAGS  # NDK toolchain file sets its own flags
  [ -f "$P/lib/libx265.a" ] && { echo "x265 $ABI cached"; return; }
  local B=$ROOT/build/x265-$ABI; rm -rf "$B"; mkdir -p "$B"; cd "$B"
  local ASMOPT=""
  [ "$ABI" = "x86_64" ] && ASMOPT="-DENABLE_ASSEMBLY=OFF"
  # NOTE: x265 ignores CMAKE_INSTALL_PREFIX; artifacts copied manually below.
  "$CMAKE" "$SRC/x265_4.1/source" -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-$API -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_SHARED=OFF -DENABLE_CLI=OFF -DENABLE_TESTS=OFF $ASMOPT \
    -DCMAKE_ASM_COMPILER="$TC/bin/${TRIP}${API}-clang" \
    -DCMAKE_C_COMPILER="$TC/bin/${TRIP}${API}-clang" \
    -DCMAKE_CXX_COMPILER="$TC/bin/${TRIP}${API}-clang++" \
    >"$ROOT/logs/x265-$ABI.log" 2>&1 || { echo "x265 $ABI CONFIG FAIL"; return 1; }
  make -j$JOBS x265-static >>"$ROOT/logs/x265-$ABI.log" 2>&1 || return 1
  mkdir -p "$P/lib" "$P/include"
  cp "$B/libx265.a" "$P/lib/"
  cp "$B/x265_config.h" "$SRC/x265_4.1/source/x265.h" "$P/include/"
  find "$P" -name "libx265.a" | head -1
}

build_vpx() {
  local ABI=$1; setup_abi "$ABI"; local P=$OUT/vpx/$ABI
  [ -f "$P/lib/libvpx.a" ] && { echo "vpx $ABI cached"; return; }
  cd "$SRC/vpx"
  make distclean >/dev/null 2>&1 || true  # in-source tree shared across ABIs
  local TARGET OPTS=""
  if [ "$ABI" = "arm64-v8a" ]; then TARGET=arm64-android-gcc
  else TARGET=generic-gnu; OPTS="--disable-multithread"; fi
  export AS="$CC"
  CROSS="$CROSS_PREFIX" ./configure --prefix="$P" --target=$TARGET \
    --disable-examples --disable-unit-tests --disable-docs \
    --enable-pic --disable-webm-io $OPTS \
    >"$ROOT/logs/vpx-$ABI.log" 2>&1 || { echo "vpx $ABI CONFIG FAIL"; return 1; }
  make -j$JOBS >>"$ROOT/logs/vpx-$ABI.log" 2>&1 && make install >>"$ROOT/logs/vpx-$ABI.log" 2>&1
}

build_lame() {
  local ABI=$1; setup_abi "$ABI"; local P=$OUT/lame/$ABI
  [ -f "$P/lib/libmp3lame.a" ] && { echo "lame $ABI cached"; return; }
  cd "$SRC/lame-3.100"
  make distclean >/dev/null 2>&1 || true  # in-source tree shared across ABIs
  ./configure --prefix="$P" --host="${TRIP%%-android*}-linux-android" \
    --disable-shared --disable-frontend --disable-gtktest \
    >"$ROOT/logs/lame-$ABI.log" 2>&1 || { echo "lame $ABI CONFIG FAIL"; return 1; }
  make -j$JOBS >>"$ROOT/logs/lame-$ABI.log" 2>&1 && make install >>"$ROOT/logs/lame-$ABI.log" 2>&1
}

build_webp() {
  local ABI=$1; setup_abi "$ABI"; local P=$OUT/webp/$ABI
  [ -f "$P/lib/libwebp.a" ] && { echo "webp $ABI cached"; return; }
  local B=$ROOT/build/webp-$ABI; rm -rf "$B"; mkdir -p "$B"; cd "$B"
  "$CMAKE" "$SRC/libwebp-1.5.0" -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-$API -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX="$P" -DBUILD_SHARED_LIBS=OFF \
    -DWEBP_BUILD_CWEBP=OFF -DWEBP_BUILD_DWEBP=OFF -DWEBP_BUILD_GIF2WEBP=OFF \
    -DWEBP_BUILD_IMG2WEBP=OFF -DWEBP_BUILD_VWEBP=OFF -DWEBP_BUILD_WEBPINFO=OFF \
    -DWEBP_BUILD_WEBPMUX=OFF -DWEBP_BUILD_DEMUX=OFF \
    >"$ROOT/logs/webp-$ABI.log" 2>&1 || { echo "webp $ABI CONFIG FAIL"; return 1; }
  make -j$JOBS >>"$ROOT/logs/webp-$ABI.log" 2>&1 && make install >>"$ROOT/logs/webp-$ABI.log" 2>&1
  # webp .pc files omit -lm, breaking static consumers (pow/expf/log10).
  sed -i 's/^Libs.private: *-pthread$/Libs.private: -pthread -lm/' "$P/lib/pkgconfig/"*.pc
}

build_aom() {
  local ABI=$1; setup_abi "$ABI"; local P=$OUT/aom/$ABI
  unset CFLAGS CXXFLAGS LDFLAGS  # NDK toolchain file sets its own flags
  [ -f "$P/lib/libaom.a" ] && { echo "aom $ABI cached"; return; }
  local B=$ROOT/build/aom-$ABI; rm -rf "$B"; mkdir -p "$B"; cd "$B"
  local EXTRA=""
  [ "$ABI" = "x86_64" ] && EXTRA="-DAOM_TARGET_CPU=generic"
  "$CMAKE" "$SRC/libaom-3.11.0" -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-$API -DCMAKE_BUILD_TYPE=MinSizeRel \
    -DCMAKE_INSTALL_PREFIX="$P" -DENABLE_TESTS=0 -DENABLE_DOCS=0 -DCONFIG_PIC=1 \
    -DENABLE_EXAMPLES=0 -DENABLE_TOOLS=0 -DENABLE_TESTDATA=0 \
    -DCONFIG_RUNTIME_CPU_DETECT=0 $EXTRA \
    >"$ROOT/logs/aom-$ABI.log" 2>&1 || { echo "aom $ABI CONFIG FAIL"; return 1; }
  make -j$JOBS >>"$ROOT/logs/aom-$ABI.log" 2>&1 && make install >>"$ROOT/logs/aom-$ABI.log" 2>&1
}

ABIS="${1:-arm64-v8a x86_64}"
for ABI in $ABIS; do
  for LIB in x264 lame webp vpx x265 aom; do
    echo "=== $LIB $ABI ==="
    build_$LIB "$ABI" || echo "*** $LIB $ABI FAILED (see logs/$LIB-$ABI.log)"
  done
done
echo ALLDONE
find "$OUT" -name "*.a" | sort
