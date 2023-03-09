package com.qing.mediacodecdemo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.qing.mediacodecdemo.ui.theme.MediaCodecDemoTheme
import com.qing.mediacodecdemo.utils.AudioCodec

class MainActivity : ComponentActivity() {


    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

        private fun verifyStoragePermissions(activity: Activity) {
            try {//检查是否有读写权限
                val permission = ActivityCompat.checkSelfPermission(
                    activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // 没有写的权限，去申请写的权限，会弹出对话框
                    ActivityCompat.requestPermissions(
                        activity, PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyStoragePermissions(this)
        setContent {
            MaterialTheme {
                Greeting("开始转换")
            }
        }
    }

    @Composable
    fun Greeting(name: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                extractedAudio()
            }) {
                Text(text = name)
            }
            Button(onClick = {
                val intent = Intent(this@MainActivity,VideoDecodeActivity::class.java)
                startActivity(intent)

            }) {
                Text(text = "开始编解码视频")
            }
        }
    }


    private fun extractedAudio() {
        val aacPath = Environment.getExternalStorageDirectory().path + "/Dawn_clip.aac"
        val pcmPath = Environment.getExternalStorageDirectory().path + "/Dawn_clip.pcm"
        val aacResultPath =
            Environment.getExternalStorageDirectory().path + "/Dawn_clip1.aac"

        AudioCodec.getPCMFromAudio(
            aacPath,
            pcmPath,
            object : AudioCodec.AudioDecodeListener {
                override fun decodeOver() {
                    Log.d(TAG, "decodeOver: 音频解码完成")

                    //解码完成之后需要编码
                    AudioCodec.pcmToAudio(
                        pcmPath,
                        aacResultPath,
                        object : AudioCodec.AudioDecodeListener {
                            override fun decodeOver() {
                                Log.d(TAG, "decodeOver: 音频编码完成")
                            }

                            override fun decodeFail() {
                                Log.d(TAG, "decodeFail: 音频编码失败")
                            }

                        })
                }

                override fun decodeFail() {
                    Log.d(TAG, "decodeFail: 音频解码失败")
                }

            })
    }

    @Preview(showBackground = false)
    @Composable
    fun GreetingPreview() {
        MediaCodecDemoTheme {
            Greeting("Android")
        }
    }
}