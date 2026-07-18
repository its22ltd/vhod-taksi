package com.its22.vhodtaksi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

/** Запазване и зареждане на подписите (като PNG файлове по uuid на плащането) */
object Signatures {

    private fun file(ctx: Context, uuid: String) = File(ctx.filesDir, "sig_$uuid.png")

    fun save(ctx: Context, uuid: String, bmp: Bitmap?) {
        if (bmp == null) return
        try {
            FileOutputStream(file(ctx, uuid)).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            // игнорираме
        }
    }

    fun load(ctx: Context, uuid: String): Bitmap? {
        val f = file(ctx, uuid)
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /** Подписът като Base64 текст (за изпращане към сървъра), или null ако няма */
    fun loadBase64(ctx: Context, uuid: String): String? {
        val f = file(ctx, uuid)
        if (!f.exists()) return null
        return try {
            Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
