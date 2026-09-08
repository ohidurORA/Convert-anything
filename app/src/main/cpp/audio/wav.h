#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_wav {
// WAV input → every supported output. One function per pair.
inline std::vector<std::string> wavToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> wavToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> wavToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> wavToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> wavToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> wavToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> wavToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for WAV inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromWav(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return wavToMp3(in, out);
    if (ext == "aac") return wavToAac(in, out);
    if (ext == "flac") return wavToFlac(in, out);
    if (ext == "ogg") return wavToOgg(in, out);
    if (ext == "opus") return wavToOpus(in, out);
    if (ext == "m4a") return wavToM4a(in, out);
    if (ext == "alac") return wavToAlac(in, out);
    return audio_out::toWav(in, out);
}
}  // namespace audio_wav
}  // namespace convert
