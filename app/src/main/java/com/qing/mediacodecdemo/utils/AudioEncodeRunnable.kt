package com.qing.mediacodecdemo.utils

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * 音频编码过程
 */
class AudioEncodeRunnable(
    private val pcmPath: String,
    private val audioPath: String,
    listener: AudioCodec.AudioDecodeListener?
) :
    Runnable {
    private val mListener: AudioCodec.AudioDecodeListener?

    init {
        mListener = listener
    }

    override fun run() {
        try {
            if (!File(pcmPath).exists()) { // pcm文件目录不存在
                mListener?.decodeFail()
                return
            }
            val fis = FileInputStream(pcmPath)
            val buffer = ByteArray(8 * 1024)
            var allAudioBytes: ByteArray
            var inputIndex: Int
            var inputBuffer: ByteBuffer
            var outputIndex: Int
            var outputBuffer: ByteBuffer
            var chunkAudio: ByteArray
            var outBitSize: Int
            var outPacketSize: Int

            // 初始化编码格式   mimetype  采样率  声道数
            val encodeFormat =
                MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2)
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
            encodeFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024)

            // 初始化编码器
            val mediaEncode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaEncode.start()
            val encodeInputBuffers = mediaEncode.inputBuffers
            val encodeOutputBuffers = mediaEncode.outputBuffers
            val encodeBufferInfo = MediaCodec.BufferInfo()

            // 初始化文件写入流
            val fos = FileOutputStream(File(audioPath))
            val bos = BufferedOutputStream(fos, 500 * 1024)
            var isReadEnd = false
            while (!isReadEnd) {
                for (i in encodeInputBuffers.indices - 1) { // 减掉1很重要，不要忘记
                    if (fis.read(buffer) != -1) {
                        allAudioBytes = buffer.copyOf(buffer.size)
                    } else {
                        Log.e(TAG, "文件读取完成")
                        isReadEnd = true
                        break
                    }
                    Log.e(TAG, "读取文件并写入编码器" + allAudioBytes.size)
                    // 从输入流队列中取数据进行编码操作
                    inputIndex = mediaEncode.dequeueInputBuffer(-1)
                    inputBuffer = encodeInputBuffers[inputIndex]
                    inputBuffer.clear()
                    inputBuffer.limit(allAudioBytes.size)
                    inputBuffer.put(allAudioBytes) // 将pcm数据填充给inputBuffer
                    // 在指定索引处填充输入buffer后，使用queueInputBuffer将buffer提交给组件。
                    mediaEncode.queueInputBuffer(inputIndex, 0, allAudioBytes.size, 0, 0) // 开始编码
                }

                // 从输入流队列中取数据进行编码操作
                outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000)
                while (outputIndex >= 0) {
                    // 从解码器中取出数据
                    outBitSize = encodeBufferInfo.size
                    outPacketSize = outBitSize + 7 // 7为adts头部大小
                    outputBuffer = encodeOutputBuffers[outputIndex] // 拿到输出的buffer
                    outputBuffer.position(encodeBufferInfo.offset)
                    outputBuffer.limit(encodeBufferInfo.offset + outBitSize)
                    chunkAudio = ByteArray(outPacketSize)
                    AudioCodec.addADTStoPacket(chunkAudio, outPacketSize) // 添加ADTS
                    outputBuffer[chunkAudio, 7, outBitSize] // 将编码得到的AAC数据取出到byte[]中，偏移量为7
                    outputBuffer.position(encodeBufferInfo.offset)
                    Log.e(TAG, "编码成功并写入文件" + chunkAudio.size)
                    bos.write(chunkAudio, 0, chunkAudio.size) // 将文件保存在sdcard中
                    bos.flush()
                    mediaEncode.releaseOutputBuffer(outputIndex, false)
                    outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000)
                }
            }
            mediaEncode.stop()
            mediaEncode.release()
            fos.close()
            mListener?.decodeOver()
        } catch (e: IOException) {
            e.printStackTrace()
            mListener?.decodeFail()
        }
    }

    companion object {
        private const val TAG = "AudioEncodeRunnable"
    }
}