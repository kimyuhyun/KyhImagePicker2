package com.hongslab.kyh_image_picker2

import android.Manifest
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hongslab.kyh_image_picker2.adapter.GalleryAdapter
import com.hongslab.kyh_image_picker2.databinding.ActivityKyhImagePicker2Binding
import com.hongslab.kyh_image_picker2.models.ImageVO
import com.hongslab.kyh_image_picker2.utils.GridSpacingItemDecoration
import com.hongslab.kyh_image_picker2.utils.PinchImageView
import com.hongslab.kyh_image_picker2.utils.compPopulation
import com.hongslab.kyh_image_picker2.utils.exifOrientationToDegrees
import com.hongslab.kyh_image_picker2.utils.getAllPhotos
import com.hongslab.kyh_image_picker2.utils.rotateImage
import com.hongslab.kyh_image_picker2.utils.saveImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections

class KyhImagePicker2AC : AppCompatActivity() {
    private lateinit var binding: ActivityKyhImagePicker2Binding
    private var screenWidth = 0
    private val list = arrayListOf<ImageVO>()
    private lateinit var adapter: GalleryAdapter
    private var limitCount = 1
    private var limitMessage = ""
    private var noSelectedMessage = ""
    private var isFullFrameMode = false
    private var selectedItem = ImageVO()
    private var isCameraOpen = false
    private var page: Int = -1

    private val adapterClickListener = object : GalleryAdapter.AdapterClickListener {
        override fun onClick(pos: Int) {
            Log.d("####", "${list[pos].isChoose} : ${list[pos].isToggle}")
            if (!list[pos].isToggle) {
                //DESC 정렬해서 가장 큰수를 가져온다!
                var seq: Int = Collections.max(list, compPopulation()).seq
                if (seq >= limitCount - 1) {
                    Toast.makeText(this@KyhImagePicker2AC, limitMessage, Toast.LENGTH_SHORT).show()
                    return
                }
                list[pos].isToggle = true
                seq++
                list[pos].seq = seq
            } else if (list[pos].isChoose && list[pos].isToggle) {
                list[pos].isToggle = false
                val seq: Int = list[pos].seq
                for (row in list) {
                    if (row.seq > seq) {
                        row.seq = row.seq - 1
                    }
                }
                list[pos].seq = -1
            }

            for (row in list) {
                row.isChoose = false

            }

            list[pos].isChoose = true
            showImage(list[pos])
            adapter.notifyDataSetChanged()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            CoroutineScope(Dispatchers.Main).launch {
                loadGallery()
                delay(100)
                showImage(list[0])
            }
        } else {
            // 권한이 거부된 경우 처리
            Log.d(
                "####", "Denied permissions: ${
                    permissions.filterValues { !it }.keys
                }"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = DataBindingUtil.setContentView(this, R.layout.activity_kyh_image_picker2)

        window.apply {
            window.statusBarColor = Color.parseColor("#000000")
            window.navigationBarColor = Color.parseColor("#000000")
        }

        limitCount = intent.getIntExtra("limit_count", 1)
        limitMessage = intent.getStringExtra("limit_message").toString()
        noSelectedMessage = intent.getStringExtra("no_selected_message").toString()

        binding.toolBar.title = intent.getStringExtra("title")
        setSupportActionBar(binding.toolBar)

        //백버튼임..
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_round_arrow_back_24)

        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        screenWidth = outMetrics.widthPixels
        val cellWidth = (outMetrics.widthPixels - 3) / 4

        binding.flPreview.updateLayoutParams<ViewGroup.LayoutParams> {
            Log.d("####", "width: $screenWidth")
            height = screenWidth
        }

        adapter = GalleryAdapter(this, cellWidth, adapterClickListener)
        adapter.submitList(list)

        binding.recyclerView.layoutManager = GridLayoutManager(this, 4)
        binding.recyclerView.addItemDecoration(GridSpacingItemDecoration(4, 1, false))
        binding.recyclerView.adapter = adapter


        binding.btnFullFrame.setOnClickListener {
            if (selectedItem.pinchImageView == null) {
                return@setOnClickListener
            }
            isFullFrameMode = !isFullFrameMode
            if (isFullFrameMode) {
                binding.btnFullFrame.setBackgroundResource(R.drawable.circle_accent)
                selectedItem.pinchImageView!!.setFullFrame()
            } else {
                binding.btnFullFrame.setBackgroundResource(R.drawable.circle_grey)
            }
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    page++
                    list.addAll(getAllPhotos(this@KyhImagePicker2AC, page))
                    adapter.notifyDataSetChanged()
                }
            }
        })

        // 뒤로가기 처리!
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        checkAndRequestPermissions()

//        CoroutineScope(Dispatchers.Main).launch {
//            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                // Android 13 (API 33) 이상
//                listOf(Manifest.permission.READ_MEDIA_IMAGES)
//            } else {
//                // Android 8.1 (API 27) ~ Android 12.1
//                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
//            }
//
//            val permissionResult = TedPermission
//                .create()
//                .setPermissions(*permissions.toTypedArray())
//                .check()
//            Log.d("####", "${permissionResult.deniedPermissions}")
//            Log.d("####", "permissionResult: ${permissionResult.isGranted}")
//            if (permissionResult.isGranted) {
//                loadGallery()
//                delay(100)
//                showImage(list[0])
//            }
//        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("####", "isCameraOpen: $isCameraOpen")
        if (isCameraOpen) {
            loadGallery()
            isCameraOpen = false
        }
    }

    private fun loadGallery() {
        page = 0
        list.clear()
        list.addAll(getAllPhotos(this, page))
        adapter.notifyDataSetChanged()
    }

    private fun showImage(item: ImageVO) {
        Log.d("####", "showImage " + item.path)
        selectedItem = item
        binding.flPreview.removeAllViews()

        //이미 생성되었다면..
        if (item.pinchImageView != null) {
            binding.flPreview.addView(item.pinchImageView)
            if (isFullFrameMode) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200)
                    item.pinchImageView!!.setFullFrame()
                }
            }
            return
        }
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = 2
            var image = BitmapFactory.decodeFile(item.path, options)
            if (image.width < screenWidth) {
                image = BitmapFactory.decodeFile(item.path)
            }

            // 이미지를 상황에 맞게 회전시킨다
            val exif: ExifInterface? = item.path?.let { ExifInterface(it) }
            val exifOrientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val exifDegree: Int? = exifOrientation?.let { exifOrientationToDegrees(it) }
            image = exifDegree?.let { rotateImage(image, it) }
            var scale = 0f
            if (image.width > image.height) {
                scale = screenWidth.toFloat() / image.height
            } else if (image.width < image.height) {
                scale = screenWidth.toFloat() / image.width
            }

            val pinchImageView = PinchImageView(this)
            pinchImageView.setScale(scale)
            pinchImageView.setImageBitmap(image)
            item.pinchImageView = pinchImageView
            binding.flPreview.addView(item.pinchImageView)
            if (isFullFrameMode) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200)
                    item.pinchImageView!!.setFullFrame()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @Override
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }

            R.id.action_camera -> {
                try {
                    val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val pm = packageManager
                    val info = pm.resolveActivity(i, 0)
                    val intent = Intent()
                    intent.component = ComponentName(info!!.activityInfo.packageName, info.activityInfo.name)
                    intent.action = Intent.ACTION_MAIN
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    startActivity(intent)
                    isCameraOpen = true
                } catch (e: Exception) {
                    Toast.makeText(this, "There is no default camera app.", Toast.LENGTH_SHORT).show()
                }
            }

            R.id.action_ok -> {
                val list1 = arrayListOf<ImageVO>()

                for (row in list) {
                    if (row.seq > -1) {
                        list1.add(row)
                    }
                }

                if (list1.size == 0) {
                    if (noSelectedMessage != null) {
                        Toast.makeText(this, noSelectedMessage, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }

                //ASC 정렬!
                val sortedList = list1.sortedBy { it.seq }

                val list2 = arrayListOf<Uri>()
                for (obj in sortedList) {
                    obj.pinchImageView?.buildDrawingCache()
                    val bmp = obj.pinchImageView?.drawingCache
                    val uri = bmp?.let { saveImage(this, it) }
                    if (uri != null) {
                        list2.add(uri)
                    }
                }

                val intent = Intent()
                intent.putParcelableArrayListExtra("uris", list2)
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.tool_bar_menu, menu)
        return true
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                loadGallery()
                delay(100)
                showImage(list[0])
            }
        } else {
            permissionLauncher.launch(permissions)
        }
    }
}