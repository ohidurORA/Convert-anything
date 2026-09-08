# Convert Anything — C++ backend (engine + in-process static FFmpeg).
cmake_minimum_required(VERSION 3.22.1)
project("converanything")

# Third-party FFmpeg tree built by <repo>/scripts/build-ffmpeg.sh.
# Override with -DFFMPEG_ANDROID_DIR=... ; layout:
#   $FFMPEG_ANDROID_DIR/src/ffmpeg-7.1.2      (sources)
#   $FFMPEG_ANDROID_DIR/build/ffmpeg-<abi>/  (config.h, .a, fftools/*.o)
#   $FFMPEG_ANDROID_DIR/out/<lib>/<abi>/{include,lib}
set(FFMPEG_ANDROID_DIR "/home/c/ffmpeg-android" CACHE PATH "Prebuilt FFmpeg tree")

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(FFABI "arm64-v8a")
elseif (${ANDROID_ABI} STREQUAL "x86_64")
    set(FFABI "x86_64")
else ()
    message(FATAL_ERROR "Unsupported ABI ${ANDROID_ABI}: build with scripts/build-ffmpeg.sh first")
endif ()

set(FFSRC "${FFMPEG_ANDROID_DIR}/src/ffmpeg-7.1.2")
set(FFBLD "${FFMPEG_ANDROID_DIR}/build/ffmpeg-${FFABI}")
set(FFOUT "${FFMPEG_ANDROID_DIR}/out")

foreach (D IN ITEMS "${FFSRC}/libavformat/avformat.h" "${FFBLD}/config.h"
        "${FFBLD}/libavformat/libavformat.a" "${FFBLD}/libavcodec/libavcodec.a")
    if (NOT EXISTS "${D}")
        message(FATAL_ERROR "Missing ${D}. Run scripts/build-ffmpeg.sh ${FFABI} (see scripts/README).")
    endif ()
endforeach ()

file(GLOB FFTOOLS_OBJ "${FFBLD}/fftools/*.o")
# ffmpeg.o is recompiled below with main() renamed to ffmpeg_main().
list(REMOVE_ITEM FFTOOLS_OBJ "${FFBLD}/fftools/ffmpeg.o")
set(FFMPEG_CLI_C "${FFSRC}/fftools/ffmpeg.c")
set_source_files_properties(${FFMPEG_CLI_C} PROPERTIES
        COMPILE_DEFINITIONS "main=ffmpeg_main"
        LANGUAGE C)

add_library(${CMAKE_PROJECT_NAME} SHARED
        engine.cpp
        bridge.cpp
        ${FFMPEG_CLI_C}
        ${FFTOOLS_OBJ})

target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE CONVERT_ANDROID)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        "${FFSRC}"
        "${FFSRC}/fftools"
        "${FFBLD}"
        "${FFOUT}/x264/${FFABI}/include"
        "${FFOUT}/x265/${FFABI}/include"
        "${FFOUT}/vpx/${FFABI}/include"
        "${FFOUT}/lame/${FFABI}/include"
        "${FFOUT}/webp/${FFABI}/include"
        "${FFOUT}/aom/${FFABI}/include")

# Static-link order is fragile across libav*/externals: one start-group.
# Static FFmpeg asm (e.g. tx_float_neon.S) uses absolute addressing for
# tables defined in the same link: bind locally so lld resolves statically.
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE "-Wl,-Bsymbolic")

target_link_libraries(${CMAKE_PROJECT_NAME} PRIVATE
        "-Wl,--start-group"
        "${FFBLD}/libavformat/libavformat.a"
        "${FFBLD}/libavcodec/libavcodec.a"
        "${FFBLD}/libavfilter/libavfilter.a"
        "${FFBLD}/libavutil/libavutil.a"
        "${FFBLD}/libswscale/libswscale.a"
        "${FFBLD}/libswresample/libswresample.a"
        "${FFOUT}/x264/${FFABI}/lib/libx264.a"
        "${FFOUT}/x265/${FFABI}/lib/libx265.a"
        "${FFOUT}/vpx/${FFABI}/lib/libvpx.a"
        "${FFOUT}/lame/${FFABI}/lib/libmp3lame.a"
        "${FFOUT}/webp/${FFABI}/lib/libwebp.a"
        "${FFOUT}/webp/${FFABI}/lib/libwebpmux.a"
        "${FFOUT}/webp/${FFABI}/lib/libwebpdemux.a"
        "${FFOUT}/webp/${FFABI}/lib/libsharpyuv.a"
        "${FFOUT}/aom/${FFABI}/lib/libaom.a"
        "-Wl,--end-group"
        android
        log
        jnigraphics
        z
        m
        dl)
