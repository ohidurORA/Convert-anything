#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_webm {
// WEBM video → every supported audio output. One function per pair.
inline std::vector<std::string> webmToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> webmToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> webmToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> webmToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> webmToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> webmToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> webmToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> webmToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for WEBM video-to-audio.
inline std::vector<std::string> buildFromWebm(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return webmToMp3(in, out);
    if (ext == "aac") return webmToAac(in, out);
    if (ext == "flac") return webmToFlac(in, out);
    if (ext == "wav") return webmToWav(in, out);
    if (ext == "ogg") return webmToOgg(in, out);
    if (ext == "opus") return webmToOpus(in, out);
    if (ext == "m4a") return webmToM4a(in, out);
    if (ext == "alac") return webmToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_webm
}  // namespace convert
