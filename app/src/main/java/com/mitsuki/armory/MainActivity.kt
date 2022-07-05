package com.mitsuki.armory

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.mitsuki.armory.imagegesture.ImageGesture

class MainActivity : AppCompatActivity() {
    private var mImageGesture: ImageGesture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageView>(R.id.main_image)?.apply {
            mImageGesture = ImageGesture(this).apply {
                autoScaleGradient = floatArrayOf(1.5f, 3f)
            }

            setImageBitmap(
                assets.open("751656388574_.pic.jpg").use {
                    BitmapFactory.decodeStream(it)
                }
            )
        }
    }

    override fun onDestroy() {
        mImageGesture = null
        super.onDestroy()
    }
}