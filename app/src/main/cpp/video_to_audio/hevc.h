#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_hevc {
// HEVC video → every supported audio output. One function per pair.
inline std::vector<std::string> hevcToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> hevcToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> hevcToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> hevcToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> hevcToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> hevcToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> hevcToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> hevcToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for HEVC video-to-audio.
inline std::vector<std::string> buildFromHevc(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return hevcToMp3(in, out);
    if (ext == "aac") return hevcToAac(in, out);
    if (ext == "flac") return hevcToFlac(in, out);
    if (ext == "wav") return hevcToWav(in, out);
    if (ext == "ogg") return hevcToOgg(in, out);
    if (ext == "opus") return hevcToOpus(in, out);
    if (ext == "m4a") return hevcToM4a(in, out);
    if (ext == "alac") return hevcToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_hevc
}  // namespace convert
