#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace video_hevc {
// HEVC input → every supported output. One function per pair.
inline std::vector<std::string> hevcToMp4(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> hevcToMkv(const std::string& in, const std::string& out) {
    return video_out::toMkv(in, out);
}
inline std::vector<std::string> hevcToWebm(const std::string& in, const std::string& out) {
    return video_out::toWebm(in, out);
}
inline std::vector<std::string> hevcToMov(const std::string& in, const std::string& out) {
    return video_out::toDefault(in, out);
}
inline std::vector<std::string> hevcToAvi(const std::string& in, const std::string& out) {
    return video_out::toAvi(in, out);
}
inline std::vector<std::string> hevcToFlv(const std::string& in, const std::string& out) {
    return video_out::toFlv(in, out);
}
inline std::vector<std::string> hevcToMpeg(const std::string& in, const std::string& out) {
    return video_out::toMpeg(in, out);
}

// Dispatcher for HEVC inputs.
inline std::vector<std::string> buildFromHevc(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp4") return hevcToMp4(in, out);
    if (ext == "mkv") return hevcToMkv(in, out);
    if (ext == "webm") return hevcToWebm(in, out);
    if (ext == "mov") return hevcToMov(in, out);
    if (ext == "avi") return hevcToAvi(in, out);
    if (ext == "flv") return hevcToFlv(in, out);
    if (ext == "mpeg") return hevcToMpeg(in, out);
    return video_out::build(in, out, ext);
}
}  // namespace video_hevc
}  // namespace convert
