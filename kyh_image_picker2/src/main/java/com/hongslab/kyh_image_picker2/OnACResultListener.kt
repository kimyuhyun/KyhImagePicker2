package com.hongslab.kyh_image_picker2

import android.content.Intent
import android.net.Uri

interface OnACResultListener {
    fun onACResult(uris: ArrayList<Uri>?)
}