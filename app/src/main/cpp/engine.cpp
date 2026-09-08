// Convert Anything — C++ engine implementation.
#include "engine.h"

#include <atomic>
#include <algorithm>
#include <chrono>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <sstream>
#include <sys/stat.h>
#include <unistd.h>

#ifdef CONVERT_ANDROID
extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/pixdesc.h"
#include "libswscale/swscale.h"
}
#include <android/bitmap.h>
extern "C" int ffmpeg_main(int argc, char** argv);
#else
// Host validation harness: builders only, no libav link.
extern "C" int ffmpeg_main(int argc, char** argv) { (void)argc; (void)argv; return -1; }
#endif

// ---- FFmpeg argv per type (-y + -progress pipe:1 ready for real linkage) ----
// Per-input-format files: one function per (input, output) pair. Each pair
// function delegates to the single per-output truth in out.h — the input
// container needs no special handling (ffmpeg auto-detects the input codec).
#include "audio/mp3.h"
#include "audio/aac.h"
#include "audio/flac.h"
#include "audio/wav.h"
#include "audio/ogg.h"
#include "audio/opus.h"
#include "audio/m4a.h"
#include "audio/alac.h"
#include "image/jpg.h"
#include "image/png.h"
#include "image/webp.h"
#include "image/avif.h"
#include "image/bmp.h"
#include "image/tiff.h"
#include "image/gif.h"
#include "image/tga.h"
#include "image/jp2.h"
#include "image/qoi.h"
#include "image/dpx.h"
#include "image/ppm.h"
#include "video/mp4.h"
#include "video/mkv.h"
#include "video/webm.h"
#include "video/mov.h"
#include "video/avi.h"
#include "video/flv.h"
#include "video/mpeg.h"
#include "video/hevc.h"
#include "video_to_audio/mp4.h"
#include "video_to_audio/mkv.h"
#include "video_to_audio/webm.h"
#include "video_to_audio/mov.h"
#include "video_to_audio/avi.h"
#include "video_to_audio/flv.h"
#include "video_to_audio/mpeg.h"
#include "video_to_audio/hevc.h"

namespace convert {

Engine& Engine::instance() {
    static Engine e;
    return e;
}

Engine::Engine() : worker_(&Engine::loop, this) {}
Engine::~Engine() {
    { std::lock_guard<std::mutex> l(m_); stop_ = true; }
    cv_.notify_all();
    if (worker_.joinable()) worker_.join();
}

// ---- Registry: the 4 launch tiles. Documents etc. = append here. ----
std::vector<TypeInfo> Engine::types() const {
    const std::vector<FormatInfo> video = {
        {"mp4", "MP4 · H.264/AAC", "video/mp4", "Universal playback — phones, web, TV."},
        {"mkv", "MKV · H.264/AAC", "video/x-matroska", "Multi-track container — subs and many audio tracks."},
        {"webm", "WebM · VP9/Opus", "video/webm", "Web streaming — royalty-free VP9/Opus."},
        {"mov", "MOV · H.264/AAC", "video/quicktime", "Apple workflows — QuickTime and Final Cut."},
        {"avi", "AVI", "video/x-msvideo", "Legacy devices — maximum compatibility."},
        {"flv", "FLV", "video/x-flv", "Old web players — legacy Flash video."},
        {"mpeg", "MPEG", "video/mpeg", "DVD and broadcast MPEG-2."},
        {"hevc", "HEVC · H.265", "video/mp4", "Half the size of H.264 — best for 4K archiving."},
    };
    const std::vector<FormatInfo> audio = {
        {"mp3", "MP3 · 320k", "audio/mpeg", "Universal audio — plays everywhere."},
        {"aac", "AAC · 256k", "audio/mp4", "Better than MP3 at the same size — streaming standard."},
        {"flac", "FLAC · Lossless", "audio/flac", "Bit-perfect archive — studio masters."},
        {"wav", "WAV · PCM", "audio/x-wav", "Uncompressed audio — editing and production."},
        {"ogg", "OGG · Vorbis", "audio/ogg", "Open format — efficient streaming."},
        {"opus", "OPUS", "audio/opus", "Best quality for voice and low bitrates."},
        {"m4a", "M4A · AAC", "audio/mp4", "Apple AAC container — iPhone and iTunes."},
        {"alac", "ALAC · Lossless", "audio/mp4", "Apple lossless — audiophile archiving."},
    };
    const std::vector<FormatInfo> image = {
        {"jpg", "JPEG · Universal", "image/jpeg", "Small universal photos — sharing and web."},
        {"png", "PNG · Lossless", "image/png", "Lossless plus transparency — graphics and screenshots."},
        {"webp", "WebP · Optimal", "image/webp", "Modern web images — much smaller than JPEG."},
        {"avif", "AVIF · HDR", "image/avif", "Best compression available — HDR photos."},
        {"bmp", "BMP", "image/bmp", "Uncompressed bitmap — legacy compatibility."},
        {"tiff", "TIFF", "image/tiff", "Print and pro archive — lossless quality."},
        {"gif", "GIF", "image/gif", "Simple animation — memes and quick sharing."},
        {"tga", "TGA · Truevision", "image/x-tga", "Game and video textures."},
        {"jp2", "JPEG 2000", "image/jp2", "Archival wavelet codec — medical and libraries."},
        {"qoi", "QOI · Fast", "image/qoi", "Blazing-fast lossless — developers and quick saves."},
        {"dpx", "DPX · Cineon", "image/x-dpx", "Film scans — cinema VFX pipelines."},
        {"ppm", "PPM · Portable", "image/x-portable-pixmap", "Simplest raw pixels — tooling and interchange."},
    };
    // NOTE: no HEIC output — FFmpeg has no HEIC muxer (validated against real FFmpeg).
    return {
        {"video", "Video Conversion", "video", "video", video},
        {"audio", "Audio Conversion", "audio", "audio", audio},
        {"image", "Image Conversion", "image", "images", image},
        {"video_to_audio", "Video to Audio", "video", "audio", audio},
    };
}

// Lowercase extension of a display name ("song.MP3" -> "mp3", "" when none).
static std::string extOf(const std::string& name) {
    auto p = name.rfind('.');
    if (p == std::string::npos || p + 1 >= name.size()) return "";
    std::string e = name.substr(p + 1);
    for (auto& c : e) c = static_cast<char>(tolower(static_cast<unsigned char>(c)));
    return e;
}

// Fold aliases onto the registry id used by per-input builders. Without this,
// "photo.jpeg" / "scan.tif" / "clip.mpg" fall through to the generic builder
// and skip the pair function that actually knows the destination encoder.
static std::string canonicalInputExt(const std::string& e) {
    if (e == "jpeg" || e == "jpe") return "jpg";
    if (e == "tif") return "tiff";
    if (e == "pnm" || e == "pgm" || e == "pbm") return "ppm";
    if (e == "j2k" || e == "jpx" || e == "jpf") return "jp2";
    if (e == "mpg" || e == "m2v" || e == "mpeg2" || e == "m2ts" || e == "ts") return "mpeg";
    if (e == "oga") return "ogg";
    if (e == "m4v" || e == "3gp" || e == "3gpp") return "mp4";
    if (e == "adts") return "aac";
    if (e == "weba") return "webm";
    return e;
}

// Single source of truth for the *container* extension a format's muxer
// actually produces on disk. Most formats' container matches their advertised
// extension; two do not:
//   - ALAC is a codec with no dedicated muxer; it ships inside an .m4a (ipod)
//     container. Kotlin must name/publish the temp file ".m4a", not ".alac".
//   - The HEVC builder writes "hvc1" into an .mp4 container. Kotlin must name/
//     publish the temp file ".mp4", not ".hevc".
// Both the job output-path normalization (enqueue) and the JSON registry
// serialization (typesJson) derive from this one function so the frontend and
// the engine can never disagree about the real on-disk extension.
static std::string containerOf(const std::string& ext) {
    if (ext == "alac") return "m4a";
    if (ext == "hevc") return "mp4";
    return ext;
}

std::vector<std::string> Engine::buildArgs(const std::string& typeId, const std::string& in,
                                           const std::string& out, const std::string& ext,
                                           const std::string& pixFmt, const std::string& inputExt) {
    const std::string inExt = canonicalInputExt(inputExt);
    const std::string outExt = ext;
    if (typeId == "video") {
        if (inExt == "mp4") return video_mp4::buildFromMp4(in, out, outExt);
        if (inExt == "mkv") return video_mkv::buildFromMkv(in, out, outExt);
        if (inExt == "webm") return video_webm::buildFromWebm(in, out, outExt);
        if (inExt == "mov") return video_mov::buildFromMov(in, out, outExt);
        if (inExt == "avi") return video_avi::buildFromAvi(in, out, outExt);
        if (inExt == "flv") return video_flv::buildFromFlv(in, out, outExt);
        if (inExt == "mpeg") return video_mpeg::buildFromMpeg(in, out, outExt);
        if (inExt == "hevc") return video_hevc::buildFromHevc(in, out, outExt);
        return video_out::build(in, out, outExt);
    }
    if (typeId == "audio" || typeId == "video_to_audio") {
        if (typeId == "video_to_audio") {
            if (inExt == "mp4") return v2a_mp4::buildFromMp4(in, out, outExt);
            if (inExt == "mkv") return v2a_mkv::buildFromMkv(in, out, outExt);
            if (inExt == "webm") return v2a_webm::buildFromWebm(in, out, outExt);
            if (inExt == "mov") return v2a_mov::buildFromMov(in, out, outExt);
            if (inExt == "avi") return v2a_avi::buildFromAvi(in, out, outExt);
            if (inExt == "flv") return v2a_flv::buildFromFlv(in, out, outExt);
            if (inExt == "mpeg") return v2a_mpeg::buildFromMpeg(in, out, outExt);
            if (inExt == "hevc") return v2a_hevc::buildFromHevc(in, out, outExt);
            return audio_out::build(in, out, outExt);
        }
        if (inExt == "mp3") return audio_mp3::buildFromMp3(in, out, outExt);
        if (inExt == "aac") return audio_aac::buildFromAac(in, out, outExt);
        if (inExt == "flac") return audio_flac::buildFromFlac(in, out, outExt);
        if (inExt == "wav") return audio_wav::buildFromWav(in, out, outExt);
        if (inExt == "ogg") return audio_ogg::buildFromOgg(in, out, outExt);
        if (inExt == "opus") return audio_opus::buildFromOpus(in, out, outExt);
        if (inExt == "m4a") return audio_m4a::buildFromM4a(in, out, outExt);
        if (inExt == "alac") return audio_alac::buildFromAlac(in, out, outExt);
        return audio_out::build(in, out, outExt);
    }
    if (typeId == "image") {
        if (inExt == "jpg") return image_jpg::buildFromJpg(in, out, outExt, pixFmt);
        if (inExt == "png") return image_png::buildFromPng(in, out, outExt, pixFmt);
        if (inExt == "webp") return image_webp::buildFromWebp(in, out, outExt, pixFmt);
        if (inExt == "avif") return image_avif::buildFromAvif(in, out, outExt, pixFmt);
        if (inExt == "bmp") return image_bmp::buildFromBmp(in, out, outExt, pixFmt);
        if (inExt == "tiff" || inExt == "tif") return image_tiff::buildFromTiff(in, out, outExt, pixFmt);
        if (inExt == "gif") return image_gif::buildFromGif(in, out, outExt, pixFmt);
        if (inExt == "tga") return image_tga::buildFromTga(in, out, outExt, pixFmt);
        if (inExt == "jp2") return image_jp2::buildFromJp2(in, out, outExt, pixFmt);
        if (inExt == "qoi") return image_qoi::buildFromQoi(in, out, outExt, pixFmt);
        if (inExt == "dpx") return image_dpx::buildFromDpx(in, out, outExt, pixFmt);
        if (inExt == "ppm") return image_ppm::buildFromPpm(in, out, outExt, pixFmt);
        return image_out::build(in, out, outExt, pixFmt);
    }
    return {"-i", in, "-y", out};
}

// ---- Queue ----
std::string Engine::enqueue(const std::string& typeId, const std::string& in,
                            const std::string& out, const std::string& ext,
                            const std::string& batchId, const std::string& uri,
                            const std::string& name, const std::string& mime,
                            const std::string& workDir) {
    std::string id;
    {
        std::lock_guard<std::mutex> l(m_);
        id = "job-" + std::to_string(nextId_++);
        Job j;
        j.id = id;
        j.batchId = batchId.empty() ? id : batchId;
        j.typeId = typeId; j.inputPath = in;
        // Normalize the output path to the real container extension (see
        // containerOf). ALAC/HEVC advertise an extension the muxer doesn't
        // produce (.alac/.hevc have no muxer), so a Kotlin-supplied path that
        // still ends in the advertised extension is rewritten to the container
        // the ffmpeg argv actually writes (.m4a/.mp4). This must mirror the
        // "container" field the registry exposes to Kotlin exactly.
        const std::string container = containerOf(ext);
        const std::string dotExt = "." + ext;
        if (container != ext && out.size() > dotExt.size() &&
            out.compare(out.size() - dotExt.size(), dotExt.size(), dotExt) == 0)
            j.outputPath = out.substr(0, out.size() - dotExt.size()) + "." + container;
        else
            j.outputPath = out;
        j.formatExt = ext;
        j.sourceUri = uri; j.sourceName = name; j.mimeType = mime;
        j.workDir = workDir;
        jobs_.push_back(std::move(j));
    }
    cv_.notify_one();
    notify();
    return id;
}

void Engine::cancel(const std::string& jobId) {
    // notify() re-locks via snapshotJson: it must run UNLOCKED or this
    // self-deadlocks (killed finalize collection / ANR — "works once, then nothing").
    {
        std::lock_guard<std::mutex> l(m_);
        for (auto& j : jobs_)
            if (j.id == jobId && (j.status == "queued" || j.status == "running")) {
                j.cancelRequested = true;
                if (j.status == "queued") { j.status = "failed"; j.error = "Cancelled"; }
            }
    }
    notify();
}

void Engine::clearDone() {
    {
        std::lock_guard<std::mutex> l(m_);
        for (auto it = jobs_.begin(); it != jobs_.end();) {
            if (it->status == "done" || it->status == "failed") it = jobs_.erase(it);
            else ++it;
        }
    }
    notify();
}

void Engine::remove(const std::string& jobId) {
    {
        std::lock_guard<std::mutex> l(m_);
        for (auto it = jobs_.begin(); it != jobs_.end();) {
            if (it->id == jobId && it->status != "running") it = jobs_.erase(it);
            else ++it;
        }
    }
    notify();
}

void Engine::setListener(std::function<void(const std::string&)> cb) {
    std::lock_guard<std::mutex> l(m_);
    listener_ = std::move(cb);
}

void Engine::notify() {
    std::function<void(const std::string&)> cb;
    std::string snap;
    { std::lock_guard<std::mutex> l(m_); cb = listener_; }
    if (cb) { snap = snapshotJson(); cb(snap); }
}

void Engine::loop() {
    auto hasQueued = [&] {
        return std::any_of(jobs_.begin(), jobs_.end(),
                           [](const Job& j) { return j.status == "queued"; });
    };
    for (;;) {
        std::string currentId;
        std::string inputPath;
        std::string typeId;
        {
            std::unique_lock<std::mutex> l(m_);
            cv_.wait(l, [&] { return stop_ || hasQueued(); });
            if (stop_) return;
            for (auto& j : jobs_) {
                if (j.status == "queued") {
                    j.status = "running";
                    currentId = j.id;
                    inputPath = j.inputPath;
                    typeId = j.typeId;
                    break;
                }
            }
            if (currentId.empty()) continue;
        }
        notify();

        // Fail fast on unreadable or incompatible input instead of feeding ffmpeg garbage.
        Probe pre = probeInput(inputPath);
        bool inputValid = true;
        std::string inputError;
        if (!pre.hasVideo && !pre.hasAudio) {
            inputValid = false;
            inputError = "Unsupported or unreadable input";
        } else if ((typeId == "audio" || typeId == "video_to_audio") && !pre.hasAudio) {
            inputValid = false;
            inputError = "Input file contains no audio track";
        } else if (typeId == "video" && !pre.hasVideo) {
            inputValid = false;
            inputError = "Input file contains no video track";
        } else if (typeId == "image" && !pre.hasVideo) {
            inputValid = false;
            inputError = "Input file is not a valid image";
        }

        if (!inputValid) {
            {
                std::lock_guard<std::mutex> l(m_);
                for (auto& j : jobs_) {
                    if (j.id == currentId) {
                        j.status = "failed";
                        j.error = inputError;
                        j.progress = 0;
                        break;
                    }
                }
            }
            notify();
            continue;
        }

        Job* currentJob = nullptr;
        {
            std::lock_guard<std::mutex> l(m_);
            for (auto& j : jobs_) {
                if (j.id == currentId) { currentJob = &j; break; }
            }
        }
        if (!currentJob) continue;

        int rc = runTranscode(*currentJob);
        {
            std::lock_guard<std::mutex> l(m_);
            for (auto& j : jobs_) {
                if (j.id == currentId) {
                    if (j.cancelRequested && j.status == "running") {
                        j.status = "failed"; j.error = "Cancelled";
                    } else if (j.status == "running") {
                        if (rc == 0) {
                            if (validateOutputFile(j.outputPath, j.typeId)) {
                                j.status = "done";
                                j.progress = 100;
                            } else {
                                j.status = "failed";
                                j.error = "Output file is empty or invalid";
                            }
                        } else if (j.error.empty()) {
                            j.status = "failed";
                            j.error = "ffmpeg exited with " + std::to_string(rc);
                        } else {
                            j.status = "failed";  // runTranscode already set stderr text
                        }
                    }
                    break;
                }
            }
        }
        notify();
    }
}

// Blocking ffmpeg invocation used by the :converter sandbox process.
// Runs on the caller thread; a watcher thread forwards cancelFlag as 'q' on
// stdin. Progress/stderr files are shared storage both processes see.
// Per-job cancel: the converter stores a map of cancel flag pointers keyed by
// job ID; nativeCancelFfmpeg(jobId) sets the right one.
std::map<std::string, std::atomic<bool>*> g_cancelFlags;
std::mutex g_cancelMtx;

void cancelCurrentTranscode(const std::string& jobId) {
    std::lock_guard<std::mutex> l(g_cancelMtx);
    auto it = g_cancelFlags.find(jobId);
    if (it != g_cancelFlags.end() && it->second) it->second->store(true);
}

int runFfmpegBlocking(const std::vector<std::string>& args,
                      const std::string& progressPath,
                      const std::string& stderrPath,
                      std::atomic<bool>* cancelFlag) {
    // ffmpeg_main uses process-wide CLI state. Never overlap two runs in
    // one process — that is the "first file works, the rest of the batch fail"
    // failure mode when the sandbox is reused.
    static std::mutex ffmpegMtx;
    std::lock_guard<std::mutex> ffmpegLock(ffmpegMtx);
    if (cancelFlag) cancelFlag->store(false);
    std::vector<std::string> owned;
    owned.reserve(args.size() + 4);
    owned.emplace_back("ffmpeg");
    owned.emplace_back("-v"); owned.emplace_back("error");
    owned.emplace_back("-progress"); owned.emplace_back(progressPath);
    owned.insert(owned.end(), args.begin(), args.end());
    std::vector<char*> argv;
    for (auto& s : owned) argv.push_back(s.data());
    argv.push_back(nullptr);

    int qpipe[2] = {-1, -1};
    int savedStdin = -1, savedStderr = -1;
    if (pipe(qpipe) == 0) {
        savedStdin = dup(STDIN_FILENO);
        dup2(qpipe[0], STDIN_FILENO);
        close(qpipe[0]);
    }
    int errFd = open(stderrPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0600);
    if (errFd >= 0) {
        savedStderr = dup(STDERR_FILENO);
        dup2(errFd, STDERR_FILENO);
        close(errFd);
    }

    std::atomic<bool> finished{false};
    std::thread watcher([&] {
        while (!finished) {
            if (cancelFlag && cancelFlag->load()) {
                if (qpipe[1] >= 0) (void)write(qpipe[1], "q", 1);
                return;
            }
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
        }
    });
    int rc = ffmpeg_main(static_cast<int>(owned.size()), argv.data());
    finished = true;
    watcher.join();

    if (savedStdin >= 0) { dup2(savedStdin, STDIN_FILENO); close(savedStdin); }
    if (qpipe[1] >= 0) close(qpipe[1]);
    if (savedStderr >= 0) { dup2(savedStderr, STDERR_FILENO); close(savedStderr); }
    return rc;
}

// Main-process side: dispatch to the sandbox, then poll the shared progress
// file until the result lands via onRemoteFinished (reply or death notice).
int Engine::runTranscode(Job& job) {
    const std::string progressPath = job.workDir + "/" + job.id + ".progress";
    const std::string stderrPath = job.workDir + "/" + job.id + ".stderr";

    const Probe probe = probeInput(job.inputPath);  // duration for % math (0 for images)

    if (!dispatchRemote(job.id, job.typeId, job.formatExt, job.outputPath,
                         progressPath, stderrPath, extOf(job.sourceName), job.inputPath)) {
        std::lock_guard<std::mutex> l(m_);
        job.error = "Converter unavailable";
        return -100;
    }

    const auto start = std::chrono::steady_clock::now();
    bool cancelSent = false;
    // Watchdog: frozen progress means a wedged native run burning CPU forever.
    // Stills are exempt by construction (their % ramps on elapsed time).
    // ponytail: fixed 30 min ceiling; per-duration budgets if this ever misfires.
    int lastProg = -1;
    auto lastChange = start;
    bool killed = false;
    for (;;) {
        {
            std::lock_guard<std::mutex> l(m_);
            if (job.cancelRequested && !cancelSent) {
                bridge::cancelRemote(job.id);
                cancelSent = true;
            }
        }
        int rc = 0;
        std::string error;
        if (takeRemoteResult(job.id, rc, error)) {
            std::lock_guard<std::mutex> l(m_);
            job.progress = (rc == 0) ? 100 : job.progress;
            if (rc != 0 && !job.cancelRequested) {
                job.error = !error.empty() ? error : lastStderrLines(stderrPath);
            }
            std::remove(progressPath.c_str());
            std::remove(stderrPath.c_str());
            return rc;
        }
        {
            std::lock_guard<std::mutex> l(m_);
            job.progress = readProgress(progressPath, probe.durationUs, start);
            if (job.progress != lastProg) {
                lastProg = job.progress;
                lastChange = std::chrono::steady_clock::now();
            } else if (!killed && std::chrono::steady_clock::now() - lastChange >
                                 std::chrono::minutes(30)) {
                bridge::killConverter();  // death notice completes the job as failed
                killed = true;
            }
        }
        notify();
        std::this_thread::sleep_for(std::chrono::milliseconds(120));
    }
}

bool Engine::dispatchRemote(const std::string& id, const std::string& typeId,
                            const std::string& formatExt, const std::string& outputPath,
                            const std::string& progressPath, const std::string& stderrPath,
                            const std::string& inputExt, const std::string& inputPath) {
    return bridge::dispatchRemote(id, typeId, formatExt, outputPath,
                                  progressPath, stderrPath, inputExt, inputPath);
}

void Engine::onRemoteFinished(const std::string& id, int rc, const std::string& error) {
    std::lock_guard<std::mutex> l(m_);
    remote_[id] = RemoteResult{true, rc, error};
}

bool Engine::takeRemoteResult(const std::string& id, int& rc, std::string& error) {
    std::lock_guard<std::mutex> l(m_);
    auto it = remote_.find(id);
    if (it == remote_.end() || !it->second.ready) return false;
    rc = it->second.rc;
    error = it->second.error;
    remote_.erase(it);
    return true;
}

/** Last progress percent from ffmpeg's -progress file (out_time_us vs duration). */
int Engine::readProgress(const std::string& path, int64_t durationUs,
                         const std::chrono::steady_clock::time_point& start) {
    FILE* f = std::fopen(path.c_str(), "r");
    int64_t outUs = -1;
    bool ended = false;
    if (f) {
        char line[256];
        while (std::fgets(line, sizeof(line), f)) {
            if (std::strncmp(line, "out_time_ms=", 12) == 0) outUs = std::atoll(line + 12);
            else if (std::strncmp(line, "progress=end", 12) == 0) ended = true;
        }
        std::fclose(f);
    }
    if (ended) return 100;
    if (durationUs > 0 && outUs >= 0)
        return static_cast<int>(std::min<int64_t>(99, outUs * 100 / durationUs));
    // Still images etc: ramp on elapsed time so the bar never looks dead.
    const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - start).count();
    return static_cast<int>(std::min<int64_t>(95, ms / 40));
}

std::string Engine::lastStderrLines(const std::string& path) {
    FILE* f = std::fopen(path.c_str(), "r");
    if (!f) return "ffmpeg failed";
    std::vector<std::string> tail;
    char buf[512];
    while (std::fgets(buf, sizeof(buf), f)) {
        std::string line = buf;
        while (!line.empty() && (line.back() == '\n' || line.back() == '\r')) line.pop_back();
        if (line.empty() || line.rfind("ffmpeg version", 0) == 0) continue;
        tail.push_back(line);
        if (tail.size() > 3) tail.erase(tail.begin());
    }
    std::fclose(f);
    if (tail.empty()) return "ffmpeg failed";
    std::string out;
    for (size_t i = 0; i < tail.size(); ++i) {
        if (i) out += " | ";
        out += tail[i];
    }
    return out.substr(0, 240);
}

Engine::Probe Engine::probeInput(const std::string& path) {
    Probe p;
#ifdef CONVERT_ANDROID
    AVFormatContext* ctx = nullptr;
    if (avformat_open_input(&ctx, path.c_str(), nullptr, nullptr) < 0) return p;
    if (avformat_find_stream_info(ctx, nullptr) < 0) {
        avformat_close_input(&ctx);
        return p;
    }
    if (ctx->duration != AV_NOPTS_VALUE) p.durationUs = ctx->duration;
    for (unsigned i = 0; i < ctx->nb_streams; ++i) {
        const AVStream* st = ctx->streams[i];
        if (st->codecpar->codec_type == AVMEDIA_TYPE_VIDEO && !p.hasVideo) {
            p.hasVideo = true;
            p.width = st->codecpar->width;
            p.height = st->codecpar->height;
            const char* pf = av_get_pix_fmt_name(
                static_cast<AVPixelFormat>(st->codecpar->format));
            if (pf) p.pixFmt = pf;
            if (st->avg_frame_rate.den > 0)
                p.fps = static_cast<double>(st->avg_frame_rate.num) / st->avg_frame_rate.den;
            if (st->duration != AV_NOPTS_VALUE) {
                const int64_t den = st->time_base.den > 0 ? st->time_base.den : 1;
                const int64_t sdur = st->duration * 1000000LL * static_cast<int64_t>(st->time_base.num) / den;
                if (sdur > p.durationUs) p.durationUs = sdur;
            }
        } else if (st->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            p.hasAudio = true;
            if (st->duration != AV_NOPTS_VALUE) {
                const int64_t den = st->time_base.den > 0 ? st->time_base.den : 1;
                const int64_t sdur = st->duration * 1000000LL * static_cast<int64_t>(st->time_base.num) / den;
                if (sdur > p.durationUs) p.durationUs = sdur;
            }
        }
    }
    avformat_close_input(&ctx);
#else
    (void)path;
#endif
    return p;
}

bool Engine::validateOutputFile(const std::string& path, const std::string& typeId) {
    struct stat st;
    if (stat(path.c_str(), &st) != 0) return false;
    if (!S_ISREG(st.st_mode)) return false;
    if (st.st_size <= 0) return false;

#ifdef CONVERT_ANDROID
    Probe p = probeInput(path);
    if (typeId == "audio" || typeId == "video_to_audio") {
        return p.hasAudio;
    }
    if (typeId == "video" || typeId == "image") {
        return p.hasVideo;
    }
#else
    (void)typeId;
#endif
    return true;
}

std::string Engine::probeJson(const std::string& path) const {
    // probeInput is non-const (uses no state); const_cast keeps JNI simple.
    Probe p = const_cast<Engine*>(this)->probeInput(path);
    std::ostringstream o;
    o << "{\"durationMs\":" << p.durationUs / 1000 << ",\"width\":" << p.width
      << ",\"height\":" << p.height << ",\"fps\":" << p.fps
      << ",\"pixFmt\":\"" << escapeJson(p.pixFmt) << "\""
      << ",\"hasVideo\":" << (p.hasVideo ? "true" : "false")
      << ",\"hasAudio\":" << (p.hasAudio ? "true" : "false") << "}";
    return o.str();
}

std::string Engine::snapshotJson() const {
    std::lock_guard<std::mutex> l(m_);
    std::ostringstream o;
    o << "[";
    bool first = true;
    for (const auto& j : jobs_) {
        if (!first) o << ",";
        first = false;
        o << "{\"id\":\"" << escapeJson(j.id) << "\",\"batchId\":\"" << escapeJson(j.batchId)
          << "\",\"typeId\":\"" << escapeJson(j.typeId) << "\",\"sourceUri\":\"" << escapeJson(j.sourceUri)
          << "\",\"sourceName\":\"" << escapeJson(j.sourceName) << "\",\"mimeType\":\"" << escapeJson(j.mimeType)
          << "\",\"formatExt\":\"" << escapeJson(j.formatExt) << "\",\"status\":\"" << j.status
          << "\",\"progress\":" << j.progress << ",\"outputPath\":\"" << escapeJson(j.outputPath)
          << "\",\"error\":\"" << escapeJson(j.error) << "\"}";
    }
    return o << "]", o.str();
}

std::string Engine::typesJson() const {
    std::ostringstream o;
    o << "[";
    bool first = true;
    for (const auto& t : types()) {
        if (!first) o << ",";
        first = false;
        o << "{\"id\":\"" << t.id << "\",\"title\":\"" << escapeJson(t.title)
          << "\",\"inputKind\":\"" << t.inputKind << "\",\"subfolder\":\"" << t.subfolder << "\",\"formats\":[";
        bool f2 = true;
        for (const auto& f : t.formats) {
            if (!f2) o << ",";
            f2 = false;
            o << "{\"ext\":\"" << f.ext << "\",\"label\":\"" << escapeJson(f.label)
              << "\",\"mime\":\"" << f.mime << "\",\"blurb\":\"" << escapeJson(f.blurb)
              << "\",\"container\":\"" << containerOf(f.ext) << "\"}";
        }
        o << "]}";
    }
    return o << "]", o.str();
}

std::string escapeJson(const std::string& s) {
    std::string o;
    for (char c : s) {
        switch (c) {
            case '"': o += "\\\""; break;
            case '\\': o += "\\\\"; break;
            case '\n': o += "\\n"; break;
            case '\r': o += "\\r"; break;
            case '\t': o += "\\t"; break;
            default: o += c;
        }
    }
    return o;
}

}  // namespace convert
