#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_mp4 {
// MP4 video → every supported audio output. One function per pair.
inline std::vector<std::string> mp4ToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> mp4ToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> mp4ToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> mp4ToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> mp4ToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> mp4ToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> mp4ToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> mp4ToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for MP4 video-to-audio.
inline std::vector<std::string> buildFromMp4(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return mp4ToMp3(in, out);
    if (ext == "aac") return mp4ToAac(in, out);
    if (ext == "flac") return mp4ToFlac(in, out);
    if (ext == "wav") return mp4ToWav(in, out);
    if (ext == "ogg") return mp4ToOgg(in, out);
    if (ext == "opus") return mp4ToOpus(in, out);
    if (ext == "m4a") return mp4ToM4a(in, out);
    if (ext == "alac") return mp4ToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_mp4
}  // namespace convert
