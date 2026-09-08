#!/usr/bin/env bash
# Host argv contract: compile the REAL engine and assert every advertised
# destination uses a dedicated encoder (not the generic -update still path
# that broke PPM→DPX, and not AAC-in-ADTS for .m4a).
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CPP="$REPO/app/src/main/cpp"
BUILDDIR="$(mktemp -d /tmp/ca-argv.XXXXXX)"
trap 'rm -rf "$BUILDDIR"' EXIT

cat > "$BUILDDIR/harness.cpp" <<'HARNESS'
#include "engine.h"
#include <cstdio>
#include <string>
namespace convert { namespace bridge {
bool dispatchRemote(const std::string&,const std::string&,const std::string&,
                    const std::string&,const std::string&,const std::string&,
                    const std::string&,const std::string&) { return false; }
void cancelRemote(const std::string&) {}
void killConverter() {}
}}
int main(int argc, char** argv) {
    if (argc < 7) return 2;
    auto args = convert::Engine::instance().buildArgs(
        argv[1], argv[4], argv[5], argv[3], argv[6], argv[2]);
    for (const auto& a : args) std::printf("%s\n", a.c_str());
    return 0;
}
HARNESS

g++ -std=c++17 -pthread -I "$CPP" "$BUILDDIR/harness.cpp" "$CPP/engine.cpp" -o "$BUILDDIR/argv"

argv() { "$BUILDDIR/argv" "$@"; }

fail=0
assert_has() {
    local hay="$1"; shift
    if ! grep -qxF -- "$1" <<<"$hay"; then
        echo "MISSING '$1' in:" >&2
        echo "$hay" >&2
        fail=$((fail+1))
    fi
}
assert_not() {
    local hay="$1"; shift
    if grep -qxF -- "$1" <<<"$hay"; then
        echo "UNEXPECTED '$1' in:" >&2
        echo "$hay" >&2
        fail=$((fail+1))
    fi
}

PPM_DPX=$(argv image ppm dpx /tmp/in.ppm /tmp/out.dpx rgb24)
assert_has "$PPM_DPX" dpx
assert_not "$PPM_DPX" -update
assert_has "$PPM_DPX" -c:v

JPG_DPX=$(argv image jpg dpx /tmp/in.jpg /tmp/out.dpx yuvj420p)
assert_has "$JPG_DPX" dpx
assert_not "$JPG_DPX" -update

DPX_PPM=$(argv image dpx ppm /tmp/in.dpx /tmp/out.ppm gbrp10le)
assert_has "$DPX_PPM" ppm
assert_not "$DPX_PPM" -update

M4A=$(argv audio mp3 m4a /tmp/in.mp3 /tmp/out.m4a "")
assert_has "$M4A" ipod
assert_not "$M4A" adts

AAC=$(argv audio wav aac /tmp/in.wav /tmp/out.aac "")
assert_has "$AAC" adts

ALAC=$(argv audio flac alac /tmp/in.flac /tmp/out.m4a "")
assert_has "$ALAC" alac
assert_has "$ALAC" ipod

V2A=$(argv video_to_audio mp4 mp3 /tmp/in.mp4 /tmp/out.mp3 "")
assert_has "$V2A" libmp3lame
assert_has "$V2A" -vn

HEVC=$(argv video avi hevc /tmp/in.avi /tmp/out.mp4 yuv420p)
assert_has "$HEVC" libx265
assert_has "$HEVC" hvc1
assert_has "$HEVC" -f
assert_has "$HEVC" mp4

JPEG=$(argv image jpeg png /tmp/in.jpeg /tmp/out.png rgb24)
assert_has "$JPEG" png

TIF=$(argv image tif jpg /tmp/in.tif /tmp/out.jpg rgb24)
assert_has "$TIF" mjpeg

if [ "$fail" != 0 ]; then
    echo "ARGV CONTRACT $fail FAILURES"
    exit 1
fi
echo "ARGV CONTRACT OK"
