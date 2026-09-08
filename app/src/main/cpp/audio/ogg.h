#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_ogg {
// OGG input → every supported output. One function per pair.
inline std::vector<std::string> oggToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> oggToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> oggToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> oggToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> oggToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> oggToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> oggToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for OGG inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromOgg(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return oggToMp3(in, out);
    if (ext == "aac") return oggToAac(in, out);
    if (ext == "flac") return oggToFlac(in, out);
    if (ext == "wav") return oggToWav(in, out);
    if (ext == "opus") return oggToOpus(in, out);
    if (ext == "m4a") return oggToM4a(in, out);
    if (ext == "alac") return oggToAlac(in, out);
    return audio_out::toOgg(in, out);
}
}  // namespace audio_ogg
}  // namespace convert
