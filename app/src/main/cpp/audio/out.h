#pragma once
#include <string>
#include <vector>

namespace convert {
namespace audio_out {
// One builder per OUTPUT format. -vn/-sn/-dn: audio flows carry audio only
// (cover art / attached pictures choke ogg/flac/adts muxers).
// AAC in .aac is ADTS; AAC in .m4a is the ipod/mp4 container — these are
// different muxers and must not share argv.
inline std::vector<std::string> toMp3(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "libmp3lame", "-b:a", "320k", "-y", out};
}
inline std::vector<std::string> toAac(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "aac", "-b:a", "256k", "-f", "adts", "-y", out};
}
inline std::vector<std::string> toM4a(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "aac", "-b:a", "256k", "-f", "ipod",
            "-movflags", "+faststart", "-y", out};
}
inline std::vector<std::string> toWav(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "pcm_s16le", "-ar", "44100", "-y", out};
}
inline std::vector<std::string> toOgg(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "vorbis", "-q:a", "6", "-ac", "2", "-strict", "-2", "-y", out};
}
inline std::vector<std::string> toOpus(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "opus", "-b:a", "160k", "-strict", "-2", "-y", out};
}
inline std::vector<std::string> toFlac(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "flac", "-y", out};
}
inline std::vector<std::string> toAlac(const std::string& in, const std::string& out) {
    return {"-i", in, "-vn", "-sn", "-dn", "-map", "0:a:0",
            "-c:a", "alac", "-f", "ipod", "-y", out};
}
inline std::vector<std::string> build(const std::string& in, const std::string& out,
                                      const std::string& ext) {
    if (ext == "mp3") return toMp3(in, out);
    if (ext == "aac") return toAac(in, out);
    if (ext == "flac") return toFlac(in, out);
    if (ext == "wav") return toWav(in, out);
    if (ext == "ogg" || ext == "oga") return toOgg(in, out);
    if (ext == "opus") return toOpus(in, out);
    if (ext == "m4a") return toM4a(in, out);
    if (ext == "alac") return toAlac(in, out);
    return toMp3(in, out);
}
}  // namespace audio_out
}  // namespace convert
