package com.mengzhen.app.speech

import android.content.Context
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

private const val MODEL_ASSET_PATH = "vosk-model-small-cn-0.22"
private const val MODEL_STORAGE_PATH = "speech-model"
private const val SAMPLE_RATE = 16_000.0f

object VoskChineseModel {
    private data class Request(
        val onReady: (Model) -> Unit,
        val onError: (Exception) -> Unit,
    )

    private val lock = Any()
    private val requests = mutableListOf<Request>()
    private var model: Model? = null
    private var loading = false

    fun load(
        context: Context,
        onReady: (Model) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        synchronized(lock) {
            model?.let {
                onReady(it)
                return
            }
            requests += Request(onReady, onError)
            if (loading) return
            loading = true
        }

        LibVosk.setLogLevel(LogLevel.WARNINGS)
        StorageService.unpack(
            context.applicationContext,
            MODEL_ASSET_PATH,
            MODEL_STORAGE_PATH,
            { loadedModel ->
                val pending = synchronized(lock) {
                    model = loadedModel
                    loading = false
                    requests.toList().also { requests.clear() }
                }
                pending.forEach { it.onReady(loadedModel) }
            },
            { error ->
                val pending = synchronized(lock) {
                    loading = false
                    requests.toList().also { requests.clear() }
                }
                pending.forEach { it.onError(error) }
            },
        )
    }
}

class VoskSpeechInput(
    private val onListeningChanged: (Boolean) -> Unit,
    private val onRecognized: (String) -> Unit,
    private val onError: (Exception) -> Unit,
) : RecognitionListener {
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var released = false
    private var resultDelivered = false

    fun start(model: Model): Boolean {
        if (released || speechService != null) return false
        return try {
            resultDelivered = false
            recognizer = Recognizer(model, SAMPLE_RATE)
            speechService = SpeechService(recognizer, SAMPLE_RATE).also {
                it.startListening(this)
            }
            onListeningChanged(true)
            true
        } catch (error: Exception) {
            releaseCurrent(cancel = true)
            onError(error)
            false
        }
    }

    fun stop() {
        releaseCurrent(cancel = false)
    }

    fun release() {
        released = true
        releaseCurrent(cancel = true)
    }

    override fun onResult(hypothesis: String) {
        deliver(resultText(hypothesis, "text"))
    }

    override fun onFinalResult(hypothesis: String) {
        deliver(resultText(hypothesis, "text"))
        releaseCurrent(cancel = true)
    }

    override fun onPartialResult(hypothesis: String) = Unit

    override fun onError(exception: Exception) {
        releaseCurrent(cancel = true)
        onError(exception)
    }

    override fun onTimeout() {
        stop()
    }

    private fun deliver(text: String) {
        if (text.isBlank() || resultDelivered) return
        resultDelivered = true
        onRecognized(text)
        releaseCurrent(cancel = true)
    }

    private fun releaseCurrent(cancel: Boolean) {
        val service = speechService
        speechService = null
        if (service != null) {
            if (cancel) service.cancel() else service.stop()
            service.shutdown()
        }
        recognizer?.close()
        recognizer = null
        onListeningChanged(false)
    }

    private fun resultText(hypothesis: String, key: String): String =
        runCatching { JSONObject(hypothesis).optString(key).trim() }
            .getOrDefault("")
}
