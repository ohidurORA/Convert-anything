#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace audio_mp3 {
// MP3 input → every supported output. One function per pair.
inline std::vector<std::string> mp3ToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> mp3ToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> mp3ToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> mp3ToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> mp3ToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> mp3ToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> mp3ToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for MP3 inputs (self/unknown falls back to its own builder).
inline std::vector<std::string> buildFromMp3(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "aac") return mp3ToAac(in, out);
    if (ext == "flac") return mp3ToFlac(in, out);
    if (ext == "wav") return mp3ToWav(in, out);
    if (ext == "ogg") return mp3ToOgg(in, out);
    if (ext == "opus") return mp3ToOpus(in, out);
    if (ext == "m4a") return mp3ToM4a(in, out);
    if (ext == "alac") return mp3ToAlac(in, out);
    return audio_out::toMp3(in, out);
}
}  // namespace audio_mp3
}  // namespace convert
