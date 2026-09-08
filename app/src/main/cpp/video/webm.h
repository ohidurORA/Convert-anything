#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_webm {
// WEBM input → every supported output. One function per pair.
inline std::vector<std::string> webmToMp4(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> webmToMkv(const std::string& in, const std::string& out) {
    return video_out::toMkv(in, out);
}
inline std::vector<std::string> webmToMov(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> webmToAvi(const std::string& in, const std::string& out) {
    return video_out::toAvi(in, out);
}
inline std::vector<std::string> webmToFlv(const std::string& in, const std::string& out) {
    return video_out::toFlv(in, out);
}
inline std::vector<std::string> webmToMpeg(const std::string& in, const std::string& out) {
    return video_out::toMpeg(in, out);
}
inline std::vector<std::string> webmToHevc(const std::string& in, const std::string& out) {
    return video_out::toHevc(in, out);
}

// Dispatcher for WEBM inputs.
inline std::vector<std::string> buildFromWebm(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp4") return webmToMp4(in, out);
    if (ext == "mkv") return webmToMkv(in, out);
    if (ext == "mov") return webmToMov(in, out);
    if (ext == "avi") return webmToAvi(in, out);
    if (ext == "flv") return webmToFlv(in, out);
    if (ext == "mpeg") return webmToMpeg(in, out);
    if (ext == "hevc") return webmToHevc(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_webm
}  // namespace convert
