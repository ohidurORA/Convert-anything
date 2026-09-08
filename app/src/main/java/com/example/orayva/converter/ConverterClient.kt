package com.example.orayva.converter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.orayva.native.EngineBridge

/**
 * Main-process client for the :converter sandbox. Binds on demand, links to
 * binder death: if the sandbox dies mid-job (native abort), the job fails
 * with a message instead of taking the app down with it.
 *
 * ffmpeg_main is NOT re-entrant (global CLI state). After every job the
 * sandbox is unbound so the next job starts a fresh :converter process.
 * Without that, "first file works, the rest of the batch fail".
 */
object ConverterClient {
    private val bindLock = Object()
    private var messenger: Messenger? = null
    private var bound = false
    private val activeJobs = mutableSetOf<String>()

    private val replies = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == ConverterService.MSG_DONE) {
                val id = msg.data.getString(ConverterService.KEY_JOB) ?: return
                val rc = msg.data.getInt(ConverterService.KEY_RC, -1)
                val err = msg.data.getString(ConverterService.KEY_ERR) ?: ""
                synchronized(bindLock) { activeJobs.remove(id) }
                EngineBridge.onRemoteFinished(id, rc, err)
            }
        }
    }
    private val replyTo = Messenger(replies)

    private fun unbindSandbox() {
        val ctx = EngineBridge.appContextForKill() ?: return
        runCatching { ctx.unbindService(conn) }
    }

    private fun converterPid(): Int? {
        val ctx = EngineBridge.appContextForKill() ?: return null
        return runCatching {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.runningAppProcesses?.firstOrNull { it.processName.endsWith(":converter") }?.pid
                ?.takeIf { it != android.os.Process.myPid() }
        }.getOrNull()
    }

    private fun ensureFreshProcess() {
        // Must unbind BEFORE killing, otherwise BIND_AUTO_CREATE on the next
        // job reconnects to a dying :converter (ffmpeg_main still dirty).
        synchronized(bindLock) {
            messenger = null
            bound = false
        }
        unbindSandbox()
        killConverterPid()
        val deadline = System.currentTimeMillis() + 2500
        while (converterPid() != null && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(40) } catch (_: InterruptedException) { return }
        }
    }

    private fun killConverterPid() {
        val ctx = EngineBridge.appContextForKill() ?: return
        runCatching {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val pid = am.runningAppProcesses
                ?.firstOrNull { it.processName.endsWith(":converter") }?.pid
            if (pid != null && pid != android.os.Process.myPid()) {
                android.os.Process.killProcess(pid)
            }
        }.onFailure { Log.w("ConverterClient", "kill failed", it) }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            synchronized(bindLock) {
                messenger = Messenger(binder)
                try {
                    binder?.linkToDeath({
                        val ids: Set<String>
                        synchronized(bindLock) {
                            ids = activeJobs.toSet()
                            activeJobs.clear()
                            messenger = null
                            bound = false
                        }
                        ids.forEach {
                            EngineBridge.onRemoteFinished(it, -999, "Converter stopped unexpectedly")
                        }
                    }, 0)
                } catch (e: Exception) {
                    Log.w("ConverterClient", "linkToDeath failed", e)
                }
                bound = true
                bindLock.notifyAll()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(bindLock) {
                messenger = null
                bound = false
            }
        }
    }

    /** Blocking until bound; then fire-and-forget (completion via reply/death). */
    fun dispatch(
        context: Context,
        jobId: String,
        typeId: String,
        formatExt: String,
        outputPath: String,
        progressPath: String,
        stderrPath: String,
        inputExt: String,
        inputPath: String,
    ): Boolean {
        val ctx = context.applicationContext
        // Worker thread: wait until any previous :converter is actually gone
        // before BIND_AUTO_CREATE, or we reconnect to a poisoned ffmpeg_main.
        ensureFreshProcess()
        // ffmpeg_main is not re-entrant. Two attempts: first bind, and one
        // self-heal rebind if the cached messenger is already dead.
        repeat(2) { attempt ->
            val sent = trySend(ctx, jobId, typeId, formatExt, outputPath, progressPath, stderrPath, inputExt, inputPath)
            if (sent) return true
            synchronized(bindLock) {
                messenger = null
                bound = false
            }
            unbindSandbox()
            if (attempt == 0) Log.w("ConverterClient", "dispatch retry for $jobId")
        }
        return false
    }

    private fun trySend(
        ctx: Context,
        jobId: String,
        typeId: String,
        formatExt: String,
        outputPath: String,
        progressPath: String,
        stderrPath: String,
        inputExt: String,
        inputPath: String,
    ): Boolean {
        synchronized(bindLock) {
            if (!bound) {
                val ok = runCatching {
                    ctx.bindService(
                        Intent(ctx, ConverterService::class.java), conn, Context.BIND_AUTO_CREATE)
                }.getOrDefault(false)
                if (!ok) return false
                bindLock.wait(15000)
            }
            val m = messenger ?: return false
            val (pfd, fallbackPath) = EngineBridge.takeInput(jobId)
            val path = fallbackPath.ifBlank { inputPath }
            // Dup the fd before the binder copies it: the original pfd still
            // belongs to ConvertApp.finalize() and must not be closed here.
            val sendPfd: ParcelFileDescriptor? = pfd?.let { runCatching { it.dup() }.getOrNull() }
            // /proc/self/fd/N is process-local. Never send the main-process
            // fd path into :converter — it would look like a valid path and
            // then fail the probe (batch job 2+ "unreadable input").
            val crossProcPath = path.takeIf { it.isNotBlank() && !it.startsWith("/proc/") }
            if (sendPfd == null && crossProcPath == null) {
                EngineBridge.noteInput(jobId, pfd, fallbackPath)
                return false
            }
            synchronized(bindLock) { activeJobs.add(jobId) }
            val data = Bundle().apply {
                putString(ConverterService.KEY_JOB, jobId)
                putString(ConverterService.KEY_TYPE, typeId)
                putString(ConverterService.KEY_EXT, formatExt)
                putString(ConverterService.KEY_OUT, outputPath)
                putString(ConverterService.KEY_PROG, progressPath)
                putString(ConverterService.KEY_ERR, stderrPath)
                putString(ConverterService.KEY_IN_EXT, inputExt)
                if (sendPfd != null) putParcelable(ConverterService.KEY_IN_FD, sendPfd)
                else putString(ConverterService.KEY_IN_PATH, crossProcPath)
            }
            return try {
                m.send(Message.obtain(null, ConverterService.MSG_RUN).apply {
                    this.data = data
                    replyTo = this@ConverterClient.replyTo
                })
                true
            } catch (t: Throwable) {
                synchronized(bindLock) { activeJobs.remove(jobId) }
                EngineBridge.noteInput(jobId, pfd, fallbackPath)
                false
            } finally {
                runCatching { sendPfd?.close() }
            }
        }
    }

    fun cancel(jobId: String) {
        synchronized(bindLock) {
            runCatching {
                messenger?.send(Message.obtain(null, ConverterService.MSG_CANCEL).apply {
                    data = Bundle().apply {
                        putString(ConverterService.KEY_JOB, jobId)
                    }
                })
            }
        }
    }

    /** Watchdog last resort: SIGKILL our own sandbox; death notice fails the job. */
    fun killSandbox() {
        killConverterPid()
        synchronized(bindLock) {
            messenger = null
            bound = false
        }
        unbindSandbox()
    }
}
