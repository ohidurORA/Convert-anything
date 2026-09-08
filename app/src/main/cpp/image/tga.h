#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_tga {
inline std::vector<std::string> tgaToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> tgaToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> tgaToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> tgaToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> tgaToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> tgaToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> tgaToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> tgaToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> tgaToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> tgaToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> tgaToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromTga(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return tgaToJpg(in, out);
    if (ext == "png") return tgaToPng(in, out);
    if (ext == "webp") return tgaToWebp(in, out);
    if (ext == "avif") return tgaToAvif(in, out, pixFmt);
    if (ext == "bmp") return tgaToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return tgaToTiff(in, out);
    if (ext == "gif") return tgaToGif(in, out);
    if (ext == "jp2" || ext == "j2k") return tgaToJp2(in, out);
    if (ext == "qoi") return tgaToQoi(in, out);
    if (ext == "dpx") return tgaToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return tgaToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_tga
}  // namespace convert
