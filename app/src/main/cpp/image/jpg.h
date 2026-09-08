#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_jpg {
inline std::vector<std::string> jpgToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> jpgToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> jpgToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> jpgToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> jpgToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> jpgToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> jpgToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> jpgToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> jpgToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> jpgToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> jpgToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromJpg(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "png") return jpgToPng(in, out);
    if (ext == "webp") return jpgToWebp(in, out);
    if (ext == "avif") return jpgToAvif(in, out, pixFmt);
    if (ext == "bmp") return jpgToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return jpgToTiff(in, out);
    if (ext == "gif") return jpgToGif(in, out);
    if (ext == "tga") return jpgToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return jpgToJp2(in, out);
    if (ext == "qoi") return jpgToQoi(in, out);
    if (ext == "dpx") return jpgToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return jpgToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_jpg
}  // namespace convert
