#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_mov {
// MOV video → every supported audio output. One function per pair.
inline std::vector<std::string> movToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> movToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> movToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> movToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> movToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> movToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> movToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> movToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for MOV video-to-audio.
inline std::vector<std::string> buildFromMov(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return movToMp3(in, out);
    if (ext == "aac") return movToAac(in, out);
    if (ext == "flac") return movToFlac(in, out);
    if (ext == "wav") return movToWav(in, out);
    if (ext == "ogg") return movToOgg(in, out);
    if (ext == "opus") return movToOpus(in, out);
    if (ext == "m4a") return movToM4a(in, out);
    if (ext == "alac") return movToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_mov
}  // namespace convert
