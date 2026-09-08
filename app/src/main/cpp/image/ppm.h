#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_ppm {
inline std::vector<std::string> ppmToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> ppmToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> ppmToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> ppmToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> ppmToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> ppmToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> ppmToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> ppmToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> ppmToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> ppmToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> ppmToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}

inline std::vector<std::string> buildFromPpm(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return ppmToJpg(in, out);
    if (ext == "png") return ppmToPng(in, out);
    if (ext == "webp") return ppmToWebp(in, out);
    if (ext == "avif") return ppmToAvif(in, out, pixFmt);
    if (ext == "bmp") return ppmToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return ppmToTiff(in, out);
    if (ext == "gif") return ppmToGif(in, out);
    if (ext == "tga") return ppmToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return ppmToJp2(in, out);
    if (ext == "qoi") return ppmToQoi(in, out);
    if (ext == "dpx") return ppmToDpx(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_ppm
}  // namespace convert
