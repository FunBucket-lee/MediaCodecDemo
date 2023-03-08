package com.qing.mediacodecdemo.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * 音频解码过程
 */
class AudioDecodeRunnable(
    private val extractor: MediaExtractor,
    private val audioTrack: Int,
    savePath: String,
    listener: AudioCodec.DecodeOverListener
) :
    Runnable {
    private val mListener: AudioCodec.DecodeOverListener
    private val mPcmFilePath: String

    init {
        mListener = listener
        mPcmFilePath = savePath
    }

    override fun run() {
        try {
            // 直接从MP3音频文件中得到音轨的MediaFormat
            val format = extractor.getTrackFormat(audioTrack)
            // 初始化音频解码器,并配置解码器属性
            val audioCodec =
                MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            audioCodec.configure(format, null, null, 0)

            // 启动MediaCodec，等待传入数据
            audioCodec.start()
            val inputBuffers = audioCodec.inputBuffers // 获取需要编码数据的输入流队列，返回的是一个ByteBuffer数组
            var outputBuffers = audioCodec.outputBuffers // 获取编解码之后的数据输出流队列，返回的是一个ByteBuffer数组
            val decodeBufferInfo = MediaCodec.BufferInfo() // 用于描述解码得到的byte[]数据的相关信息
            val inputInfo = MediaCodec.BufferInfo() // 用于描述输入数据的byte[]数据的相关信息
            var codeOver = false
            val inputDone = false // 整体输入结束标记
            val fos = FileOutputStream(mPcmFilePath)
            while (!codeOver) {
                if (!inputDone) {
                    for (i in inputBuffers.indices) {
                        // 从输入流队列中取数据进行操作
                        // 返回用于填充有效数据的输入buffer的索引，如果当前没有可用的buffer，则返回-1
                        val inputIndex = audioCodec.dequeueInputBuffer(TIMEOUT_USE.toLong())
                        if (inputIndex >= 0) {
                            // 从分离器拿出输入，写入解码器
                            // 拿到inputBuffer
                            val inputBuffer = inputBuffers[inputIndex]
                            // 将position置为0，并不清除buffer内容
                            inputBuffer.clear()
                            val sampleSize = extractor.readSampleData(
                                inputBuffer,
                                0
                            ) // 将MediaExtractor读取数据到inputBuffer
                            if (sampleSize < 0) { // 表示所有数据已经读取完毕
                                audioCodec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            } else {
                                inputInfo.offset = 0
                                inputInfo.size = sampleSize
                                inputInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME
                                inputInfo.presentationTimeUs = extractor.sampleTime
                                Log.e(
                                    TAG,
                                    "往解码器写入数据，当前时间戳：" + inputInfo.presentationTimeUs
                                )
                                // 通知MediaCodec解码刚刚传入的数据
                                audioCodec.queueInputBuffer(
                                    inputIndex,
                                    inputInfo.offset,
                                    sampleSize,
                                    inputInfo.presentationTimeUs,
                                    0
                                )
                                // 读取下一帧数据
                                extractor.advance()
                            }
                        }
                    }
                }


                // dequeueInputBuffer dequeueOutputBuffer 返回值解释
                // INFO_TRY_AGAIN_LATER=-1 等待超时
                // INFO_OUTPUT_FORMAT_CHANGED=-2 媒体格式更改
                // INFO_OUTPUT_BUFFERS_CHANGED=-3 缓冲区已更改（过时）
                // 大于等于0的为缓冲区数据下标
                var decodeOutputDone = false // 整体解码结束标记
                var chunkPCM: ByteArray
                while (!decodeOutputDone) {
                    val outputIndex =
                        audioCodec.dequeueOutputBuffer(decodeBufferInfo, TIMEOUT_USE.toLong())
                    if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // 没有可用的解码器
                        decodeOutputDone = true
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        outputBuffers = audioCodec.outputBuffers
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        audioCodec.outputFormat
                    } else if (outputIndex < 0) {
                    } else {
                        val outputBuffer: ByteBuffer? =
                            audioCodec.getOutputBuffer(outputIndex)
                        chunkPCM = ByteArray(decodeBufferInfo.size)
                        outputBuffer!![chunkPCM]
                        outputBuffer.clear()
                        fos.write(chunkPCM) //数据写入文件中
                        fos.flush()
                        Log.e(
                            TAG,
                            "释放输出流缓冲区：$outputIndex"
                        )
                        audioCodec.releaseOutputBuffer(outputIndex, false)
                        if (decodeBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) { // 编解码结束
                            extractor.release()
                            audioCodec.stop()
                            audioCodec.release()
                            codeOver = true
                            decodeOutputDone = true
                        }
                    }
                }
            }
            fos.close()
            mListener.decodeIsOver()
            mListener.decodeIsOver()
        } catch (e: IOException) {
            e.printStackTrace()
            mListener.decodeFail()
        }
    }

    companion object {
        private const val TAG = "AudioDecodeRunnable"
        const val TIMEOUT_USE = 0
    }
}