#pragma once
#include <string>
#include <vector>

#include "../audio/out.h"

namespace convert {
namespace v2a_mkv {
// MKV video → every supported audio output. One function per pair.
inline std::vector<std::string> mkvToMp3(const std::string& in, const std::string& out) {
    return audio_out::toMp3(in, out);
}
inline std::vector<std::string> mkvToAac(const std::string& in, const std::string& out) {
    return audio_out::toAac(in, out);
}
inline std::vector<std::string> mkvToFlac(const std::string& in, const std::string& out) {
    return audio_out::toFlac(in, out);
}
inline std::vector<std::string> mkvToWav(const std::string& in, const std::string& out) {
    return audio_out::toWav(in, out);
}
inline std::vector<std::string> mkvToOgg(const std::string& in, const std::string& out) {
    return audio_out::toOgg(in, out);
}
inline std::vector<std::string> mkvToOpus(const std::string& in, const std::string& out) {
    return audio_out::toOpus(in, out);
}
inline std::vector<std::string> mkvToM4a(const std::string& in, const std::string& out) {
    return audio_out::toM4a(in, out);
}
inline std::vector<std::string> mkvToAlac(const std::string& in, const std::string& out) {
    return audio_out::toAlac(in, out);
}

// Dispatcher for MKV video-to-audio.
inline std::vector<std::string> buildFromMkv(const std::string& in, const std::string& out,
                                         const std::string& ext) {
    if (ext == "mp3") return mkvToMp3(in, out);
    if (ext == "aac") return mkvToAac(in, out);
    if (ext == "flac") return mkvToFlac(in, out);
    if (ext == "wav") return mkvToWav(in, out);
    if (ext == "ogg") return mkvToOgg(in, out);
    if (ext == "opus") return mkvToOpus(in, out);
    if (ext == "m4a") return mkvToM4a(in, out);
    if (ext == "alac") return mkvToAlac(in, out);
    return audio_out::build(in, out, ext);
}
}  // namespace v2a_mkv
}  // namespace convert
