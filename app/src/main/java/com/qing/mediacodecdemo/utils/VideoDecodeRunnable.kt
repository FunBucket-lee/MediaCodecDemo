package com.qing.mediacodecdemo.utils

import android.media.MediaCodec
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException


class VideoDecodeRunnable(
    private val mMediaCodec: MediaCodec,
) : Runnable {

    private lateinit var mInputStream: DataInputStream

    init {
        getFileInputStream()
    }

    companion object {
        private val H264_FILE = Environment.getExternalStorageDirectory().path + "/hh264.h264"
    }

    override fun run() {
        decode()
    }

    /**
     * 获取需要解码的文件流
     */
    private fun getFileInputStream() {
        try {
            val file = File(H264_FILE)
            mInputStream = DataInputStream(FileInputStream(file))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            try {
                mInputStream.close()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
        }
    }

    private fun decode() {
        //获取一组缓存数据8个
        val inputBuffers = mMediaCodec.inputBuffers
        //解码后的数据，包含每一个buffer的元数据
        val bufferInfo = MediaCodec.BufferInfo()
        //获取缓存数据时，需要设置等待的时间（单位：毫秒）
        val timeOutUs = 10000L
        var streamBuffer: ByteArray?
        try {//返回可用的字节数组
            streamBuffer = getBytes()

            //得到可用字节数的长度
            val bytesCnt = streamBuffer.size
            //没有得到可用的数组
            if (bytesCnt == 0) {
                streamBuffer = null
            }
            //每一帧的开始位置
            var startIndex = 0
            // 定义记录剩余字节的变量
            // while(true)大括号内的内容是获取一帧，解码，然后显示；直到获取最后一帧，解码，结束
            while (true) {
                // 当剩余的字节=0或者开始的读取的字节下标大于可用的字节数时  不在继续读取
                if (bytesCnt == 0 || startIndex >= bytesCnt) {
                    break
                }
                //寻找头部帧
                var nextFrameStart = findHeadFrame(streamBuffer!!, startIndex + 2, bytesCnt)
                //找不到头部 -1
                if (nextFrameStart == -1) {
                    nextFrameStart = bytesCnt
                }
                // 得到可用的缓存区
                val inputIndex = mMediaCodec.dequeueInputBuffer(timeOutUs)
                // 有可用缓存区
                if (inputIndex > 0) {
                    val byteBuffer = inputBuffers[inputIndex]
                    byteBuffer.clear()
                    // 将可用的字节数组(一帧)，传入缓冲区
                    byteBuffer.put(streamBuffer, startIndex, nextFrameStart - startIndex)
                    // 把数据传递给解码器
                    mMediaCodec.queueInputBuffer(inputIndex, 0, nextFrameStart - startIndex, 0, 0)
                    //指定下一帧的位置
                    startIndex = nextFrameStart
                } else {
                    continue
                }

                val outputIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, timeOutUs)
                if (outputIndex > 0) {
                    mMediaCodec.releaseOutputBuffer(outputIndex, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 查找帧头部的位置
     * 在实际的H264数据帧中，往往帧前面带有00 00 00 01 或 00 00 01分隔符
     * @param bytes
     * @param start
     * @param totalSize
     * @return
     */
    private fun findHeadFrame(bytes: ByteArray, start: Int, totalSize: Int): Int {
        for (i in start until totalSize - 4) {
            if (bytes[i] == 0x00.toByte() && bytes[i + 1] == 0x00.toByte() && bytes[i + 2] == 0x00.toByte() && bytes[i + 3] == 0x01.toByte() || bytes[i] == 0x00.toByte() && bytes[i + 1] == 0x00.toByte() && bytes[i + 2] == 0x01.toByte()) {
                return i
            }
        }
        return -1
    }

    /**
     * 获得可用的字节数组
     * @return
     */
    private fun getBytes(): ByteArray {
        var len: Int
        val size = 1024

        val bos = ByteArrayOutputStream()
        var buf = ByteArray(size)
        while (mInputStream.read(buf, 0, size).also { len = it } != -1) {
            // 将读取的数据写入到字节输出流
            bos.write(buf, 0, len)
        }
        buf = bos.toByteArray()
        return buf
    }
}