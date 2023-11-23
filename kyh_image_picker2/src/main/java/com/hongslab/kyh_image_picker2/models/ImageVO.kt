package com.hongslab.kyh_image_picker2.models

import android.graphics.Bitmap
import android.net.Uri
import com.hongslab.kyh_image_picker2.utils.PinchImageView

data class ImageVO(
    var seq: Int = -1,
    var id: Long? = null,
    var uri: Uri? = null,
    var path: String? = null,
    var isToggle: Boolean = false,
    var isChoose: Boolean = false,
    var pinchImageView: PinchImageView? = null

)
