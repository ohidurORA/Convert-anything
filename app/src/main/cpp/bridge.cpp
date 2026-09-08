// Thin JNI bridge: forwards calls to convert::Engine. No logic lives here.
#include <jni.h>
#include <string>
#include <vector>

#include "engine.h"

#ifdef CONVERT_ANDROID
extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
}
#include <android/bitmap.h>
#endif

static JavaVM* g_vm = nullptr;
static jobject g_listener = nullptr;  // global ref to EngineBridge.JobListener
static jclass g_cls = nullptr;        // global ref: local class refs die with the call
static jmethodID g_onChanged = nullptr;

static std::string jstr(JNIEnv* env, jstring s) {
    if (!s) return "";
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string o(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return o;
}

static void emitToKotlin(const std::string& snapshot) {
    if (!g_vm || !g_listener || !g_onChanged) return;
    JNIEnv* env = nullptr;
    bool attached = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK;
    if (attached && g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
    jstring s = env->NewStringUTF(snapshot.c_str());
    env->CallVoidMethod(g_listener, g_onChanged, s);
    if (env->ExceptionCheck()) env->ExceptionClear();  // never let a UI parse bug kill the worker
    env->DeleteLocalRef(s);
    if (attached) g_vm->DetachCurrentThread();
}

// Cached app-class refs for worker-thread upcalls (resolved on app threads,
// where the class loader is valid — never in JNI_OnLoad).
static jclass g_bridgeCls = nullptr;
static jmethodID g_runRemote = nullptr;    // static runRemote(8 x String) -> boolean
static jmethodID g_cancelRemote = nullptr;  // static cancelRemote(String)
static jmethodID g_killConverter = nullptr;  // static killConverter()

static void ensureBridgeRefs(JNIEnv* env) {
    if (g_bridgeCls) return;
    jclass local =
        env->FindClass("com/example/converanything/native/EngineBridge");
    if (!local) return;
    g_bridgeCls = static_cast<jclass>(env->NewGlobalRef(local));
    env->DeleteLocalRef(local);
    g_runRemote = env->GetStaticMethodID(g_bridgeCls, "runRemote",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z");
    g_cancelRemote =
        env->GetStaticMethodID(g_bridgeCls, "cancelRemote",
            "(Ljava/lang/String;)V");
    g_killConverter =
        env->GetStaticMethodID(g_bridgeCls, "killConverter", "()V");
}

namespace convert {
namespace bridge {

bool dispatchRemote(const std::string& jobId, const std::string& typeId,
                    const std::string& formatExt, const std::string& outputPath,
                    const std::string& progressPath, const std::string& stderrPath,
                    const std::string& inputExt, const std::string& inputPath) {
    if (!g_vm) return false;
    JNIEnv* env = nullptr;
    bool attached = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK;
    if (attached && g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return false;
    ensureBridgeRefs(env);
    bool ok = g_runRemote != nullptr;
    if (ok) {
        jstring jid = env->NewStringUTF(jobId.c_str());
        jstring jtype = env->NewStringUTF(typeId.c_str());
        jstring jext = env->NewStringUTF(formatExt.c_str());
        jstring jout = env->NewStringUTF(outputPath.c_str());
        jstring jprog = env->NewStringUTF(progressPath.c_str());
        jstring jerr = env->NewStringUTF(stderrPath.c_str());
        jstring jinext = env->NewStringUTF(inputExt.c_str());
        jstring jinpath = env->NewStringUTF(inputPath.c_str());
        jboolean dispatched = env->CallStaticBooleanMethod(
            g_bridgeCls, g_runRemote, jid, jtype, jext, jout, jprog, jerr, jinext, jinpath);
        if (env->ExceptionCheck()) { env->ExceptionClear(); ok = false; }
        else ok = dispatched == JNI_TRUE;
        env->DeleteLocalRef(jid);
        env->DeleteLocalRef(jtype);
        env->DeleteLocalRef(jext);
        env->DeleteLocalRef(jout);
        env->DeleteLocalRef(jprog);
        env->DeleteLocalRef(jerr);
        env->DeleteLocalRef(jinext);
        env->DeleteLocalRef(jinpath);
    }
    if (attached) g_vm->DetachCurrentThread();
    return ok;
}

void cancelRemote(const std::string& jobId) {
    if (!g_vm) return;
    JNIEnv* env = nullptr;
    bool attached = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK;
    if (attached && g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
    ensureBridgeRefs(env);
    if (g_cancelRemote) {
        jstring jid = env->NewStringUTF(jobId.c_str());
        env->CallStaticVoidMethod(g_bridgeCls, g_cancelRemote, jid);
        if (env->ExceptionCheck()) env->ExceptionClear();
        env->DeleteLocalRef(jid);
    }
    if (attached) g_vm->DetachCurrentThread();
}

void killConverter() {
    if (!g_vm) return;
    JNIEnv* env = nullptr;
    bool attached = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK;
    if (attached && g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
    ensureBridgeRefs(env);
    if (g_killConverter) {
        env->CallStaticVoidMethod(g_bridgeCls, g_killConverter);
        if (env->ExceptionCheck()) env->ExceptionClear();
    }
    if (attached) g_vm->DetachCurrentThread();
}

}  // namespace bridge
}  // namespace convert

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL
Java_com_example_converanything_native_EngineBridge_nativeTypes(JNIEnv* env, jobject) {
    return env->NewStringUTF(convert::Engine::instance().typesJson().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_converanything_native_EngineBridge_nativeEnqueue(
        JNIEnv* env, jobject, jstring typeId, jstring in, jstring out, jstring ext,
        jstring batchId, jstring uri, jstring name, jstring mime, jstring workDir) {
    std::string id = convert::Engine::instance().enqueue(
        jstr(env, typeId), jstr(env, in), jstr(env, out), jstr(env, ext),
        jstr(env, batchId), jstr(env, uri), jstr(env, name), jstr(env, mime),
        jstr(env, workDir));
    return env->NewStringUTF(id.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_converanything_native_EngineBridge_nativeCancel(JNIEnv* env, jobject, jstring id) {
    convert::Engine::instance().cancel(jstr(env, id));
}

JNIEXPORT void JNICALL
Java_com_example_converanything_native_EngineBridge_nativeClearDone(JNIEnv*, jobject) {
    convert::Engine::instance().clearDone();
}

JNIEXPORT void JNICALL
Java_com_example_converanything_native_EngineBridge_nativeRemove(JNIEnv* env, jobject, jstring id) {
    convert::Engine::instance().remove(jstr(env, id));
}

JNIEXPORT jstring JNICALL
Java_com_example_converanything_native_EngineBridge_nativeSnapshot(JNIEnv* env, jobject) {
    return env->NewStringUTF(convert::Engine::instance().snapshotJson().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_converanything_native_EngineBridge_nativeProbe(JNIEnv* env, jobject, jstring path) {
    const char* c = env->GetStringUTFChars(path, nullptr);
    std::string json = convert::Engine::instance().probeJson(c ? c : "");
    if (c) env->ReleaseStringUTFChars(path, c);
    return env->NewStringUTF(json.c_str());
}

#ifdef CONVERT_ANDROID
// First-frame decode to RGBA Bitmap for formats the platform thumbnailer
// can't render (tiff, jp2, dpx, ppm, avif...). Null on any failure.
static jobject decodeThumb(JNIEnv* env, const std::string& path, int maxDim) {
    AVFormatContext* fmt = nullptr;
    if (avformat_open_input(&fmt, path.c_str(), nullptr, nullptr) < 0) return nullptr;
    if (avformat_find_stream_info(fmt, nullptr) < 0) {
        avformat_close_input(&fmt);
        return nullptr;
    }
    int vs = av_find_best_stream(fmt, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    if (vs < 0) { avformat_close_input(&fmt); return nullptr; }
    const AVCodec* codec = avcodec_find_decoder(fmt->streams[vs]->codecpar->codec_id);
    AVCodecContext* ctx = codec ? avcodec_alloc_context3(codec) : nullptr;
    jobject out = nullptr;
    if (ctx && avcodec_parameters_to_context(ctx, fmt->streams[vs]->codecpar) >= 0 &&
        avcodec_open2(ctx, codec, nullptr) >= 0) {
        AVPacket* pkt = av_packet_alloc();
        AVFrame* frame = av_frame_alloc();
        bool got = false;
        while (!got && av_read_frame(fmt, pkt) >= 0) {
            if (pkt->stream_index != vs) { av_packet_unref(pkt); continue; }
            if (avcodec_send_packet(ctx, pkt) < 0) { av_packet_unref(pkt); break; }
            got = avcodec_receive_frame(ctx, frame) >= 0;
            av_packet_unref(pkt);
        }
        if (got && frame->width > 0 && frame->height > 0) {
            int w = frame->width, h = frame->height;
            if (w > maxDim || h > maxDim) {
                if (w >= h) { h = h * maxDim / w; w = maxDim; } else { w = w * maxDim / h; h = maxDim; }
                w &= ~1; h &= ~1;
            }
            jclass bmpCls = env->FindClass("android/graphics/Bitmap");
            jclass cfgCls = env->FindClass("android/graphics/Bitmap$Config");
            jfieldID argb = env->GetStaticFieldID(cfgCls, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
            jobject argbObj = env->GetStaticObjectField(cfgCls, argb);
            jmethodID create = env->GetStaticMethodID(
                bmpCls, "createBitmap",
                "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
            jobject bmp = env->CallStaticObjectMethod(bmpCls, create, w, h, argbObj);
            if (bmp && !env->ExceptionCheck()) {
                void* pixels = nullptr;
                AndroidBitmapInfo info;
                if (AndroidBitmap_getInfo(env, bmp, &info) >= 0 &&
                    AndroidBitmap_lockPixels(env, bmp, &pixels) >= 0) {
                    SwsContext* sws = sws_getContext(
                        frame->width, frame->height, static_cast<AVPixelFormat>(frame->format),
                        w, h, AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr);
                    if (sws) {
                        uint8_t* dst[4] = {static_cast<uint8_t*>(pixels), nullptr, nullptr, nullptr};
                        int stride[4] = {static_cast<int>(info.stride), 0, 0, 0};
                        sws_scale(sws, frame->data, frame->linesize, 0, frame->height, dst, stride);
                        sws_freeContext(sws);
                        out = bmp;
                    } else {
                        env->DeleteLocalRef(bmp);
                    }
                    AndroidBitmap_unlockPixels(env, bmp);
                } else if (bmp) {
                    env->DeleteLocalRef(bmp);
                }
            }
            if (env->ExceptionCheck()) env->ExceptionClear();
            env->DeleteLocalRef(argbObj);
            env->DeleteLocalRef(bmpCls);
            env->DeleteLocalRef(cfgCls);
        }
        av_frame_free(&frame);
        av_packet_free(&pkt);
        avcodec_free_context(&ctx);
    } else if (ctx) {
        avcodec_free_context(&ctx);
    }
    avformat_close_input(&fmt);
    return out;
}
#endif

JNIEXPORT jobject JNICALL
Java_com_example_converanything_native_EngineBridge_nativeDecodeThumb(JNIEnv* env, jobject,
                                                                      jstring path, jint maxDim) {
#ifdef CONVERT_ANDROID
    const char* c = env->GetStringUTFChars(path, nullptr);
    jobject bmp = c ? decodeThumb(env, c, maxDim > 0 ? maxDim : 320) : nullptr;
    if (c) env->ReleaseStringUTFChars(path, c);
    return bmp;
#else
    (void)env; (void)path; (void)maxDim;
    return nullptr;
#endif
}

JNIEXPORT jobjectArray JNICALL
Java_com_example_converanything_native_EngineBridge_nativeBuildArgs(JNIEnv* env, jobject,
        jstring typeId, jstring in, jstring out, jstring ext, jstring pixFmt, jstring inputExt) {
    std::vector<std::string> args = convert::Engine::instance().buildArgs(
        jstr(env, typeId), jstr(env, in), jstr(env, out), jstr(env, ext),
        jstr(env, pixFmt), jstr(env, inputExt));
    jclass strCls = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(static_cast<jsize>(args.size()), strCls, nullptr);
    for (size_t i = 0; i < args.size(); ++i)
        env->SetObjectArrayElement(arr, static_cast<jsize>(i),
                                   env->NewStringUTF(args[i].c_str()));
    return arr;
}

JNIEXPORT jint JNICALL
Java_com_example_converanything_native_EngineBridge_nativeRunFfmpeg(JNIEnv* env, jobject,
        jobjectArray jargs, jstring jprogress, jstring jstderr, jstring jjobId) {
    jsize n = env->GetArrayLength(jargs);
    std::vector<std::string> args;
    for (jsize i = 0; i < n; ++i) {
        auto* s = static_cast<jstring>(env->GetObjectArrayElement(jargs, i));
        const char* c = env->GetStringUTFChars(s, nullptr);
        args.emplace_back(c ? c : "");
        if (c) env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }
    // Register a per-job cancel flag so nativeCancelFfmpeg(jobId) can target this run.
    std::string jobId = jstr(env, jjobId);
    auto* flag = new std::atomic<bool>(false);
    {
        std::lock_guard<std::mutex> l(convert::g_cancelMtx);
        convert::g_cancelFlags[jobId] = flag;
    }
    int rc = convert::runFfmpegBlocking(
        args, jstr(env, jprogress), jstr(env, jstderr), flag);
    // Clean up the cancel flag after the run completes.
    {
        std::lock_guard<std::mutex> l(convert::g_cancelMtx);
        convert::g_cancelFlags.erase(jobId);
    }
    delete flag;
    return rc;
}

JNIEXPORT void JNICALL
Java_com_example_converanything_native_EngineBridge_nativeCancelFfmpeg(JNIEnv* env, jobject,
        jstring jobId) {
    convert::cancelCurrentTranscode(jstr(env, jobId));
}

JNIEXPORT void JNICALL
Java_com_example_converanything_native_EngineBridge_nativeNotifyFinished(JNIEnv* env, jobject,
        jstring jobId, jint rc, jstring error) {
    convert::Engine::instance().onRemoteFinished(
        jstr(env, jobId), rc, jstr(env, error));
}

JNIEXPORT void JNICALL
Java_com_example_converanything_native_EngineBridge_nativeSetListener(JNIEnv* env, jobject, jobject listener) {
    if (g_listener) {
        env->DeleteGlobalRef(g_listener);
        env->DeleteGlobalRef(g_cls);
        g_listener = nullptr;
        g_cls = nullptr;
        g_onChanged = nullptr;
    }
    if (listener) {
        g_listener = env->NewGlobalRef(listener);
        jclass local = env->GetObjectClass(listener);
        g_cls = static_cast<jclass>(env->NewGlobalRef(local));
        env->DeleteLocalRef(local);
        g_onChanged = env->GetMethodID(g_cls, "onChanged", "(Ljava/lang/String;)V");
        // Prime the upcall cache here (app thread: class loader is valid).
        ensureBridgeRefs(env);
        convert::Engine::instance().setListener(emitToKotlin);
    } else {
        convert::Engine::instance().setListener(nullptr);
    }
}

// Legacy sample entry kept for template compat.
JNIEXPORT jstring JNICALL
Java_com_example_converanything_MainActivity_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("Hello from C++");
}

}  // extern "C"
