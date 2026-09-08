#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_dpx {
inline std::vector<std::string> dpxToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> dpxToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> dpxToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> dpxToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> dpxToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> dpxToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> dpxToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> dpxToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> dpxToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> dpxToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> dpxToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromDpx(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return dpxToJpg(in, out);
    if (ext == "png") return dpxToPng(in, out);
    if (ext == "webp") return dpxToWebp(in, out);
    if (ext == "avif") return dpxToAvif(in, out, pixFmt);
    if (ext == "bmp") return dpxToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return dpxToTiff(in, out);
    if (ext == "gif") return dpxToGif(in, out);
    if (ext == "tga") return dpxToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return dpxToJp2(in, out);
    if (ext == "qoi") return dpxToQoi(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return dpxToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_dpx
}  // namespace convert
