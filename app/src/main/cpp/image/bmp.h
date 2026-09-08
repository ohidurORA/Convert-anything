#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_bmp {
inline std::vector<std::string> bmpToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> bmpToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> bmpToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> bmpToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> bmpToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> bmpToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> bmpToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> bmpToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> bmpToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> bmpToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> bmpToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromBmp(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return bmpToJpg(in, out);
    if (ext == "png") return bmpToPng(in, out);
    if (ext == "webp") return bmpToWebp(in, out);
    if (ext == "avif") return bmpToAvif(in, out, pixFmt);
    if (ext == "tiff" || ext == "tif") return bmpToTiff(in, out);
    if (ext == "gif") return bmpToGif(in, out);
    if (ext == "tga") return bmpToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return bmpToJp2(in, out);
    if (ext == "qoi") return bmpToQoi(in, out);
    if (ext == "dpx") return bmpToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return bmpToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_bmp
}  // namespace convert
