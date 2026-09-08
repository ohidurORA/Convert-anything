#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_qoi {
inline std::vector<std::string> qoiToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> qoiToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> qoiToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> qoiToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> qoiToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> qoiToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> qoiToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> qoiToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> qoiToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> qoiToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> qoiToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromQoi(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return qoiToJpg(in, out);
    if (ext == "png") return qoiToPng(in, out);
    if (ext == "webp") return qoiToWebp(in, out);
    if (ext == "avif") return qoiToAvif(in, out, pixFmt);
    if (ext == "bmp") return qoiToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return qoiToTiff(in, out);
    if (ext == "gif") return qoiToGif(in, out);
    if (ext == "tga") return qoiToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return qoiToJp2(in, out);
    if (ext == "dpx") return qoiToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return qoiToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_qoi
}  // namespace convert
