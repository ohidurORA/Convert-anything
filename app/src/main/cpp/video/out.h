#pragma once
#include <string>
#include <vector>

namespace convert {
namespace video_out {
// Optional audio map (0:a:0?) so muted clips still convert. Even size +
// yuv420p keeps H.264/VP9/HEVC encoders from rejecting odd sources.
inline std::vector<std::string> toWebm(const std::string& in, const std::string& out) {
    return {"-i", in, "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "libvpx-vp9", "-crf", "30", "-b:v", "0",
            "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
            "-c:a", "opus", "-strict", "-2", "-y", out};
}
inline std::vector<std::string> toMkv(const std::string& in, const std::string& out) {
    return {"-i", in, "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
            "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
            "-c:a", "aac", "-y", out};
}
inline std::vector<std::string> toHevc(const std::string& in, const std::string& out) {
    return {"-i", in, "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "libx265", "-tag:v", "hvc1", "-crf", "28",
            "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
            "-c:a", "aac", "-movflags", "+faststart", "-f", "mp4", "-y", out};
}
inline std::vector<std::string> toAvi(const std::string& in, const std::string& out) {
    return {"-i", in, "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "mpeg4", "-q:v", "3", "-c:a", "libmp3lame", "-y", out};
}
inline std::vector<std::string> toFlv(const std::string& in, const std::string& out) {
    return {"-i", in, "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "flv", "-c:a", "libmp3lame", "-y", out};
}
inline std::vector<std::string> toMpeg(const std::string& in, const std::string& out) {
    return {"-i", in, "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "mpeg2video", "-c:a", "mp2", "-y", out};
}
inline std::vector<std::string> toDefault(const std::string& in, const std::string& out) {
    return {"-i", in, "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
            "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
            "-c:a", "aac", "-movflags", "+faststart", "-y", out};
}
inline std::vector<std::string> build(const std::string& in, const std::string& out,
                                      const std::string& ext) {
    if (ext == "webm") return toWebm(in, out);
    if (ext == "mkv") return toMkv(in, out);
    if (ext == "hevc") return toHevc(in, out);
    if (ext == "avi") return toAvi(in, out);
    if (ext == "flv") return toFlv(in, out);
    if (ext == "mpeg" || ext == "mpg") return toMpeg(in, out);
    return toDefault(in, out);
}
}  // namespace video_out
}  // namespace convert
