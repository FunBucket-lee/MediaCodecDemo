package com.qing.mediacodecdemo.utils

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Log


class AudioCodec {
    companion object {
        private const val TAG = "AudioCodec"
        private val handler = Handler(Looper.getMainLooper())

        /**
         * 将音频文件解码成原始的PCM数据
         * @param audioPath         音频文件目录
         * @param audioSavePath     pcm文件保存位置
         * @param listener
         */
        @JvmStatic
        private fun getPCMFromAudio(
            audioPath: String,
            audioSavePath: String,
            listener: DecodeOverListener
        ) {
            // 此类可分离视频文件的音轨和视频轨道
            val mediaExtractor = MediaExtractor()
            // 音频MP3文件其实只有一个音轨
            var audioTrack = -1
            // 判断音频文件是否有音频音轨
            var hasAudio = false

            try {
                mediaExtractor.setDataSource(audioPath)
                val mediaFormat: MediaFormat
                for (i in 0 until mediaExtractor.trackCount) {
                    val mimeType = mediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                    if (mimeType != null && mimeType.startsWith("audio/")) {
                        audioTrack = i
                        hasAudio = true
                        break
                    }
                }

                if (hasAudio) {
                    mediaExtractor.selectTrack(audioTrack)
                    //开启新线程原始音视频解码
                    Thread {

                    }.start()
                } else {
                    Log.e(TAG, "getPCMFromAudio: 音频文件没有音轨")
                    listener.decodeFail()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * pcm文件转音频
         * @param pcmPath       pcm文件目录
         * @param audioPath     音频文件目录
         * @param listener
         */
        @JvmStatic
        private fun pcmToAudio(
            pcmPath: String,
            audioSavePath: String,
            listener: AudioDecodeListener
        ) {
            Thread {}.start()
        }

        /**
         * 写入ADTS头部数据
         * @param packet
         * @param packetLen
         */
        @JvmStatic
        private fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
            val profile = 2 // AAC LC
            val freqIdx = 4 // 44.1KHz
            val chanCfg = 2 // CPE

            packet[0] = 0xff.toByte()
            packet[1] = 0xf9.toByte()
            packet[2] = (((profile - 1) shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
            packet[3] = (((chanCfg and 3) shl 6) + (packetLen shr 11)).toByte()
            packet[4] = ((packetLen and 0x7FF) shr 3).toByte()
            packet[5] = (((packetLen and 7) shl 5) + 0x1F).toByte()
            packet[6] = 0xFC.toByte()
        }
    }

    /**
     * 音频编码监听器：监听是否编码成功
     */
    interface DecodeOverListener {
        fun decodeIsOver()
        fun decodeFail()
    }

    /**
     * 音频解码监听器：监听是否解码成功
     */
    interface AudioDecodeListener {
        fun decodeOver()
        fun decodeFail()
    }
}