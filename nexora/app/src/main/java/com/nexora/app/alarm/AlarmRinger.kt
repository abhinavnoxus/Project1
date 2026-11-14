package com.nexora.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Audio + haptics for one ring. Owns exactly one [MediaPlayer] and one vibrator, and is
 * idempotent: [stop] twice is safe, [start] twice does nothing.
 *
 * Fade-in is a short coroutine ramp; it is cancelled the moment [stop] is called so a
 * dismiss is never delayed by the ramp.
 */
class AlarmRinger(private val context: Context) {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var fadeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    @Volatile private var started = false

    fun start(ringtoneUri: String?, vibrate: Boolean, fadeInSeconds: Int, volumeFraction: Float = 1f) {
        if (started) return
        started = true
        startAudio(ringtoneUri, fadeInSeconds, volumeFraction)
        if (vibrate) startVibration()
    }

    private fun startAudio(uriString: String?, fadeInSeconds: Int, volumeFraction: Float) {
        val uri: Uri = resolveRingtone(uriString)
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(context, uri)
                isLooping = true
                prepare()
                val target = volumeFraction.coerceIn(0.05f, 1f)
                if (fadeInSeconds <= 0) {
                    setVolume(target, target)
                    start()
                } else {
                    setVolume(0f, 0f)
                    start()
                    fadeJob = scope.launch {
                        val steps = (fadeInSeconds * 10).coerceIn(1, 200)
                        repeat(steps) { i ->
                            val v = target * (i + 1) / steps
                            runCatching { player?.setVolume(v, v) }
                            delay(100L)
                        }
                    }
                }
            }
        }.onFailure {
            // Fall back to the system default alarm sound rather than ringing silently.
            Log.w(TAG, "custom ringtone failed, using default", it)
            runCatching {
                player?.release()
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    isLooping = true
                    prepare()
                    start()
                }
            }.onFailure { Log.e(TAG, "default ringtone also failed", it) }
        }
    }

    private fun resolveRingtone(uriString: String?): Uri {
        if (!uriString.isNullOrBlank()) {
            val candidate = Uri.parse(uriString)
            val ok = runCatching { context.contentResolver.openAssetFileDescriptor(candidate, "r") != null }
                .getOrDefault(false)
            if (ok) return candidate
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val pattern = longArrayOf(0, 600, 400, 600, 800)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        }
    }

    /** Stops sound and vibration. Never throws, never blocks on animation. */
    fun stop() {
        if (!started) return
        started = false
        fadeJob?.cancel()
        fadeJob = null
        runCatching {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private companion object {
        const val TAG = "NexoraRinger"
    }
}
