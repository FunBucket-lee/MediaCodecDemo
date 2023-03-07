package com.qing.mediacodecdemo.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.FileOutputStream

class AudioDecodeRunnable(
    val extractor: MediaExtractor,
    val audioTrack: Int,
    val mPcmFilePath: String,
    val mListener: AudioCodec.DecodeOverListener
) : Runnable {

    companion object {
        private const val TAG = "AudioDecodeRunnable"
        private const val TIMEOUT_USEC = 0L
    }

    override fun run() {
        try {
            // 直接从MP3音频文件中得到音轨的MediaFormat
            val trackFormat = extractor.getTrackFormat(audioTrack)
            // 初始化音频解码器,并配置解码器属性
            val audioCodec =
                MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME)!!)
            audioCodec.configure(trackFormat, null, null, 0)

            //启动MediaCodec,等待传入数据
            audioCodec.start()

            // 用于描述解码得到的byte[]数据的相关信息
            val decodeBufferInfo = MediaCodec.BufferInfo()
            // 用于描述输入数据的byte[]数据的相关信息
            val inputInfo = MediaCodec.BufferInfo()
            // 整体输入结束标记
            var codeOver = false
            var inputDone = false

            val fos = FileOutputStream(mPcmFilePath)
            while (!codeOver) {
                if (!inputDone) {
                    /**
                     * 延迟 TIME_US 等待拿到空的 input buffer下标，单位为 us
                     * -1 表示一直等待，知道拿到数据，0 表示立即返回
                     */
                    val bufferIndex = audioCodec.dequeueInputBuffer(TIMEOUT_USEC)
                    if (bufferIndex >= 0) {
                        val inputBuffer = audioCodec.getInputBuffer(bufferIndex)
                        inputBuffer?.apply {
                            inputBuffer.clear()
                            val sampleData = extractor.readSampleData(inputBuffer, 0)
                            if (sampleData < 0) {
                                //代表数据已经读取完毕
                                audioCodec.queueInputBuffer(
                                    bufferIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            } else {
                                inputInfo.apply {
                                    offset = 0
                                    size = sampleData
                                    presentationTimeUs = extractor.sampleTime

                                    Log.e(
                                        TAG,
                                        "run: 往解码器写入数据，当前时间戳为==》${inputInfo.presentationTimeUs}",
                                    )
                                    audioCodec.queueInputBuffer(bufferIndex, 0, 0, 0L, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}