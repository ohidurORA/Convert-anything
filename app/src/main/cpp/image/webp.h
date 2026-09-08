#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_webp {
inline std::vector<std::string> webpToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> webpToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> webpToAvif(const std::string& in, const std::string& out,
                                           const std::string& pixFmt) {
    return image_out::toAvif(in, out, pixFmt);
}
inline std::vector<std::string> webpToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> webpToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> webpToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> webpToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> webpToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> webpToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> webpToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> webpToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromWebp(const std::string& in, const std::string& out,
                                              const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return webpToJpg(in, out);
    if (ext == "png") return webpToPng(in, out);
    if (ext == "avif") return webpToAvif(in, out, pixFmt);
    if (ext == "bmp") return webpToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return webpToTiff(in, out);
    if (ext == "gif") return webpToGif(in, out);
    if (ext == "tga") return webpToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return webpToJp2(in, out);
    if (ext == "qoi") return webpToQoi(in, out);
    if (ext == "dpx") return webpToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return webpToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_webp
}  // namespace convert
