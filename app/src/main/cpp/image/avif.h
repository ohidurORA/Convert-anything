#pragma once
#include <string>
#include <vector>

#include "out.h"

namespace convert {
namespace image_avif {
inline std::vector<std::string> avifToJpg(const std::string& in, const std::string& out) {
    return image_out::toJpg(in, out);
}
inline std::vector<std::string> avifToPng(const std::string& in, const std::string& out) {
    return image_out::toPng(in, out);
}
inline std::vector<std::string> avifToWebp(const std::string& in, const std::string& out) {
    return image_out::toWebp(in, out);
}
inline std::vector<std::string> avifToBmp(const std::string& in, const std::string& out) {
    return image_out::toBmp(in, out);
}
inline std::vector<std::string> avifToTiff(const std::string& in, const std::string& out) {
    return image_out::toTiff(in, out);
}
inline std::vector<std::string> avifToGif(const std::string& in, const std::string& out) {
    return image_out::toGif(in, out);
}
inline std::vector<std::string> avifToTga(const std::string& in, const std::string& out) {
    return image_out::toTga(in, out);
}
inline std::vector<std::string> avifToJp2(const std::string& in, const std::string& out) {
    return image_out::toJp2(in, out);
}
inline std::vector<std::string> avifToQoi(const std::string& in, const std::string& out) {
    return image_out::toQoi(in, out);
}
inline std::vector<std::string> avifToDpx(const std::string& in, const std::string& out) {
    return image_out::toDpx(in, out);
}
inline std::vector<std::string> avifToPpm(const std::string& in, const std::string& out) {
    return image_out::toPpm(in, out);
}

inline std::vector<std::string> buildFromAvif(const std::string& in, const std::string& out,
                                              const std::string& ext, const std::string& pixFmt) {
    if (ext == "jpg" || ext == "jpeg") return avifToJpg(in, out);
    if (ext == "png") return avifToPng(in, out);
    if (ext == "webp") return avifToWebp(in, out);
    if (ext == "bmp") return avifToBmp(in, out);
    if (ext == "tiff" || ext == "tif") return avifToTiff(in, out);
    if (ext == "gif") return avifToGif(in, out);
    if (ext == "tga") return avifToTga(in, out);
    if (ext == "jp2" || ext == "j2k") return avifToJp2(in, out);
    if (ext == "qoi") return avifToQoi(in, out);
    if (ext == "dpx") return avifToDpx(in, out);
    if (ext == "ppm" || ext == "pnm" || ext == "pgm") return avifToPpm(in, out);
    return image_out::build(in, out, ext, pixFmt);
}
}  // namespace image_avif
}  // namespace convert
