#!/usr/bin/env bash
# Conversion matrix verifier.
#
# Exercises EVERY advertised (conversionType, srcFormat, dstFormat) pair the
# way the app actually would: it asks the REAL engine (app/src/main/cpp/engine.cpp)
# for the exact ffmpeg argv via buildArgs(), then runs real ffmpeg with that
# argv and validates the output file with ffprobe (non-empty + correct codec
# family present). A pair "passes" only when ffmpeg exits 0 AND a real,
# non-empty output stream of the expected kind is produced — the same gate the
# production engine uses via validateOutputFile().
#
# This is the machine-readable test matrix the spec asks for (section 18): for
# each advertised path it produces a TSV row with a clear PASS/FAIL and the
# observed codec, so regressions are visible at a glance and the whole thing is
# repeatable on any host with ffmpeg/ffprobe.
#
# Output: a results TSV under $OUTROOT plus a human summary to stdout.
# Exit code 0 if every pair passes, nonzero otherwise.

set -u
FF="${FF:-/usr/bin/ffmpeg}"
FFP="${FFP:-/usr/bin/ffprobe}"

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CPP="$REPO/app/src/main/cpp"
BUILDDIR="$(mktemp -d /tmp/ca-matrix.XXXXXX)"
trap 'rm -rf "$BUILDDIR"' EXIT

# ---- 1. Build a tiny harness over the real engine (buildArgs is the only
# symbol we call; bridge/JNI symbols are stubbed on the host path). ----
cat > "$BUILDDIR/harness.cpp" <<'HARNESS'
#include <atomic>
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
    if (argc < 7) { std::fprintf(stderr,"usage: matrix type inExt outExt in out pixFmt\n"); return 2; }
    auto args = convert::Engine::instance().buildArgs(
        argv[1], argv[4], argv[5], argv[3], argv[6], argv[2]);
    for (const auto& a : args) std::printf("%s\n", a.c_str());
    return 0;
}
HARNESS
if ! g++ -std=c++17 -pthread -I "$CPP" "$BUILDDIR/harness.cpp" "$CPP/engine.cpp" \
        -o "$BUILDDIR/matrix" >/tmp/ca-matrix-build.log 2>&1; then
    echo "ENGINE HARNESS BUILD FAILED - see /tmp/ca-matrix-build.log" >&2
    tail -30 /tmp/ca-matrix-build.log >&2
    exit 2
fi
echo "harness built from $CPP/engine.cpp" >&2

# ---- 2. Generate a small real fixture for every input format. ----
FIX="$BUILDDIR/fixtures"; mkdir -p "$FIX"
SRC_MP3="$FIX/base.mp3"
$FF -y -v error -f lavfi -i "sine=frequency=440:duration=1" -c:a libmp3lame -q:a 2 "$SRC_MP3"
$FF -y -v error -i "$SRC_MP3" -c:a aac -b:a 128k "$FIX/a.aac"
$FF -y -v error -i "$SRC_MP3" -c:a flac "$FIX/a.flac"
$FF -y -v error -i "$SRC_MP3" -c:a pcm_s16le "$FIX/a.wav"
$FF -y -v error -i "$SRC_MP3" -c:a libvorbis -q:a 4 "$FIX/a.ogg"
$FF -y -v error -i "$SRC_MP3" -c:a libopus -b:a 128k "$FIX/a.opus"
$FF -y -v error -i "$SRC_MP3" -c:a aac -f ipod "$FIX/a.m4a"
$FF -y -v error -i "$SRC_MP3" -c:a alac -f ipod "$FIX/a.alac"
V='-f lavfi -i testsrc2=d=1:s=160x120:r=10 -f lavfi -i sine=frequency=440:duration=1'
$FF -y -v error $V -c:v libx264 -preset veryfast -pix_fmt yuv420p -c:a aac "$FIX/v.mp4"
$FF -y -v error $V -c:v libx264 -preset veryfast -pix_fmt yuv420p -c:a aac "$FIX/v.mkv"
$FF -y -v error $V -c:v libvpx-vp9 -crf 30 -b:v 0 -pix_fmt yuv420p -c:a libopus "$FIX/v.webm"
$FF -y -v error $V -c:v libx264 -preset veryfast -pix_fmt yuv420p -c:a aac -f mov "$FIX/v.mov"
$FF -y -v error $V -c:v mpeg4 -q:v 3 -c:a libmp3lame "$FIX/v.avi"
$FF -y -v error $V -c:v flv -c:a libmp3lame "$FIX/v.flv"
$FF -y -v error $V -c:v mpeg2video -c:a mp2 "$FIX/v.mpeg"
# HEVC is advertised as H.265-in-MP4. A file named .hevc is a raw
# elementary stream (no audio, no mp4 demuxer), so the fixture must
# be a real MP4 even though the registry id is "hevc".
$FF -y -v error $V -c:v libx265 -preset veryfast -pix_fmt yuv420p -tag:v hvc1 -c:a aac -f mp4 "$FIX/v.hevc.mp4"
IMG='-f lavfi -i testsrc=size=64x48:rate=1'
$FF -y -v error $IMG -frames:v 1 "$FIX/i.jpg"
$FF -y -v error $IMG -frames:v 1 "$FIX/i.png"
$FF -y -v error $IMG -frames:v 1 -c:v libwebp "$FIX/i.webp"
$FF -y -v error $IMG -frames:v 1 -c:v libaom-av1 "$FIX/i.avif"
$FF -y -v error $IMG -frames:v 1 -c:v bmp "$FIX/i.bmp"
$FF -y -v error $IMG -frames:v 1 -c:v tiff "$FIX/i.tiff"
$FF -y -v error $IMG -frames:v 2 "$FIX/i.gif"
$FF -y -v error $IMG -frames:v 1 -c:v targa "$FIX/i.tga"
$FF -y -v error $IMG -frames:v 1 -c:v jpeg2000 "$FIX/i.jp2"
$FF -y -v error $IMG -frames:v 1 -c:v qoi "$FIX/i.qoi"
$FF -y -v error $IMG -frames:v 1 -c:v dpx "$FIX/i.dpx"
$FF -y -v error $IMG -frames:v 1 -c:v ppm "$FIX/i.ppm"

# Fixture lookup: resolve (typeId, inExt) -> real input file on disk.
fixture() {
    case "$1" in
        audio) case "$2" in mp3) echo "$SRC_MP3";; alac) echo "$FIX/a.alac";; *) echo "$FIX/a.$2";; esac;;
        video|video_to_audio) case "$2" in hevc) echo "$FIX/v.hevc.mp4";; *) echo "$FIX/v.$2";; esac;;
        image) echo "$FIX/i.$2";;
    esac
}

# Expected dominant output kind for a conversion type.
# audio/video_to_audio -> must contain an audio stream; video/image -> video.
expected_kind() { case "$1" in audio|video_to_audio) echo a;; *) echo v;; esac; }

# On-disk container for advertised formats whose muxer does not match the
# extension (mirrors engine.cpp containerOf).
container_ext() {
    case "$1" in
        hevc) echo mp4;;
        alac) echo m4a;;
        *) echo "$1";;
    esac
}

# ---- 3. Run the matrix. ----
OUTROOT="${OUTROOT:-$BUILDDIR/out}"; mkdir -p "$OUTROOT"
REPORT="$OUTROOT/matrix.tsv"
printf 'type\tsrc\tdst\tresult\tstream\tsize\n' > "$REPORT"

pass=0; fail=0; total=0
run_type() {
    local typeId="$1"; shift
    local outList="$1"; shift
    local -a inList=("$@")
    local inExt outExt src out args rc kind vcodec acodec
    for inExt in "${inList[@]}"; do
        src="$(fixture "$typeId" "$inExt")"
        if [ ! -f "$src" ]; then
            # A missing fixture is a test-harness failure, not a neutral skip:
            # silently continuing would let the matrix report success while
            # never actually exercising these pairs.
            printf '%s\t%s\t-\tNO_FIXTURE\t\t\t\n' "$typeId" "$inExt" >> "$REPORT"
            fail=$((fail+1)); total=$((total+1)); continue
        fi
        for outExt in $outList; do
            out="$OUTROOT/${typeId}_${inExt}_to_${outExt}.$(container_ext "$outExt")"
            args=()
            while IFS= read -r l; do args+=("$l"); done \
                < <("$BUILDDIR/matrix" "$typeId" "$inExt" "$outExt" "$src" "$out" "yuv420p")
            "$FF" -y "${args[@]}" >/dev/null 2>&1; rc=$?
            kind="$(expected_kind "$typeId")"
            if [ "$rc" = 0 ] && [ -s "$out" ]; then
                vcodec="$("$FFP" -v error -select_streams v:0 -show_entries stream=codec_name -of csv=p=0 "$out" 2>/dev/null)"
                acodec="$("$FFP" -v error -select_streams a:0 -show_entries stream=codec_name -of csv=p=0 "$out" 2>/dev/null)"
                if { [ "$kind" = v ] && [ -n "$vcodec" ]; } || { [ "$kind" = a ] && [ -n "$acodec" ]; }; then
                    printf '%s\t%s\t%s\tPASS\tv=%s a=%s\t%s\n' \
                        "$typeId" "$inExt" "$outExt" "$vcodec" "$acodec" "$(stat -c%s "$out")" >> "$REPORT"
                    pass=$((pass+1))
                else
                    printf '%s\t%s\t%s\tBAD_STREAM\tv=%s a=%s\t%s\n' \
                        "$typeId" "$inExt" "$outExt" "$vcodec" "$acodec" "$(stat -c%s "$out")" >> "$REPORT"
                    fail=$((fail+1))
                fi
            else
                printf '%s\t%s\t%s\tFFMPEG_RC=%s\t\t\t\n' "$typeId" "$inExt" "$outExt" "$rc" >> "$REPORT"
                fail=$((fail+1))
            fi
            total=$((total+1))
        done
    done
}

run_type audio \
    'mp3 aac flac wav ogg opus m4a alac' \
    mp3 aac flac wav ogg opus m4a alac
run_type video \
    'mp4 mkv webm mov avi flv mpeg hevc' \
    mp4 mkv webm mov avi flv mpeg hevc
run_type image \
    'jpg png webp avif gif bmp tiff tga jp2 qoi dpx ppm' \
    jpg png webp avif bmp tiff gif tga jp2 qoi dpx ppm
run_type video_to_audio \
    'mp3 aac flac wav ogg opus m4a alac' \
    mp4 mkv webm mov avi flv mpeg hevc

# ---- 4. Report. ----
echo
echo "=== conversion matrix: $pass/$total PASS, $fail FAIL ==="
echo "type       src        dst        result       streams         size"
awk -F'\t' '{printf "%-16s %-10s %-10s %-16s %-18s %s\n", $1,$2,$3,$4,$5,$6}' "$REPORT"
echo
echo "full report: $REPORT"
[ "$fail" = 0 ] && echo "ALL ADVERTISED CONVERSIONS VERIFIED WORKING"
exit "$fail"
