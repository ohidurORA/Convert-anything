#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_tiff {
inline std::vector<std::string> tiffToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> tiffToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> tiffToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> tiffToAvif(const std::string& in, const std::string& out,
                                           const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> tiffToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> tiffToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> tiffToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> tiffToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> tiffToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> tiffToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> tiffToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromTiff(const std::string& in, const std::string& out,
                                              const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return tiffToJpg(in, out);
    if (ext == "png") return tiffToPng(in, out);
    if (ext == "webp") return tiffToWebp(in, out);
    if (ext == "avif") return tiffToAvif(in, out, pixFmt);
    if (ext == "bmp") return tiffToBmp(in, out);
    if (ext == "gif") return tiffToGif(in, out);
    if (ext == "tga") return tiffToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return tiffToJp2(in, out);
    if (ext == "qoi") return tiffToQoi(in, out);
    if (ext == "dpx") return tiffToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return tiffToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_tiff
}  // namespace convert
