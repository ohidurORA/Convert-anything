#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_m4a {
// M4A input → every supported output. One function per pair.
inline std::vector<std::string> m4aToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> m4aToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> m4aToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> m4aToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> m4aToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> m4aToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> m4aToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for M4A inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromM4a(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return m4aToMp3(in, out);
    if (ext == "aac") return m4aToAac(in, out);
    if (ext == "flac") return m4aToFlac(in, out);
    if (ext == "wav") return m4aToWav(in, out);
    if (ext == "ogg") return m4aToOgg(in, out);
    if (ext == "opus") return m4aToOpus(in, out);
    if (ext == "alac") return m4aToAlac(in, out);
    return audio_out::toM4a(in, out);
}
}  // namespace audio_m4a
}  // namespace convert
