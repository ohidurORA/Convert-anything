#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_jp2 {
inline std::vector<std::string> jp2ToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> jp2ToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> jp2ToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> jp2ToAvif(const std::string& in, const std::string& out,
                                          const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> jp2ToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> jp2ToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> jp2ToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> jp2ToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> jp2ToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> jp2ToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> jp2ToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromJp2(const std::string& in, const std::string& out,
                                             const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return jp2ToJpg(in, out);
    if (ext == "png") return jp2ToPng(in, out);
    if (ext == "webp") return jp2ToWebp(in, out);
    if (ext == "avif") return jp2ToAvif(in, out, pixFmt);
    if (ext == "bmp") return jp2ToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return jp2ToTiff(in, out);
    if (ext == "gif") return jp2ToGif(in, out);
    if (ext == "tga") return jp2ToTga(in, out);
    if (ext == "qoi") return jp2ToQoi(in, out);
    if (ext == "dpx") return jp2ToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return jp2ToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_jp2
}  // namespace convert
