#pragma once
// Convert Anything — C++ engine (the backend). Owns:
// conversion-type registry, FFmpeg argv builders, job queue + worker,
// in-process ffmpeg_main invocation, libavformat probing.
// Kotlin is a thin JNI bridge; UI only collects snapshots.
#include <atomic>
#include <condition_variable>
#include <deque>
#include <functional>
#include <map>
#include <mutex>
#include <string>
#include <thread>
#include <vector>
#include <chrono>

namespace convert {

struct FormatInfo {
    std::string ext, label, mime;
    std::string blurb;  // one-line benefit + typical use, shown on long-press
};

struct TypeInfo {
    std::string id, title, inputKind, subfolder;
    std::vector<FormatInfo> formats;
};

struct Job {
    std::string id, batchId, typeId;
    std::string inputPath, outputPath, formatExt;
    std::string sourceUri, sourceName, mimeType;
    std::string workDir;  // writable scratch dir for .progress/.stderr files
    std::string status = "queued";  // queued|running|done|failed
    int progress = 0;
    std::string error;
    bool cancelRequested = false;
};

class Engine {
public:
    static Engine& instance();

    // Registry: new conversion type = one entry in builtinTypes(). No JNI change.
    std::vector<TypeInfo> types() const;
    // Per-type argv builders (no binary name). New type = one case here.
    // pixFmt (e.g. "gray") lets builders normalize inputs an encoder rejects.
    // inputExt routes to the per-input-format file (audio/mp3.h, image/jpg.h…);
    // empty/unknown falls back to the output-side builder — same argv.
    std::vector<std::string> buildArgs(const std::string& typeId,
                                       const std::string& in,
                                       const std::string& out,
                                       const std::string& ext,
                                       const std::string& pixFmt = "",
                                       const std::string& inputExt = "");

    std::string enqueue(const std::string& typeId, const std::string& in,
                        const std::string& out, const std::string& ext,
                        const std::string& batchId, const std::string& uri,
                        const std::string& name, const std::string& mime,
                        const std::string& workDir);
    void cancel(const std::string& jobId);
    void clearDone();
    void remove(const std::string& jobId);  // drops one finished job (never running)
    std::string snapshotJson() const;  // jobs array for Kotlin StateFlow
    std::string typesJson() const;
    /** libavformat probe for Studio metadata + progress math. */
    std::string probeJson(const std::string& path) const;
    /** Sandbox completion/death notice from Kotlin. */
    void onRemoteFinished(const std::string& id, int rc, const std::string& error);
    void setListener(std::function<void(const std::string&)> cb);

private:
    Engine();
    ~Engine();
    Engine(const Engine&) = delete;

    void loop();  // sequential worker; parallel encodes OOM phones
    int runTranscode(Job& job);  // 0 ok; sets job.error from ffmpeg's stderr
    void notify();

    // Remote execution in the :converter sandbox process. A native abort
    // there kills only that process; here it surfaces as a failed job.
    struct RemoteResult { bool ready = false; int rc = 0; std::string error; };
    bool dispatchRemote(const std::string& id, const std::string& typeId,
                        const std::string& formatExt, const std::string& outputPath,
                        const std::string& progressPath, const std::string& stderrPath,
                        const std::string& inputExt, const std::string& inputPath);
    bool takeRemoteResult(const std::string& id, int& rc, std::string& error);
    std::map<std::string, RemoteResult> remote_;

    struct Probe {
        int64_t durationUs = 0;
        int width = 0, height = 0;
        double fps = 0;
        bool hasVideo = false, hasAudio = false;
        std::string pixFmt;  // input pixel format name, e.g. "yuv420p", "gray"
    };
    Probe probeInput(const std::string& path);
    bool validateOutputFile(const std::string& path, const std::string& typeId);
    static int readProgress(const std::string& path, int64_t durationUs,
                            const std::chrono::steady_clock::time_point& start);
    static std::string lastStderrLines(const std::string& path);

    mutable std::mutex m_;
    std::condition_variable cv_;
    // deque: references stay valid across push_back/erase of OTHER elements,
    // so the worker's Job& survives concurrent enqueue/clearDone.
    std::deque<Job> jobs_;
    std::function<void(const std::string&)> listener_;
    std::thread worker_;
    bool stop_ = false;
    long nextId_ = 1;
};

std::string escapeJson(const std::string& s);

// Runs ffmpeg_main on the CALLER thread (used by the :converter sandbox).
// cancelFlag: per-job atomic the caller monitors; set to true to request abort.
// Returns ffmpeg's rc. foil for aborts: none in-process — the process boundary
// is the isolation.
int runFfmpegBlocking(const std::vector<std::string>& args,
                      const std::string& progressPath,
                      const std::string& stderrPath,
                      std::atomic<bool>* cancelFlag = nullptr);
void cancelCurrentTranscode(const std::string& jobId);

// Per-job cancel flags (used by bridge.cpp to register/cancel individual runs).
extern std::map<std::string, std::atomic<bool>*> g_cancelFlags;
extern std::mutex g_cancelMtx;

// JNI upcalls into Kotlin (implemented in bridge.cpp): dispatch/cancel the
// sandboxed run. Declared here so the engine worker can drive them.
namespace bridge {
bool dispatchRemote(const std::string& jobId, const std::string& typeId,
                    const std::string& formatExt, const std::string& outputPath,
                    const std::string& progressPath, const std::string& stderrPath,
                    const std::string& inputExt, const std::string& inputPath);
void cancelRemote(const std::string& jobId);
void killConverter();  // SIGKILL the sandbox (watchdog last resort)
}  // namespace bridge

}  // namespace convert
