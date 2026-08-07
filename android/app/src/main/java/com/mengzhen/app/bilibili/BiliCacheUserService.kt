package com.mengzhen.app.bilibili

import android.content.Context
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.annotation.Keep
import java.io.File
import java.io.IOException

@Keep
class BiliCacheUserService : IBiliCacheService.Stub {
    private val watcherLock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cacheObserver: FileObserver? = null
    private var changeOutput: ParcelFileDescriptor.AutoCloseOutputStream? = null
    private val rebuildObserver = Runnable {
        synchronized(watcherLock) {
            if (changeOutput != null) replaceObserverLocked()
        }
    }

    constructor()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context)

    override fun scanDefaultCaches(): String =
        BiliCacheItem.listToJson(BiliShellCacheScanner.scanDefaultCaches())

    override fun openFile(absolutePath: String?): ParcelFileDescriptor? {
        val path = absolutePath?.takeIf(String::isNotBlank) ?: return null
        val file = File(path)
        if (!isAllowedCacheFile(file) || !file.isFile) return null
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun watchDefaultCaches(): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        synchronized(watcherLock) {
            stopWatchingLocked()
            changeOutput = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
            replaceObserverLocked()
        }
        return pipe[0]
    }

    override fun stopWatchingDefaultCaches() {
        synchronized(watcherLock) {
            stopWatchingLocked()
        }
    }

    override fun identity(): String = "uid=${Process.myUid()},pid=${Process.myPid()}"

    override fun destroy() {
        stopWatchingDefaultCaches()
        System.exit(0)
    }

    private fun replaceObserverLocked() {
        cacheObserver?.stopWatching()
        val directories = BiliShellCacheScanner.watchDirectories(MAX_WATCH_DEPTH)
        cacheObserver = if (directories.isEmpty()) {
            null
        } else {
            object : FileObserver(directories, WATCH_EVENTS) {
                override fun onEvent(event: Int, path: String?) {
                    emitChange()
                    if (event and REBUILD_EVENTS != 0) {
                        mainHandler.removeCallbacks(rebuildObserver)
                        mainHandler.postDelayed(rebuildObserver, REBUILD_DELAY_MS)
                    }
                }
            }.also(FileObserver::startWatching)
        }
    }

    private fun emitChange() {
        synchronized(watcherLock) {
            val output = changeOutput ?: return
            try {
                output.write(1)
                output.flush()
            } catch (_: IOException) {
                stopWatchingLocked()
            }
        }
    }

    private fun stopWatchingLocked() {
        mainHandler.removeCallbacks(rebuildObserver)
        cacheObserver?.stopWatching()
        cacheObserver = null
        runCatching { changeOutput?.close() }
        changeOutput = null
    }

    private fun isAllowedCacheFile(file: File): Boolean {
        val canonical = runCatching { file.canonicalPath }.getOrNull() ?: return false
        return canonical.startsWith(
            "/storage/emulated/0/Android/data/tv.danmaku.bili/",
            ignoreCase = false,
        )
    }

    companion object {
        private const val MAX_WATCH_DEPTH = 9
        private const val REBUILD_DELAY_MS = 250L
        private const val WATCH_EVENTS =
            FileObserver.CLOSE_WRITE or
                FileObserver.CREATE or
                FileObserver.DELETE or
                FileObserver.MOVED_FROM or
                FileObserver.MOVED_TO or
                FileObserver.DELETE_SELF or
                FileObserver.MOVE_SELF
        private const val REBUILD_EVENTS =
            FileObserver.CREATE or
                FileObserver.MOVED_TO or
                FileObserver.DELETE_SELF or
                FileObserver.MOVE_SELF
    }
}
