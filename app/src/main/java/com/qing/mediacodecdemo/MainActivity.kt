package com.qing.mediacodecdemo

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.qing.mediacodecdemo.ui.theme.MediaCodecDemoTheme

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
                TODO("Not yet implemented")
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
        Column {
            Button(onClick = {
                Log.d(TAG, "Greeting: ")
            }) {
                Text(text = name)
            }
        }
    }

    @Preview(showBackground = false)
    @Composable
    fun GreetingPreview() {
        MediaCodecDemoTheme {
            Greeting("Android")
        }
    }
}