package com.example.converanything.converter

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import androidx.core.os.BundleCompat
import com.example.converanything.native.EngineBridge

/**
 * Sandboxed ffmpeg runner (`android:process=":converter"`).
 * A native abort here kills only this process; the app marks the job failed.
 * ffmpeg runs on a 16 MB-stack thread (deep encoder stacks + 1 MB bionic
 * default stacks have caused SIGSEGVs in the wild).
 */
class ConverterService : Service() {
    companion object {
        const val MSG_RUN = 1
        const val MSG_CANCEL = 2
        const val MSG_DONE = 3
        const val KEY_JOB = "job"
        const val KEY_TYPE = "type"
        const val KEY_EXT = "ext"
        const val KEY_OUT = "out"
        const val KEY_PROG = "prog"
        const val KEY_ERR = "err"
        const val KEY_IN_FD = "inFd"
        const val KEY_IN_PATH = "inPath"
        const val KEY_IN_EXT = "inExt"
        const val KEY_RC = "rc"

        /**
         * Return code used when the sandbox run itself threw before ffmpeg could
         * run (see MSG_RUN). Sentinel that can never collide with a real ffmpeg
         * exit status, so the engine reliably fails the job instead of hanging.
         */
        const val ERROR_UNKNOWN = -5000
    }

    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MSG_RUN -> {
                val data = msg.data
                val replyTo = msg.replyTo
                val jobId = data.getString(KEY_JOB) ?: return@Handler true
                Thread(null, {
                    var rc = ERROR_UNKNOWN
                    var err = ""
                    try {
                        rc = runOne(data, jobId)
                    } catch (e: Throwable) {
                        // Never leave the engine worker polling forever: ALWAYS
                        // report a terminal result. A missing MSG_DONE stalls the
                        // whole *sequential* queue until the 30-minute watchdog,
                        // misreporting every subsequently queued job too. (A native
                        // abort is NOT a Throwable — that path is covered by the
                        // binder death-notice handled in ConverterClient.)
                        rc = ERROR_UNKNOWN
                        err = "${e.javaClass.simpleName}: ${e.message ?: "sandbox conversion error"}"
                    }
                    try {
                        replyTo?.send(Message.obtain(null, MSG_DONE).apply {
                            this.data = Bundle().apply {
                                putString(KEY_JOB, jobId)
                                putInt(KEY_RC, rc)
                                putString(KEY_ERR, err)
                            }
                        })
                    } catch (_: Throwable) {
                        // The client may have died already; its death-notice
                        // handler fails the job, so there is nothing more to do.
                    }
                    // MSG_DONE is already copied onto the client's queue.
                    // Stop so the next job binds a fresh process: ffmpeg_main
                    // is not re-entrant, which is why batch job 2+ used to fail.
                    stopSelf()
                }, "ffmpeg-run", 16 * 1024 * 1024L).start()
                true
            }
            MSG_CANCEL -> {
                val jobId = msg.data?.getString(KEY_JOB) ?: return@Handler true
                EngineBridge.nativeCancelFfmpeg(jobId)
                true
            }
            else -> false
        }
    }
    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?) = messenger.binder

    private val runLock = Any()

    private fun runOne(data: Bundle, jobId: String): Int {
        synchronized(runLock) {
            return runOneLocked(data, jobId)
        }
    }

    private fun runOneLocked(data: Bundle, jobId: String): Int {
        EngineBridge.ensureLoaded()
        val inPath: String
        val pfd: ParcelFileDescriptor? =
            BundleCompat.getParcelable(data, KEY_IN_FD, ParcelFileDescriptor::class.java)
        if (pfd != null) {
            inPath = "/proc/self/fd/${pfd.fd}"
            try {
                val pix = EngineBridge.probe(inPath).pixFmt
                val args = EngineBridge.nativeBuildArgs(
                    data.getString(KEY_TYPE, ""), inPath,
                    data.getString(KEY_OUT, ""), data.getString(KEY_EXT, ""), pix,
                    data.getString(KEY_IN_EXT, ""))
                return EngineBridge.nativeRunFfmpeg(
                    args, data.getString(KEY_PROG, ""), data.getString(KEY_ERR, ""), jobId)
            } finally {
                runCatching { pfd.close() }
            }
        }
        val args = EngineBridge.nativeBuildArgs(
            data.getString(KEY_TYPE, ""), data.getString(KEY_IN_PATH, ""),
            data.getString(KEY_OUT, ""), data.getString(KEY_EXT, ""),
            EngineBridge.probe(data.getString(KEY_IN_PATH, "")).pixFmt,
            data.getString(KEY_IN_EXT, ""))
        return EngineBridge.nativeRunFfmpeg(
            args, data.getString(KEY_PROG, ""), data.getString(KEY_ERR, ""), jobId)
    }
}
