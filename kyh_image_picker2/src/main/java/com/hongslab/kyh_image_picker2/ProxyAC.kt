package com.hongslab.kyh_image_picker2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.util.ArrayDeque
import java.util.Deque
import java.util.Random

class ProxyAC : Activity() {
    companion object {
        private var activityRequestStack: Deque<ActivityRequest>? = null

        fun startActivityForResult(context: Context, intent: Intent, listener: OnACResultListener) {
            if (activityRequestStack == null) {
                activityRequestStack = ArrayDeque<ActivityRequest>()
            }
            val activityRequest = ActivityRequest(intent, listener)
            activityRequestStack!!.push(activityRequest)
            val tempIntent = Intent(context, ProxyAC::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(tempIntent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        if (activityRequestStack == null) {
            finish()
            return
        }
        val activityRequest = activityRequestStack!!.peek()
        val intent = activityRequest.getIntent()
        super.startActivityForResult(intent, 60000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 60000 && resultCode == RESULT_OK) {
            val uris: ArrayList<Uri>? = data?.getParcelableArrayListExtra("uris")
            val list = arrayListOf<Uri>()
            if (uris != null) {
                for (uri in uris) {
                    list.add(uri)
                }
                val activityRequest = activityRequestStack?.pop()
                val listener: OnACResultListener? = activityRequest?.getListener()
                listener?.onACResult(list)
                if (activityRequestStack?.size == 0) {
                    activityRequestStack = null
                }
            }
        }
        finish()
    }

}