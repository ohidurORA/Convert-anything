#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_mkv {
// MKV input → every supported output. One function per pair.
inline std::vector<std::string> mkvToMp4(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> mkvToWebm(const std::string& in, const std::string& out) {
    return video_out::toWebm(in, out);
}
inline std::vector<std::string> mkvToMov(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> mkvToAvi(const std::string& in, const std::string& out) {
    return video_out::toAvi(in, out);
}
inline std::vector<std::string> mkvToFlv(const std::string& in, const std::string& out) {
    return video_out::toFlv(in, out);
}
inline std::vector<std::string> mkvToMpeg(const std::string& in, const std::string& out) {
    return video_out::toMpeg(in, out);
}
inline std::vector<std::string> mkvToHevc(const std::string& in, const std::string& out) {
    return video_out::toHevc(in, out);
}

// Dispatcher for MKV inputs.
inline std::vector<std::string> buildFromMkv(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp4") return mkvToMp4(in, out);
    if (ext == "webm") return mkvToWebm(in, out);
    if (ext == "mov") return mkvToMov(in, out);
    if (ext == "avi") return mkvToAvi(in, out);
    if (ext == "flv") return mkvToFlv(in, out);
    if (ext == "mpeg") return mkvToMpeg(in, out);
    if (ext == "hevc") return mkvToHevc(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_mkv
}  // namespace convert
