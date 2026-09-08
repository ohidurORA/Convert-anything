#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_avi {
// AVI input → every supported output. One function per pair.
inline std::vector<std::string> aviToMp4(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> aviToMkv(const std::string& in, const std::string& out) {
    return video_out::toMkv(in, out);
}
inline std::vector<std::string> aviToWebm(const std::string& in, const std::string& out) {
    return video_out::toWebm(in, out);
}
inline std::vector<std::string> aviToMov(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> aviToFlv(const std::string& in, const std::string& out) {
    return video_out::toFlv(in, out);
}
inline std::vector<std::string> aviToMpeg(const std::string& in, const std::string& out) {
    return video_out::toMpeg(in, out);
}
inline std::vector<std::string> aviToHevc(const std::string& in, const std::string& out) {
    return video_out::toHevc(in, out);
}

// Dispatcher for AVI inputs.
inline std::vector<std::string> buildFromAvi(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp4") return aviToMp4(in, out);
    if (ext == "mkv") return aviToMkv(in, out);
    if (ext == "webm") return aviToWebm(in, out);
    if (ext == "mov") return aviToMov(in, out);
    if (ext == "flv") return aviToFlv(in, out);
    if (ext == "mpeg") return aviToMpeg(in, out);
    if (ext == "hevc") return aviToHevc(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_avi
}  // namespace convert
