#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_gif {
inline std::vector<std::string> gifToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> gifToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> gifToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> gifToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> gifToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> gifToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> gifToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> gifToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> gifToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> gifToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> gifToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromGif(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return gifToJpg(in, out);
    if (ext == "png") return gifToPng(in, out);
    if (ext == "webp") return gifToWebp(in, out);
    if (ext == "avif") return gifToAvif(in, out, pixFmt);
    if (ext == "bmp") return gifToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return gifToTiff(in, out);
    if (ext == "tga") return gifToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return gifToJp2(in, out);
    if (ext == "qoi") return gifToQoi(in, out);
    if (ext == "dpx") return gifToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return gifToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_gif
}  // namespace convert
