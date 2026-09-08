#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_mov {
// MOV input → every supported output. One function per pair.
inline std::vector<std::string> movToMp4(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> movToMkv(const std::string& in, const std::string& out) {
    return video_out::toMkv(in, out);
}
inline std::vector<std::string> movToWebm(const std::string& in, const std::string& out) {
    return video_out::toWebm(in, out);
}
inline std::vector<std::string> movToAvi(const std::string& in, const std::string& out) {
    return video_out::toAvi(in, out);
}
inline std::vector<std::string> movToFlv(const std::string& in, const std::string& out) {
    return video_out::toFlv(in, out);
}
inline std::vector<std::string> movToMpeg(const std::string& in, const std::string& out) {
    return video_out::toMpeg(in, out);
}
inline std::vector<std::string> movToHevc(const std::string& in, const std::string& out) {
    return video_out::toHevc(in, out);
}

// Dispatcher for MOV inputs.
inline std::vector<std::string> buildFromMov(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp4") return movToMp4(in, out);
    if (ext == "mkv") return movToMkv(in, out);
    if (ext == "webm") return movToWebm(in, out);
    if (ext == "avi") return movToAvi(in, out);
    if (ext == "flv") return movToFlv(in, out);
    if (ext == "mpeg") return movToMpeg(in, out);
    if (ext == "hevc") return movToHevc(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_mov
}  // namespace convert
