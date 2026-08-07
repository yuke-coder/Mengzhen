package com.mengzhen.app.bilibili

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BiliShizukuBridge(context: Context) {
    private val appContext = context.applicationContext
    private val connectMutex = Mutex()
    private val permissionMutex = Mutex()

    @Volatile
    private var service: IBiliCacheService? = null

    @Volatile
    private var serviceConnection: ServiceConnection? = null

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(appContext, BiliCacheUserService::class.java),
        )
            .daemon(false)
            .processNameSuffix("bili_cache")
            .debuggable(com.mengzhen.app.BuildConfig.DEBUG)
            .version(com.mengzhen.app.BuildConfig.VERSION_CODE)
    }

    fun isManagerInstalled(): Boolean = runCatching {
        appContext.packageManager.getApplicationInfo(SHIZUKU_PACKAGE, 0)
    }.isSuccess

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = isRunning() && runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun managerLaunchIntent(): Intent? =
        appContext.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)

    suspend fun requestPermission(): Boolean = permissionMutex.withLock {
        if (hasPermission()) return@withLock true
        if (!isRunning()) return@withLock false
        suspendCancellableCoroutine { continuation ->
            val listener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    if (requestCode != REQUEST_CODE) return
                    Shizuku.removeRequestPermissionResultListener(this)
                    if (continuation.isActive) {
                        continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }
            Shizuku.addRequestPermissionResultListener(listener)
            continuation.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(listener)
            }
            runCatching { Shizuku.requestPermission(REQUEST_CODE) }
                .onFailure {
                    Shizuku.removeRequestPermissionResultListener(listener)
                    if (continuation.isActive) continuation.resume(false)
                }
        }
    }

    suspend fun scanDefaultCaches(): List<BiliCacheItem> = withContext(Dispatchers.IO) {
        val raw = connect().scanDefaultCaches()
        BiliCacheItem.listFromJson(raw)
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

    suspend fun identity(): String = withContext(Dispatchers.IO) {
        connect().identity()
    }

    private suspend fun connect(): IBiliCacheService = connectMutex.withLock {
        service?.takeIf { it.asBinder().isBinderAlive }?.let { return@withLock it }
        if (!hasPermission()) throw SecurityException("尚未获得 Shizuku/Sui 授权")

        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val connected = IBiliCacheService.Stub.asInterface(binder)
                    service = connected
                    serviceConnection = this
                    if (continuation.isActive) continuation.resume(connected)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    service = null
                    serviceConnection = null
                }
            }
            serviceConnection = connection
            continuation.invokeOnCancellation {
                runCatching {
                    Shizuku.unbindUserService(userServiceArgs, connection, false)
                }
                if (serviceConnection === connection) serviceConnection = null
            }
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (error: Throwable) {
                serviceConnection = null
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE = 71_023
    }
}
