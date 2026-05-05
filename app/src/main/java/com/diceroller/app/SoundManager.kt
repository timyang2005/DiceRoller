package com.diceroller.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(context: Context) {
    private var soundPool: SoundPool? = null
    private var soundIds = mutableMapOf<String, Int>()
    private var enabled = true
    private var loaded = false
    private val context: Context = context.applicationContext

    init {
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(audioAttributes).build()
        soundPool?.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        generateAndLoadSounds()
    }

    private fun generateAndLoadSounds() {
        try {
            val dir = File(context.cacheDir, "dice_sounds"); if (!dir.exists()) dir.mkdirs()
            val rollLightFile = File(dir, "roll_light.wav"); val rollMediumFile = File(dir, "roll_medium.wav"); val rollHeavyFile = File(dir, "roll_heavy.wav")
            val bounceFile = File(dir, "bounce.wav"); val settleFile = File(dir, "settle.wav")
            if (!rollLightFile.exists()) {
                generateWav(rollLightFile, generateRollSound(0.15f, 200f, 400f))
                generateWav(rollMediumFile, generateRollSound(0.25f, 150f, 350f))
                generateWav(rollHeavyFile, generateRollSound(0.35f, 100f, 300f))
                generateWav(bounceFile, generateBounceSound()); generateWav(settleFile, generateSettleSound())
            }
            soundIds["roll_light"] = soundPool?.load(rollLightFile.absolutePath, 1) ?: -1
            soundIds["roll_medium"] = soundPool?.load(rollMediumFile.absolutePath, 1) ?: -1
            soundIds["roll_heavy"] = soundPool?.load(rollHeavyFile.absolutePath, 1) ?: -1
            soundIds["bounce"] = soundPool?.load(bounceFile.absolutePath, 1) ?: -1
            soundIds["settle"] = soundPool?.load(settleFile.absolutePath, 1) ?: -1
        } catch (e: Exception) { loaded = false }
    }

    private fun generateRollSound(duration: Float, startFreq: Float, endFreq: Float): ShortArray {
        val sampleRate = 22050; val numSamples = (sampleRate * duration).toInt(); val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate; val progress = i.toFloat() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            val envelope = if (progress < 0.1f) progress / 0.1f else if (progress > 0.7f) (1f - progress) / 0.3f else 1f
            val noise = (Math.random() * 2 - 1).toFloat() * 0.3f; val tone = sin(2.0 * PI * freq * t).toFloat() * 0.5f
            samples[i] = ((tone + noise) * envelope * 16000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun generateBounceSound(): ShortArray {
        val sampleRate = 22050; val numSamples = (sampleRate * 0.08f).toInt(); val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate; val progress = i.toFloat() / numSamples
            val freq = 800f - 400f * progress; val envelope = (1f - progress).let { it * it }
            samples[i] = (sin(2.0 * PI * freq * t).toFloat() * envelope * 20000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun generateSettleSound(): ShortArray {
        val sampleRate = 22050; val numSamples = (sampleRate * 0.12f).toInt(); val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate; val progress = i.toFloat() / numSamples
            val freq = 600f + 200f * sin(PI * progress); val envelope = if (progress < 0.05f) progress / 0.05f else (1f - progress)
            samples[i] = (sin(2.0 * PI * freq * t).toFloat() * envelope * 15000f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun generateWav(file: File, samples: ShortArray) {
        val sampleRate = 22050; val numChannels = 1; val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8; val blockAlign = numChannels * bitsPerSample / 8; val dataSize = samples.size * blockAlign
        val buffer = ByteArray(44 + dataSize)
        buffer[0] = 'R'.code.toByte(); buffer[1] = 'I'.code.toByte(); buffer[2] = 'F'.code.toByte(); buffer[3] = 'F'.code.toByte()
        writeInt32(buffer, 4, 36 + dataSize)
        buffer[8] = 'W'.code.toByte(); buffer[9] = 'A'.code.toByte(); buffer[10] = 'V'.code.toByte(); buffer[11] = 'E'.code.toByte()
        buffer[12] = 'f'.code.toByte(); buffer[13] = 'm'.code.toByte(); buffer[14] = 't'.code.toByte(); buffer[15] = ' '.code.toByte()
        writeInt32(buffer, 16, 16); writeInt16(buffer, 20, 1); writeInt16(buffer, 22, numChannels)
        writeInt32(buffer, 24, sampleRate); writeInt32(buffer, 28, byteRate); writeInt16(buffer, 32, blockAlign); writeInt16(buffer, 34, bitsPerSample)
        buffer[36] = 'd'.code.toByte(); buffer[37] = 'a'.code.toByte(); buffer[38] = 't'.code.toByte(); buffer[39] = 'a'.code.toByte()
        writeInt32(buffer, 40, dataSize)
        for (i in samples.indices) { val offset = 44 + i * 2; buffer[offset] = (samples[i].toInt() and 0xFF).toByte(); buffer[offset + 1] = (samples[i].toInt() shr 8 and 0xFF).toByte() }
        FileOutputStream(file).use { it.write(buffer) }
    }

    private fun writeInt32(buffer: ByteArray, offset: Int, value: Int) { buffer[offset] = (value and 0xFF).toByte(); buffer[offset + 1] = (value shr 8 and 0xFF).toByte(); buffer[offset + 2] = (value shr 16 and 0xFF).toByte(); buffer[offset + 3] = (value shr 24 and 0xFF).toByte() }
    private fun writeInt16(buffer: ByteArray, offset: Int, value: Int) { buffer[offset] = (value and 0xFF).toByte(); buffer[offset + 1] = (value shr 8 and 0xFF).toByte() }

    fun setEnabled(enabled: Boolean) { this.enabled = enabled }
    fun playRoll(forceMultiplier: Float) {
        if (!enabled || !loaded) return
        val soundName = when { forceMultiplier < 0.8f -> "roll_light"; forceMultiplier < 1.8f -> "roll_medium"; else -> "roll_heavy" }
        soundIds[soundName]?.let { soundPool?.play(it, 0.7f, 0.7f, 1, 0, 1.0f) }
    }
    fun playBounce(bounceCount: Int) {
        if (!enabled || !loaded) return
        soundIds["bounce"]?.let { soundPool?.play(it, (1.0f / (bounceCount + 1)).coerceIn(0.1f, 0.8f), (1.0f / (bounceCount + 1)).coerceIn(0.1f, 0.8f), 1, 0, (0.8f + bounceCount * 0.05f).coerceAtMost(1.5f)) }
    }
    fun playSettle() { if (!enabled || !loaded) return; soundIds["settle"]?.let { soundPool?.play(it, 0.5f, 0.5f, 1, 0, 1.0f) } }
    fun release() { soundPool?.release(); soundPool = null; soundIds.clear(); loaded = false }
}
