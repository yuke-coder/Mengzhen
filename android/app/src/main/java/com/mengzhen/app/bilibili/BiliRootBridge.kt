package com.mengzhen.app.bilibili

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BiliRootBridge(context: Context) {
    private val appContext = context.applicationContext
    private val connectMutex = Mutex()

    @Volatile
    private var service: IBiliCacheService? = null

    fun hasPermission(): Boolean =
        service?.asBinder()?.isBinderAlive == true || Shell.isAppGrantedRoot() == true

    suspend fun requestPermission(): Boolean {
        if (!hasRootShell()) return false
        return runCatching { connect() }.isSuccess
    }

    suspend fun scanDefaultCaches(): List<BiliCacheItem> = withContext(Dispatchers.IO) {
        BiliCacheItem.listFromJson(connect().scanDefaultCaches()).map {
            it.copy(accessMode = BiliCacheAccessMode.ROOT)
        }
    }

    suspend fun openFile(path: String): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        connect().openFile(path)
    }

    fun observeDefaultCacheChanges(): Flow<Unit> = flow {
        val descriptor = connect().watchDefaultCaches()
        try {
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                val buffer = ByteArray(32)
                while (
                    currentCoroutineContext().isActive &&
                    runInterruptible { input.read(buffer) } >= 0
                ) {
                    emit(Unit)
                }
            }
        } finally {
            runCatching { service?.stopWatchingDefaultCaches() }
        }
    }.conflate().flowOn(Dispatchers.IO)

    private suspend fun connect(): IBiliCacheService = connectMutex.withLock {
        service?.takeIf { it.asBinder().isBinderAlive }?.let { return@withLock it }
        if (!hasRootShell()) {
            throw SecurityException("未获得本地缓存读取授权")
        }

        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                        val connected = IBiliCacheService.Stub.asInterface(binder)
                        service = connected
                        if (continuation.isActive) continuation.resume(connected)
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        service = null
                    }

                    override fun onNullBinding(name: ComponentName) {
                        service = null
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("本地缓存读取服务连接失败"),
                            )
                        }
                    }
                }
                try {
                    RootService.bind(
                        Intent(appContext, BiliRootCacheService::class.java),
                        connection,
                    )
                } catch (error: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        }
    }

    private suspend fun hasRootShell(): Boolean = withContext(Dispatchers.IO) {
        runCatching { Shell.getShell().isRoot }.getOrDefault(false)
    }
}
