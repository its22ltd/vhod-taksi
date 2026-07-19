package com.its22.vhodtaksi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/** Поле за подпис с пръст */
class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val path = Path()
    private val paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private var lastX = 0f
    private var lastY = 0f

    var hasContent = false
        private set

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                path.moveTo(x, y)
                lastX = x; lastY = y
                hasContent = true
            }
            MotionEvent.ACTION_MOVE -> {
                path.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f)
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_UP -> {
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun clear() {
        path.reset()
        hasContent = false
        invalidate()
    }

    fun getBitmap(): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.drawPath(path, paint)
        return bmp
    }
}
