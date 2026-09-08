#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_mpeg {
// MPEG input → every supported output. One function per pair.
inline std::vector<std::string> mpegToMp4(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> mpegToMkv(const std::string& in, const std::string& out) {
    return video_out::toMkv(in, out);
}
inline std::vector<std::string> mpegToWebm(const std::string& in, const std::string& out) {
    return video_out::toWebm(in, out);
}
inline std::vector<std::string> mpegToMov(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> mpegToAvi(const std::string& in, const std::string& out) {
    return video_out::toAvi(in, out);
}
inline std::vector<std::string> mpegToFlv(const std::string& in, const std::string& out) {
    return video_out::toFlv(in, out);
}
inline std::vector<std::string> mpegToHevc(const std::string& in, const std::string& out) {
    return video_out::toHevc(in, out);
}

// Dispatcher for MPEG inputs.
inline std::vector<std::string> buildFromMpeg(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp4") return mpegToMp4(in, out);
    if (ext == "mkv") return mpegToMkv(in, out);
    if (ext == "webm") return mpegToWebm(in, out);
    if (ext == "mov") return mpegToMov(in, out);
    if (ext == "avi") return mpegToAvi(in, out);
    if (ext == "flv") return mpegToFlv(in, out);
    if (ext == "hevc") return mpegToHevc(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_mpeg
}  // namespace convert
