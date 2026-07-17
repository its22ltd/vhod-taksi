package com.its22.vhodtaksi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

/**
 * Рендерираме бележката като изображение (Bitmap) и я печатаме като растер (ESC/POS GS v 0).
 * Така кирилицата излиза правилно на всеки принтер, независимо дали има кирилски шрифт.
 */
object Escpos {

    enum class Align { LEFT, CENTER, RIGHT }

    data class Line(
        val text: String = "",
        val size: Float = 26f,
        val bold: Boolean = false,
        val align: Align = Align.LEFT,
        val rightText: String = "",
        val separator: Boolean = false,
        val extra: Float = 6f
    )

    fun buildReceiptBitmap(width: Int, lines: List<Line>): Bitmap {
        val padTop = 14
        val padBottom = 20
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val heights = ArrayList<Int>(lines.size)
        var total = padTop
        for (ln in lines) {
            paint.textSize = ln.size
            paint.typeface = if (ln.bold)
                Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            else
                Typeface.MONOSPACE
            val fm = paint.fontMetrics
            val h = ((fm.descent - fm.ascent) + ln.extra).toInt()
            heights.add(h)
            total += h
        }
        val height = total + padBottom

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        var y = padTop
        for (i in lines.indices) {
            val ln = lines[i]
            paint.textSize = ln.size
            paint.typeface = if (ln.bold)
                Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            else
                Typeface.MONOSPACE
            paint.color = Color.BLACK
            val fm = paint.fontMetrics
            val baseline = y - fm.ascent

            if (ln.separator) {
                val ch = "-"
                val cw = paint.measureText(ch)
                val n = if (cw > 0f) (width / cw).toInt() else 32
                val sb = StringBuilder()
                for (k in 0 until n) sb.append(ch)
                canvas.drawText(sb.toString(), 0f, baseline, paint)
            } else {
                when (ln.align) {
                    Align.CENTER -> {
                        val w = paint.measureText(ln.text)
                        canvas.drawText(ln.text, (width - w) / 2f, baseline, paint)
                    }
                    Align.RIGHT -> {
                        val w = paint.measureText(ln.text)
                        canvas.drawText(ln.text, width - w, baseline, paint)
                    }
                    Align.LEFT -> {
                        canvas.drawText(ln.text, 0f, baseline, paint)
                    }
                }
                if (ln.rightText.isNotEmpty()) {
                    val rw = paint.measureText(ln.rightText)
                    canvas.drawText(ln.rightText, width - rw, baseline, paint)
                }
            }
            y += heights[i]
        }
        return bmp
    }

    fun bitmapToEscPos(bmp: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0x1B); out.write(0x40)             // ESC @  -> reset
        out.write(0x1B); out.write(0x61); out.write(0x00) // ESC a 0 -> left align

        val width = bmp.width
        val height = bmp.height
        val bytesPerRow = (width + 7) / 8
        val pixels = IntArray(width * height)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)

        val band = 128
        var yStart = 0
        while (yStart < height) {
            val h = minOf(band, height - yStart)
            out.write(0x1D); out.write(0x76); out.write(0x30); out.write(0x00) // GS v 0
            out.write(bytesPerRow and 0xFF); out.write((bytesPerRow shr 8) and 0xFF)
            out.write(h and 0xFF); out.write((h shr 8) and 0xFF)
            for (row in 0 until h) {
                val yy = yStart + row
                for (bx in 0 until bytesPerRow) {
                    var b = 0
                    for (bit in 0 until 8) {
                        val x = bx * 8 + bit
                        if (x < width) {
                            val px = pixels[yy * width + x]
                            val a = (px ushr 24) and 0xFF
                            val r = (px shr 16) and 0xFF
                            val g = (px shr 8) and 0xFF
                            val bl = px and 0xFF
                            val lum = (r * 299 + g * 587 + bl * 114) / 1000
                            if (a > 128 && lum < 128) b = b or (0x80 shr bit)
                        }
                    }
                    out.write(b)
                }
            }
            yStart += h
        }

        out.write(0x1B); out.write(0x64); out.write(0x03) // ESC d 3 -> feed 3 lines
        out.write(0x1D); out.write(0x56); out.write(0x42); out.write(0x00) // GS V 66 0 -> partial cut (игнорира се ако няма нож)
        return out.toByteArray()
    }
}
