#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_mp4 {
// MP4 input → every supported output. One function per pair.
inline std::vector<std::string> mp4ToMkv(const std::string& in, const std::string& out) {
    return video_out::toMkv(in, out);
}
inline std::vector<std::string> mp4ToWebm(const std::string& in, const std::string& out) {
    return video_out::toWebm(in, out);
}
inline std::vector<std::string> mp4ToMov(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> mp4ToAvi(const std::string& in, const std::string& out) {
    return video_out::toAvi(in, out);
}
inline std::vector<std::string> mp4ToFlv(const std::string& in, const std::string& out) {
    return video_out::toFlv(in, out);
}
inline std::vector<std::string> mp4ToMpeg(const std::string& in, const std::string& out) {
    return video_out::toMpeg(in, out);
}
inline std::vector<std::string> mp4ToHevc(const std::string& in, const std::string& out) {
    return video_out::toHevc(in, out);
}

// Dispatcher for MP4 inputs.
inline std::vector<std::string> buildFromMp4(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mkv") return mp4ToMkv(in, out);
    if (ext == "webm") return mp4ToWebm(in, out);
    if (ext == "mov") return mp4ToMov(in, out);
    if (ext == "avi") return mp4ToAvi(in, out);
    if (ext == "flv") return mp4ToFlv(in, out);
    if (ext == "mpeg") return mp4ToMpeg(in, out);
    if (ext == "hevc") return mp4ToHevc(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_mp4
}  // namespace convert
