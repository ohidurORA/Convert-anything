#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_mpeg {
// MPEG video → every supported audio output. One function per pair.
inline std::vector<std::string> mpegToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> mpegToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> mpegToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> mpegToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> mpegToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> mpegToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> mpegToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> mpegToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for MPEG video-to-audio.
inline std::vector<std::string> buildFromMpeg(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return mpegToMp3(in, out);
    if (ext == "aac") return mpegToAac(in, out);
    if (ext == "flac") return mpegToFlac(in, out);
    if (ext == "wav") return mpegToWav(in, out);
    if (ext == "ogg") return mpegToOgg(in, out);
    if (ext == "opus") return mpegToOpus(in, out);
    if (ext == "m4a") return mpegToM4a(in, out);
    if (ext == "alac") return mpegToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_mpeg
}  // namespace convert
