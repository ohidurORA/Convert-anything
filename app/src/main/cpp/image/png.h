#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_png {
inline std::vector<std::string> pngToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> pngToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> pngToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> pngToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> pngToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> pngToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> pngToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> pngToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> pngToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> pngToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> pngToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromPng(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return pngToJpg(in, out);
    if (ext == "webp") return pngToWebp(in, out);
    if (ext == "avif") return pngToAvif(in, out, pixFmt);
    if (ext == "bmp") return pngToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return pngToTiff(in, out);
    if (ext == "gif") return pngToGif(in, out);
    if (ext == "tga") return pngToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return pngToJp2(in, out);
    if (ext == "qoi") return pngToQoi(in, out);
    if (ext == "dpx") return pngToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return pngToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_png
}  // namespace convert
