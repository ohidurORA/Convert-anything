#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_aac {
// AAC input → every supported output. One function per pair.
inline std::vector<std::string> aacToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> aacToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> aacToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> aacToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> aacToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> aacToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> aacToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for AAC inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromAac(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return aacToMp3(in, out);
    if (ext == "flac") return aacToFlac(in, out);
    if (ext == "wav") return aacToWav(in, out);
    if (ext == "ogg") return aacToOgg(in, out);
    if (ext == "opus") return aacToOpus(in, out);
    if (ext == "m4a") return aacToM4a(in, out);
    if (ext == "alac") return aacToAlac(in, out);
    return audio_out::toAac(in, out);
}
}  // namespace audio_aac
}  // namespace convert
