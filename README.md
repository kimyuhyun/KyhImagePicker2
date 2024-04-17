# KyhImagePicker2
instagram style image picker

![KakaoTalk_Photo_2022-08-09-10-56-40 002](https://user-images.githubusercontent.com/29136588/183547138-8cf9168c-7a13-451e-9a01-cdf043447be0.jpeg)
![KakaoTalk_Photo_2022-08-09-10-56-39 001](https://user-images.githubusercontent.com/29136588/183547144-9315d0d7-8f1d-4e33-a916-12e915e20bed.jpeg)

- The image returns uri path as a cropped shooting image.

```
dependencyResolutionManagement {
    repositories {
        ...
        maven("https://jitpack.io")
    }
}
```

```
dependencies {
    implementation ("com.github.kimyuhyun:KyhImagePicker2:1.0.3")
}
```


- Insert the code below into the button to run the image picker.
```
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
```


