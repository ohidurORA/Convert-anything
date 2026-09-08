#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_flv {
// FLV video → every supported audio output. One function per pair.
inline std::vector<std::string> flvToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> flvToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> flvToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> flvToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> flvToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> flvToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> flvToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> flvToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for FLV video-to-audio.
inline std::vector<std::string> buildFromFlv(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return flvToMp3(in, out);
    if (ext == "aac") return flvToAac(in, out);
    if (ext == "flac") return flvToFlac(in, out);
    if (ext == "wav") return flvToWav(in, out);
    if (ext == "ogg") return flvToOgg(in, out);
    if (ext == "opus") return flvToOpus(in, out);
    if (ext == "m4a") return flvToM4a(in, out);
    if (ext == "alac") return flvToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_flv
}  // namespace convert
