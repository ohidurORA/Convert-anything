#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_flv {
// FLV input → every supported output. One function per pair.
inline std::vector<std::string> flvToMp4(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> flvToMkv(const std::string& in, const std::string& out) {
    return video_out::toMkv(in, out);
}
inline std::vector<std::string> flvToWebm(const std::string& in, const std::string& out) {
    return video_out::toWebm(in, out);
}
inline std::vector<std::string> flvToMov(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> flvToAvi(const std::string& in, const std::string& out) {
    return video_out::toAvi(in, out);
}
inline std::vector<std::string> flvToMpeg(const std::string& in, const std::string& out) {
    return video_out::toMpeg(in, out);
}
inline std::vector<std::string> flvToHevc(const std::string& in, const std::string& out) {
    return video_out::toHevc(in, out);
}

// Dispatcher for FLV inputs.
inline std::vector<std::string> buildFromFlv(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp4") return flvToMp4(in, out);
    if (ext == "mkv") return flvToMkv(in, out);
    if (ext == "webm") return flvToWebm(in, out);
    if (ext == "mov") return flvToMov(in, out);
    if (ext == "avi") return flvToAvi(in, out);
    if (ext == "mpeg") return flvToMpeg(in, out);
    if (ext == "hevc") return flvToHevc(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_flv
}  // namespace convert
