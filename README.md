# ImageGesture

implementation --> [maven](https://github.com/mmm3w/maven)

手势插件，为ImageView提供图片手势操作

### 依赖引用
```groovy
repositories {
    maven { url 'https://raw.githubusercontent.com/mmm3w/maven/main' }
}

dependencies {
    implementation "com.mitsuki.armory:imagegesture:1.0.0"
}
```

### 使用说明
```kotlin
val mImageGesture: ImageGesture = ImageGesture(imageViewInstance)
//基础配置
//图片初始展示形式
//NONE,       //全显示
//TOP,        //顶部起始
//LEFT,       //左边起始
//RIGHT,      //右边起始
//AUTO_LEFT,  //自动适配，出现横向时从左开始
//AUTO_RIGHT  //自动适配，出现横向时从右开始
mImageGesture.startType = StartType.AUTO_LEFT
//图片双指放大最大大小（原图大小倍率）
mImageGesture.minScale = 3f
//图片双指缩小最小大小（原图大小倍率）
mImageGesture.minScale = 3f
//双击图片时图片大小变换梯度，与原图大小进行循环切换
mImageGesture.autoScaleGradient = floatArrayOf(1.5f, 3f)
```

如果希望获取一些点击事件，可以通过继承并重写相关方法来实现

以下是区域点击事件判定示例

```kotlin
class GalleryImageGesture(imageView: ImageView) : ImageGesture(imageView) {

    var onLongPress: (() -> Unit)? = null

    var onAreaTap: ((Int) -> Unit)? = null

    private val mLeftRect = RectF()
    private val mTopRect = RectF()
    private val mRightRect = RectF()
    private val mBottomRect = RectF()
    private val mCenterTopRect = RectF()
    private val mCenterBottomRect = RectF()

    private val vp = 0.25F
    private val hp = 0.3F

    override fun onLongPress(e: MotionEvent) {
        onLongPress?.invoke()
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val tapX = e.x
        val tapY = e.y

        when {
            mLeftRect.contains(tapX, tapY) -> onAreaTap?.invoke(0)
            mTopRect.contains(tapX, tapY) -> onAreaTap?.invoke(1)
            mRightRect.contains(tapX, tapY) -> onAreaTap?.invoke(2)
            mBottomRect.contains(tapX, tapY) -> onAreaTap?.invoke(3)
            mCenterTopRect.contains(tapX, tapY) -> onAreaTap?.invoke(4)
            mCenterBottomRect.contains(tapX, tapY) -> onAreaTap?.invoke(5)
            else -> return false
        }
        return true
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        super.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
        val centerY = (top + bottom) / 2F
        val width = right - left
        val height = bottom - top

        val inLeft = left + width * hp
        val inRight = right - width * hp
        val inTop = top + height * vp
        val inBottom = bottom - height * vp

        mLeftRect.set(left.toFloat(), inTop, inLeft, bottom.toFloat())
        mTopRect.set(left.toFloat(), top.toFloat(), inRight, inTop)
        mRightRect.set(inRight, top.toFloat(), right.toFloat(), inBottom)
        mBottomRect.set(inLeft, inBottom, right.toFloat(), bottom.toFloat())
        mCenterTopRect.set(inLeft, inTop, inRight, centerY)
        mCenterBottomRect.set(inLeft, centerY, inRight, inBottom)
    }

}
```
