package com.its22.vhodtaksi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Изпраща (push) незасинхронизираните плащания към ASP.NET Core API-то (/api/sync),
 * което ги записва в MSSQL. Работи идемпотентно чрез uuid - повторно изпращане не
 * прави дубликати. При успех локално маркира записите като synced=1.
 */
object Sync {

    fun push(ctx: Context): Int {
        val urlStr = Prefs.serverUrl(ctx)
        if (urlStr.isBlank()) throw Exception("Не е зададен адрес на сървъра (Настройки)")

        val db = Db(ctx)
        val recs = db.unsynced()
        if (recs.isEmpty()) return 0

        val arr = JSONArray()
        for (r in recs) {
            val o = JSONObject()
            o.put("uuid", r.uuid)
            o.put("apt", r.apt)
            o.put("name", r.name)
            o.put("people", r.people)
            o.put("period", r.period)
            o.put("amount", r.amount)
            o.put("personal", r.personal)
            o.put("elevatorShare", r.elevatorShare)
            o.put("otherShare", r.otherShare)
            o.put("ts", r.ts)
            o.put("extraLabel", r.extraLabel)
            o.put("extraAmount", r.extraAmount)
            val sig = Signatures.loadBase64(ctx, r.uuid)
            if (sig != null) o.put("signature", sig)
            arr.put(o)
        }
        val body = JSONObject()
        body.put("token", Prefs.serverToken(ctx))
        body.put("device", Prefs.deviceId(ctx))
        body.put("records", arr)
        val payload = body.toString().toByteArray(Charsets.UTF_8)

        val conn = URL(urlStr).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.outputStream.use { it.write(payload) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val respText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw Exception("Сървър върна код $code: $respText")

            val resp = JSONObject(respText)
            if (!resp.optBoolean("ok", false)) {
                throw Exception("Сървър: " + resp.optString("error", "неизвестна грешка"))
            }
            db.markSynced(recs.map { it.uuid })
            return recs.size
        } finally {
            conn.disconnect()
        }
    }
}
