package com.hongslab.kyh_image_picker2

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object KyhImagePicker2 {
    fun of(context: Context): Builder = Builder(context)

    fun deleteCroppedImage(context: Context, list: ArrayList<Uri>) {
        for (uri in list) {
            try {
                val deletedRows = context.contentResolver.delete(uri, null, null)
                if (deletedRows > 0) {
                    Log.d("####", "delete ok")
                }

                //갤러리에 추가
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Builder(private val context: Context) {
        private val intent: Intent = Intent()


        fun setTitle(title: String): Builder {
            intent.putExtra("title", title)
            return this
        }

        fun setLimitCount(limitCount: Int): Builder {
            intent.putExtra("limit_count", limitCount)
            return this
        }

        fun setLimitMessage(msg: String): Builder {
            intent.putExtra("limit_message", msg)
            return this
        }

        fun setNoSelectedMessage(msg: String): Builder {
            intent.putExtra("no_selected_message", msg)
            return this
        }

        suspend fun open() = suspendCoroutine {
            intent.setClass(context, KyhImagePicker2AC::class.java)
            ProxyAC.startActivityForResult(context, intent, object : OnACResultListener {
                override fun onACResult(uris: ArrayList<Uri>) {
                    it.resume(uris)
                }
            })
        }
    }


}