#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_flac {
// FLAC input → every supported output. One function per pair.
inline std::vector<std::string> flacToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> flacToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> flacToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> flacToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> flacToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> flacToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> flacToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for FLAC inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromFlac(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return flacToMp3(in, out);
    if (ext == "aac") return flacToAac(in, out);
    if (ext == "wav") return flacToWav(in, out);
    if (ext == "ogg") return flacToOgg(in, out);
    if (ext == "opus") return flacToOpus(in, out);
    if (ext == "m4a") return flacToM4a(in, out);
    if (ext == "alac") return flacToAlac(in, out);
    return audio_out::toFlac(in, out);
}
}  // namespace audio_flac
}  // namespace convert
