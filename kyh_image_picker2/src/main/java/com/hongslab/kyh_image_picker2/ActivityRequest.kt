package com.hongslab.kyh_image_picker2

import android.content.Intent

class ActivityRequest(private val intent: Intent, private val listener: OnACResultListener) {
    fun getIntent(): Intent {
        return intent
    }

    fun getListener(): OnACResultListener? {
        return listener
    }
}