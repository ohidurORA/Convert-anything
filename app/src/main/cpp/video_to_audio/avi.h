#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_avi {
// AVI video → every supported audio output. One function per pair.
inline std::vector<std::string> aviToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> aviToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> aviToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> aviToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> aviToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> aviToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> aviToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> aviToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for AVI video-to-audio.
inline std::vector<std::string> buildFromAvi(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return aviToMp3(in, out);
    if (ext == "aac") return aviToAac(in, out);
    if (ext == "flac") return aviToFlac(in, out);
    if (ext == "wav") return aviToWav(in, out);
    if (ext == "ogg") return aviToOgg(in, out);
    if (ext == "opus") return aviToOpus(in, out);
    if (ext == "m4a") return aviToM4a(in, out);
    if (ext == "alac") return aviToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_avi
}  // namespace convert
