@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.mengzhen.app.audio

import androidx.media3.common.C
import androidx.media3.common.audio.GainProcessor

/** Supplies the source Media3 gain processor with one continuous playback envelope. */
internal class PlaybackGainEnvelope : GainProcessor.GainProvider {
    @Volatile
    private var envelope: Envelope = Envelope.Unity

    @Volatile
    private var lastObservedSample = 0L

    fun startFadeIn(durationMs: Long, elapsedMs: Long) {
        lastObservedSample = 0L
        envelope = Envelope.FadeIn(
            durationMs = durationMs.coerceAtLeast(1L),
            elapsedMs = elapsedMs.coerceAtLeast(0L),
        )
    }

    fun finishFadeIn() {
        if (envelope is Envelope.FadeIn) envelope = Envelope.Unity
    }

    fun startFadeOut(durationMs: Long) {
        envelope = Envelope.FadeOut(
            startSample = lastObservedSample,
            durationMs = durationMs.coerceAtLeast(1L),
        )
    }

    fun reset() {
        lastObservedSample = 0L
        envelope = Envelope.Unity
    }

    override fun getGainFactorAtSamplePosition(samplePosition: Long, sampleRate: Int): Float {
        val current = envelope
        if (current is Envelope.Unity || samplePosition - lastObservedSample >= OBSERVE_INTERVAL) {
            lastObservedSample = samplePosition
        }
        return when (current) {
            Envelope.Unity -> 1f
            is Envelope.FadeIn -> {
                val duration = current.durationMs.toSamples(sampleRate).coerceAtLeast(1L)
                val position = current.elapsedMs.toSamples(sampleRate) + samplePosition
                if (position >= duration) {
                    envelope = Envelope.Unity
                    1f
                } else {
                    position.toFloat() / duration
                }
            }
            is Envelope.FadeOut -> {
                val duration = current.durationMs.toSamples(sampleRate).coerceAtLeast(1L)
                val position = (samplePosition - current.startSample).coerceAtLeast(0L)
                ((duration - position.coerceAtMost(duration)).toFloat() / duration)
            }
        }
    }

    override fun isUnityUntil(samplePosition: Long, sampleRate: Int): Long {
        lastObservedSample = samplePosition
        return when (val current = envelope) {
            Envelope.Unity -> if (samplePosition == 0L) 1L else C.TIME_END_OF_SOURCE
            is Envelope.FadeIn -> {
                val duration = current.durationMs.toSamples(sampleRate).coerceAtLeast(1L)
                val position = current.elapsedMs.toSamples(sampleRate) + samplePosition
                if (position >= duration) C.TIME_END_OF_SOURCE else C.TIME_UNSET
            }
            is Envelope.FadeOut -> when {
                samplePosition < current.startSample -> current.startSample
                samplePosition == current.startSample -> samplePosition + 1L
                else -> C.TIME_UNSET
            }
        }
    }

    private fun Long.toSamples(sampleRate: Int): Long = this * sampleRate / 1_000L

    private sealed interface Envelope {
        data object Unity : Envelope
        data class FadeIn(val durationMs: Long, val elapsedMs: Long) : Envelope
        data class FadeOut(val startSample: Long, val durationMs: Long) : Envelope
    }

    private companion object {
        const val OBSERVE_INTERVAL = 1_024L
    }
}
