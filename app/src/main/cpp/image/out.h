#pragma once
#include <string>
#include <vector>

namespace convert {
namespace image_out {
// One builder per OUTPUT format. Never pass image2-only flags (-update)
// to dedicated muxers (dpx, jp2, qoi, avif): that is why pairs like
// PPM→DPX failed even though a builder existed.
inline std::vector<std::string> toJpg(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1",
            "-vf", "format=yuvj420p", "-c:v", "mjpeg", "-q:v", "2", "-y", out};
}
inline std::vector<std::string> toPng(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "png", "-y", out};
}
inline std::vector<std::string> toWebp(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-c:v", "libwebp", "-quality", "90",
            "-frames:v", "1", "-y", out};
}
inline std::vector<std::string> toAvif(const std::string& in, const std::string& out,
                                       const std::string& pixFmt) {
    if (pixFmt.rfind("gray", 0) == 0 || pixFmt.rfind("mono", 0) == 0)
        return {"-i", in, "-an", "-sn", "-dn", "-c:v", "libaom-av1", "-crf", "28",
                "-vf", "format=yuv420p", "-frames:v", "1", "-y", out};
    return {"-i", in, "-an", "-sn", "-dn", "-c:v", "libaom-av1", "-crf", "28",
            "-vf", "format=yuv420p", "-frames:v", "1", "-y", out};
}
inline std::vector<std::string> toBmp(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "bmp", "-y", out};
}
inline std::vector<std::string> toTiff(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "tiff", "-y", out};
}
inline std::vector<std::string> toGif(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-lavfi",
            "scale=trunc(iw/2)*2:trunc(ih/2)*2,split[s0][s1];[s0]palettegen=reserve_transparent=on:stats_mode=single[p];[s1][p]paletteuse=dither=bayer",
            "-y", out};
}
inline std::vector<std::string> toTga(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "targa", "-y", out};
}
inline std::vector<std::string> toJp2(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "jpeg2000",
            "-pix_fmt", "yuv422p", "-strict", "-2", "-y", out};
}
inline std::vector<std::string> toQoi(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "qoi",
            "-pix_fmt", "rgba", "-y", out};
}
inline std::vector<std::string> toDpx(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "dpx",
            "-pix_fmt", "gbrp10le", "-y", out};
}
inline std::vector<std::string> toPpm(const std::string& in, const std::string& out) {
    return {"-i", in, "-an", "-sn", "-dn", "-frames:v", "1", "-c:v", "ppm",
            "-pix_fmt", "rgb24", "-y", out};
}

inline std::vector<std::string> build(const std::string& in, const std::string& out,
                                      const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return toJpg(in, out);
    if (ext == "png") return toPng(in, out);
    if (ext == "webp") return toWebp(in, out);
    if (ext == "avif") return toAvif(in, out, pixFmt);
    if (ext == "bmp") return toBmp(in, out);
    if (ext == "tiff" || ext == "tif") return toTiff(in, out);
    if (ext == "gif") return toGif(in, out);
    if (ext == "tga") return toTga(in, out);
    if (ext == "jp2" || ext == "j2k") return toJp2(in, out);
    if (ext == "qoi") return toQoi(in, out);
    if (ext == "dpx") return toDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return toPpm(in, out);
    return toPng(in, out);
}
}  // namespace image_out
}  // namespace convert
