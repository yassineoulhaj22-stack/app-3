package com.zonerush.game

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max

/** Chat vocal "push-to-talk" (maintenir le bouton micro), fonctionne sans internet. */
class Voice {
    private val rate = 16000
    @Volatile private var talking = false
    @Volatile private var running = true
    private val queue = LinkedBlockingQueue<ByteArray>()

    init { startPlayback() }

    @SuppressLint("MissingPermission")
    fun startTalk(net: Net?) {
        if (talking || net == null) return
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return
        val rec: AudioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, rate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(min, 2560)
            )
        } catch (e: Exception) {
            return
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return
        }
        talking = true
        rec.startRecording()
        Thread {
            val buf = ByteArray(1280)
            while (talking) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) net.sendVoice(buf, n)
            }
            try { rec.stop() } catch (e: Exception) { }
            rec.release()
        }.start()
    }

    fun stopTalk() { talking = false }

    fun play(d: ByteArray) {
        if (queue.size < 50) queue.offer(d)
    }

    private fun startPlayback() {
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(min, 4096))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        Thread {
            while (running) {
                val d = queue.poll(200, TimeUnit.MILLISECONDS) ?: continue
                track.write(d, 0, d.size)
            }
            track.release()
        }.start()
    }

    fun release() {
        talking = false
        running = false
    }
}
