package com.hongslab.kyhimagepicker2

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import com.hongslab.kyh_image_picker2.KyhImagePicker2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.btn_open_gallery).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val result = KyhImagePicker2.of(this@MainActivity)
                    .setTitle("Title")
                    .setLimitCount(5)
                    .setLimitMessage("Limit")
                    .setNoSelectedMessage("no selected")
                    .open()
                Log.d("####", "$result")

                for (i in result.indices) {
                    when (i) {
                        0 -> findViewById<ImageView>(R.id.iv_0).setImageURI(result[i])
                        1 -> findViewById<ImageView>(R.id.iv_1).setImageURI(result[i])
                        2 -> findViewById<ImageView>(R.id.iv_2).setImageURI(result[i])
                        3 -> findViewById<ImageView>(R.id.iv_3).setImageURI(result[i])
                        4 -> findViewById<ImageView>(R.id.iv_4).setImageURI(result[i])
                    }
                }

                // KyhImagePicker2.deleteCroppedImage(this@MainActivity, result)
            }
        }
    }
}