package com.hongslab.kyh_image_picker2.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.hongslab.kyh_image_picker2.models.ImageVO
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


fun getAllPhotos(context: Context, page: Int): ArrayList<ImageVO> {
    val projection = arrayOf(
        MediaStore.Images.ImageColumns.DATA,
        MediaStore.Images.ImageColumns.DISPLAY_NAME, // 이름
        MediaStore.Images.ImageColumns.SIZE, // 크기
        MediaStore.Images.ImageColumns.DATE_TAKEN,
        MediaStore.Images.ImageColumns.DATE_ADDED, // 추가된 날짜
        MediaStore.Images.ImageColumns._ID,
    )
    val resolver = context.contentResolver

    var selection: String? = MediaStore.Images.Media.SIZE + " > 0"
    val sortArgs = arrayOf(MediaStore.Images.ImageColumns.DATE_ADDED)
    val queryArgs = Bundle()
    queryArgs.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, sortArgs)
    queryArgs.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
    queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)

    val start = page * 80
    Log.d("####", "start: $start")
    queryArgs.putInt(ContentResolver.QUERY_ARG_OFFSET, start)
    queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, 80)

    val cursor = resolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        queryArgs,
        null
    )
    val list = arrayListOf<ImageVO>()

    if (cursor == null) {
        return list
    }

    if (cursor.moveToFirst()) {
        do {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME)
            val filePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)

            val id = cursor.getLong(idColumn)
            val filePath = cursor.getString(filePathColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getInt(sizeColumn)
            val date = cursor.getString(dateColumn)
            val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val thumbnailUri = getThumbnailUri(context, id)

            val vo = ImageVO(
                id = id,
                path = filePath,
                uri = contentUri,
                thumb = thumbnailUri,
            )
            list.add(vo)
        } while (cursor.moveToNext())
    }

    Log.d("####", "${list.size}")

    return list
}

fun getThumbnailUri(context: Context, id: Long): Bitmap? {
    val bmpOptions = BitmapFactory.Options()
    bmpOptions.inSampleSize = 1
    return MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, bmpOptions)
}


fun exifOrientationToDegrees(exifOrientation: Int): Int {
    return when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> {
            90
        }

        ExifInterface.ORIENTATION_ROTATE_180 -> {
            180
        }

        ExifInterface.ORIENTATION_ROTATE_270 -> {
            270
        }

        else -> 0
    }
}

/**
 * 이미지를 회전시킵니다.
 *
 * @param bitmap  비트맵 이미지
 * @param degrees 회전 각도
 * @return 회전된 이미지
 */
fun rotateImage(bmp: Bitmap?, degrees: Int): Bitmap? {
    var bitmap = bmp
    if (degrees != 0 && bitmap != null) {
        val m = Matrix()
        m.setRotate(degrees.toFloat(), bitmap.width.toFloat() / 2, bitmap.height.toFloat() / 2)
        try {
            val converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            if (bitmap != converted) {
                bitmap.recycle()
                bitmap = converted
            }
        } catch (ex: OutOfMemoryError) {
            // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
        }
    }
    return bitmap
}

class compPopulation : Comparator<ImageVO> {
    override fun compare(a: ImageVO, b: ImageVO): Int {
        if (a.seq < b.seq) return -1 // highest value first
        return if (a.seq === b.seq) 0 else 1
    }
}

fun saveImage(context: Context, bmp: Bitmap): Uri {
    val values = ContentValues()
    values.put(MediaStore.Images.Media.DISPLAY_NAME, "bbiribbabba" + System.currentTimeMillis() + ".png")
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/*")
    values.put(MediaStore.Images.Media.IS_PENDING, 1)
    val contentResolver: ContentResolver = context.contentResolver
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = contentResolver.insert(collection, values)
    try {
        val pfd = contentResolver.openFileDescriptor(uri!!, "w", null)
        if (pfd != null) {
            val inputStream = getImageInputStream(bmp)
            val strToByte = inputStream?.let { getBytes(it) }
            val fos = FileOutputStream(pfd.fileDescriptor)
            fos.write(strToByte)
            fos.close()
            inputStream?.close()
            pfd.close()
            contentResolver.update(uri, values, null, null)
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    values.clear()
    // 파일을 모두 write하고 다른곳에서 사용할 수 있도록 0으로 업데이트를 해줍니다.
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    contentResolver.update(uri!!, values, null, null)

    //갤러리에 추가
    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))

    return uri
}

fun getImageInputStream(bmp: Bitmap): InputStream? {
    val bytes = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val bitmapData = bytes.toByteArray()
    return ByteArrayInputStream(bitmapData)
}

fun getBytes(inputStream: InputStream): ByteArray? {
    val byteBuffer = ByteArrayOutputStream()
    val bufferSize = 1024
    val buffer = ByteArray(bufferSize)
    var len = 0
    while (inputStream.read(buffer).also { len = it } != -1) {
        byteBuffer.write(buffer, 0, len)
    }
    return byteBuffer.toByteArray()
}