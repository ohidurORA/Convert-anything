#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_alac {
// ALAC input → every supported output. One function per pair.
inline std::vector<std::string> alacToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> alacToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> alacToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> alacToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> alacToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> alacToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> alacToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}

// Dispatcher for ALAC inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromAlac(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return alacToMp3(in, out);
    if (ext == "aac") return alacToAac(in, out);
    if (ext == "flac") return alacToFlac(in, out);
    if (ext == "wav") return alacToWav(in, out);
    if (ext == "ogg") return alacToOgg(in, out);
    if (ext == "opus") return alacToOpus(in, out);
    if (ext == "m4a") return alacToM4a(in, out);
    return audio_out::toAlac(in, out);
}
}  // namespace audio_alac
}  // namespace convert
