package com.qing.mediacodecdemo

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.qing.mediacodecdemo.databinding.ActivityMideoDecodeBinding
import com.qing.mediacodecdemo.utils.VideoDecodeRunnable


class VideoDecodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMideoDecodeBinding
    private lateinit var mMediaCodec: MediaCodec


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMideoDecodeBinding.inflate(LayoutInflater.from(this))
        this.setTheme(R.style.Theme_MediaCodecDemo)
        setContentView(binding.root)

        initMediaCodec()
    }

    private fun initMediaCodec() {
        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                try {
                    mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                    // 使用MediaFormat初始化编码器，设置宽，高
                    val format =
                        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
                    //设置帧率
                    format.setInteger(MediaFormat.KEY_FRAME_RATE, 40)
                    //配置编码器
                    mMediaCodec.configure(format, Surface(surface), null, 0)
                    startDecodeThread()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

        }
    }

    private fun startDecodeThread() {
        mMediaCodec.start()
        Thread(VideoDecodeRunnable(mMediaCodec = mMediaCodec)).start()
    }
}