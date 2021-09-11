package com.mitsuki.armory.imagegesture

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import kotlin.math.*

@SuppressLint("ClickableViewAccessibility")
open class ImageGesture(protected val mImageView: ImageView) : AllGesture(), View.OnTouchListener,
    View.OnLayoutChangeListener {

    var startType = StartType.AUTO_LEFT
        set(startType) {
            field = startType
            initBase()
        }

    private val mDrawMatrix = Matrix()
    private val mBaseMatrix = Matrix()
    private val mDecoMatrix = Matrix()
    private val mDisplayRect = RectF()
    private val mMatrixValues = FloatArray(9)

    private var mScaleUpAnimation: Animation? = null
    private var mScaleDownAnimation: Animation? = null

    //启用mSlideType阈值，除非超过阈值，否则都为NONE mode
    private var mScaleThreshold = 0.0f


    private val mCurrentFlingRunnable = FlingAnimation()
    private val mScaleGestureDetector = ScaleGestureDetector(mImageView.context, this)
    private val mGestureDetector = GestureDetector(mImageView.context, this)

    companion object {
        private const val MAX_SCALE = 3.0f
        private const val SCALE_DURATION = 300
    }

    init {
        mImageView.scaleType = ImageView.ScaleType.MATRIX
        mImageView.setOnTouchListener(this)
        mImageView.addOnLayoutChangeListener(this)
    }

    /** GestureListener ***************************************************************************/
    final override fun onScale(detector: ScaleGestureDetector): Boolean {
        return detector.run {
            var handled = false
            val currentScale = mDecoMatrix.getScale()
            if (scaleFactor > 1f && currentScale < MAX_SCALE) {
                if (currentScale * scaleFactor > MAX_SCALE) {
                    mDecoMatrix.postScale(
                        MAX_SCALE / currentScale, MAX_SCALE / currentScale,
                        focusX, focusY
                    )
                } else {
                    mDecoMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                }
                updateImageMatrix()
                handled = true
            }

            if (scaleFactor < 1f && currentScale > 1f) {
                if (currentScale * scaleFactor < 1f) {
                    mDecoMatrix.postScale(
                        1f / currentScale, 1f / currentScale, focusX, focusY
                    )
                } else {
                    mDecoMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                }
                updateImageMatrix()
                handled = true
            }
            handled
        }
    }

    final override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float
    ): Boolean {
        var handled = false
        val viewWidth = mImageView.width
        val viewHeight = mImageView.height
        finalMatrix.getDisplayRect()

        if (distanceX > 0) {
            if (mDisplayRect.right > viewWidth) handled = true
            else {
                if (abs((e2?.x ?: 0f) - (e1?.x ?: 0f)) - abs((e2?.y ?: 0f) - (e1?.y ?: 0f)) > 64) {
                    mImageView.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        } else {
            if (mDisplayRect.left < 0f) handled = true
            else {
                if (abs((e2?.x ?: 0f) - (e1?.x ?: 0f)) - abs((e2?.y ?: 0f) - (e1?.y ?: 0f)) > 64) {
                    mImageView.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        }

        if (distanceY > 0) {
            if (mDisplayRect.bottom > viewHeight) handled = true
            else {
                if (abs((e2?.y ?: 0f) - (e1?.y ?: 0f)) - abs((e2?.x ?: 0f) - (e1?.x ?: 0f)) > 64) {
                    mImageView.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        } else {
            if (mDisplayRect.top < 0f) handled = true
            else {
                if (abs((e2?.y ?: 0f) - (e1?.y ?: 0f)) - abs((e2?.x ?: 0f) - (e1?.x ?: 0f)) > 64) {
                    mImageView.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        }

        if (handled) {
            mDecoMatrix.postTranslate(-distanceX, -distanceY)
            updateImageMatrix()
        }
        return handled
    }

    final override fun onFling(
        e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float
    ): Boolean {
        //velocityX < 0 手指向左滑动
        //velocityY > 0 手指向右滑动
        //velocityY > 0 手指向下滑动
        //velocityY < 0 手指向上滑动
        mCurrentFlingRunnable.fling(-velocityX.roundToInt(), -velocityY.roundToInt())
        mImageView.post(mCurrentFlingRunnable)
        return true
    }

    final override fun onDoubleTap(e: MotionEvent): Boolean {
        e.apply {
            val currentScale = mDecoMatrix.getScale()
            if (currentScale > 1f) {
                if (!mScaleDownAnimation.isAnimationRunning())
                    mScaleDownAnimation = startScaleDown(e.x, e.y, currentScale)
            } else {
                if (!mScaleUpAnimation.isAnimationRunning())
                    mScaleUpAnimation = startScaleUp(e.x, e.y, currentScale)
            }
        }
        return true
    }

    /** View.OnTouchListener **********************************************************************/
    final override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mCurrentFlingRunnable.finish()
                mImageView.clearAnimation()
                v?.parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                v?.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        var handled = false

        if (mScaleGestureDetector.onTouchEvent(event)) {
            handled = true
        }

        if (mGestureDetector.onTouchEvent(event)) {
            handled = true
        }

        return handled
    }

    /** View.OnLayoutChangeListener ***************************************************************/
    override fun onLayoutChange(
        v: View?, left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
    ) {
        initBase()
    }

    private fun initBase() {
        val drawable = mImageView.drawable ?: return
        val viewWidth = mImageView.width  //view的宽度
        val viewHeight = mImageView.height  //view的高度
        val sourceWidth = drawable.intrinsicWidth  //源图宽度
        val sourceHeight = drawable.intrinsicHeight  //源图高度
        mBaseMatrix.reset()  //初始化矩阵
        val widthRatio = viewWidth.toFloat() / sourceWidth.toFloat()  //宽度比例
        val heightRatio = viewHeight.toFloat() / sourceHeight.toFloat()  //高度比例

        val scale: Float
        val translateX: Float
        when (startType) {
            StartType.NONE -> {
                scale = if (widthRatio < heightRatio) widthRatio else heightRatio
                translateX = (viewWidth - sourceWidth * scale) / 2
            }
            StartType.TOP -> {
                scale = when {
                    widthRatio > mScaleThreshold -> widthRatio
                    widthRatio < heightRatio -> widthRatio
                    else -> heightRatio
                }
                translateX = (viewWidth - sourceWidth * scale) / 2
            }
            StartType.LEFT -> {
                scale = when {
                    heightRatio > mScaleThreshold -> heightRatio
                    widthRatio < heightRatio -> widthRatio
                    else -> heightRatio
                }
                translateX = ((viewWidth - sourceWidth * scale) / 2).coerceAtLeast(0f)
            }
            StartType.RIGHT -> {
                scale = when {
                    heightRatio > mScaleThreshold -> heightRatio
                    widthRatio < heightRatio -> widthRatio
                    else -> heightRatio
                }
                val temp = (viewWidth - sourceWidth * scale) / 2
                translateX = if (temp < 0) temp * 2 else temp
            }
            StartType.AUTO_LEFT -> {
                val tempScale = if (widthRatio < heightRatio) heightRatio else widthRatio
                scale = when {
                    tempScale > mScaleThreshold -> tempScale
                    widthRatio < heightRatio -> widthRatio
                    else -> heightRatio
                }
                translateX = ((viewWidth - sourceWidth * scale) / 2).coerceAtLeast(0f)
            }
            StartType.AUTO_RIGHT -> {
                val tempScale = if (widthRatio < heightRatio) heightRatio else widthRatio
                if (tempScale > mScaleThreshold) {
                    scale = tempScale
                    translateX =
                        if (tempScale == widthRatio) 0f else viewWidth - sourceWidth * scale
                } else {
                    scale = if (widthRatio < heightRatio) widthRatio else heightRatio
                    translateX = (viewWidth - sourceWidth * scale) / 2
                }
            }
        }
        val translateY: Float = ((viewHeight - sourceHeight * scale) / 2).coerceAtLeast(0f)

        mBaseMatrix.postScale(scale, scale)
        mBaseMatrix.postTranslate(translateX, translateY)
        mImageView.imageMatrix = finalMatrix
    }

    private fun updateImageMatrix() {
        checkDecoMatrix()
        mImageView.imageMatrix = finalMatrix
    }

    private fun checkDecoMatrix() {
        finalMatrix.getDisplayRect()
        val viewWidth = mImageView.width
        val viewHeight = mImageView.height
        var offsetX = 0f
        var offsetY = 0f

        if (mDisplayRect.width() > viewWidth) {
            if (mDisplayRect.left > 0) {
                offsetX = -mDisplayRect.left
            }
            if (mDisplayRect.right < viewWidth) {
                offsetX = viewWidth - mDisplayRect.right
            }
        } else {
            val correctLeft = (viewWidth - mDisplayRect.width()) / 2
            offsetX = correctLeft - mDisplayRect.left
        }

        if (mDisplayRect.height() > viewHeight) {
            if (mDisplayRect.top > 0) {
                offsetY = -mDisplayRect.top
            }
            if (mDisplayRect.bottom < viewHeight) {
                offsetY = viewHeight - mDisplayRect.bottom
            }
        } else {
            val correctTop = (viewHeight - mDisplayRect.height()) / 2
            offsetY = correctTop - mDisplayRect.top
        }

        mDecoMatrix.postTranslate(offsetX, offsetY)
    }

    private fun startScaleUp(px: Float, py: Float, start: Float): Animation {
        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val currentScale = mDecoMatrix.getScale()
                val targetScale = (MAX_SCALE - start) * interpolatedTime + start
                mDecoMatrix.postScale(
                    targetScale / currentScale, targetScale / currentScale, px, py
                )
                updateImageMatrix()
            }
        }.apply {
            duration = SCALE_DURATION.toLong()
            mImageView.clearAnimation()
            mImageView.startAnimation(this)
        }
    }

    private fun startScaleDown(px: Float, py: Float, start: Float): Animation {
        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val currentScale = mDecoMatrix.getScale()
                val targetScale = start - (start - 1f) * interpolatedTime
                mDecoMatrix.postScale(
                    targetScale / currentScale, targetScale / currentScale,
                    px, py
                )
                updateImageMatrix()
            }
        }.apply {
            duration = SCALE_DURATION.toLong()
            mImageView.clearAnimation()
            mImageView.startAnimation(this)
        }
    }

    private fun Matrix.getDisplayRect() {
        val drawable: Drawable = mImageView.drawable ?: return
        mDisplayRect.set(
            0f,
            0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )
        mapRect(mDisplayRect)
    }

    private fun Matrix.getScale(): Float {
        return sqrt(
            getValue(Matrix.MSCALE_X).toDouble().pow(2.0)
                    + getValue(Matrix.MSKEW_Y).toDouble().pow(2.0)
        ).toFloat()
    }

    private fun Matrix.getValue(key: Int): Float {
        getValues(mMatrixValues)
        return mMatrixValues[key]
    }

    private val finalMatrix: Matrix
        get() = mDrawMatrix.apply {
            set(mBaseMatrix)
            postConcat(mDecoMatrix)
        }

    private inner class FlingAnimation : Runnable {
        private val mScroller: OverScroller = OverScroller(mImageView.context)
        private var mCurrentX: Int = 0
        private var mCurrentY: Int = 0

        override fun run() {
            if (mScroller.isFinished) return
            if (mScroller.computeScrollOffset()) {
                val newX = mScroller.currX
                val newY = mScroller.currY
                mDecoMatrix.postTranslate(
                    (mCurrentX - newX).toFloat(),
                    (mCurrentY - newY).toFloat()
                )
                updateImageMatrix()
                mCurrentX = newX
                mCurrentY = newY
                ViewCompat.postOnAnimation(mImageView, this)
            }
        }

        fun fling(velocityX: Int, velocityY: Int) {
            finalMatrix.getDisplayRect()
            val viewWidth = mImageView.width
            val viewHeight = mImageView.height
            val (startX: Int, startY: Int) =
                -mDisplayRect.left.roundToInt() to -mDisplayRect.top.roundToInt()
            mCurrentX = startX
            mCurrentY = startY
            /*
             * Minimum and maximum scroll positions. The minimum scroll
             * position is generally zero and the maximum scroll position
             * is generally the content size less the screen size. So if the
             * content width is 1000 pixels and the screen width is 200
             * pixels, the maximum scroll offset should be 800 pixels.
             */
            val (minX: Int, minY: Int) = 0 to 0
            val (maxX: Int, maxY: Int) =
                (mDisplayRect.width() - viewWidth).coerceAtLeast(0f).roundToInt() to
                        (mDisplayRect.height() - viewHeight).coerceAtLeast(0f).roundToInt()
            if (maxX != 0 || maxY != 0) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
            }
        }

        fun finish() {
            mScroller.forceFinished(true)
        }

    }
}