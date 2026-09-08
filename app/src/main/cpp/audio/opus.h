#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_opus {
// OPUS input → every supported output. One function per pair.
inline std::vector<std::string> opusToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> opusToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> opusToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> opusToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> opusToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> opusToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> opusToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for OPUS inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromOpus(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return opusToMp3(in, out);
    if (ext == "aac") return opusToAac(in, out);
    if (ext == "flac") return opusToFlac(in, out);
    if (ext == "wav") return opusToWav(in, out);
    if (ext == "ogg") return opusToOgg(in, out);
    if (ext == "m4a") return opusToM4a(in, out);
    if (ext == "alac") return opusToAlac(in, out);
    return audio_out::toOpus(in, out);
}
}  // namespace audio_opus
}  // namespace convert
