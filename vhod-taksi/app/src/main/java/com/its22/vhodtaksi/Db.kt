package com.its22.vhodtaksi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class PaymentRecord(
    val id: Long,
    val uuid: String,
    val apt: String,
    val name: String,
    val people: Int,
    val period: String,
    val amount: Double,
    val personal: Double,
    val elevatorShare: Double,
    val otherShare: Double,
    val ts: Long,
    val synced: Boolean
)

class Db(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "vhod.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE payment(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT UNIQUE, " +
                "apt TEXT NOT NULL, " +
                "name TEXT, " +
                "people INTEGER NOT NULL, " +
                "period TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "personal REAL NOT NULL, " +
                "elevator REAL NOT NULL, " +
                "other REAL NOT NULL, " +
                "ts INTEGER NOT NULL, " +
                "synced INTEGER NOT NULL DEFAULT 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun insert(p: PaymentRecord): Long {
        val cv = ContentValues()
        cv.put("uuid", p.uuid)
        cv.put("apt", p.apt)
        cv.put("name", p.name)
        cv.put("people", p.people)
        cv.put("period", p.period)
        cv.put("amount", p.amount)
        cv.put("personal", p.personal)
        cv.put("elevator", p.elevatorShare)
        cv.put("other", p.otherShare)
        cv.put("ts", p.ts)
        cv.put("synced", 0)
        return writableDatabase.insert("payment", null, cv)
    }

    private fun readList(sql: String, args: Array<String>?): List<PaymentRecord> {
        val out = mutableListOf<PaymentRecord>()
        val c = readableDatabase.rawQuery(sql, args)
        c.use {
            while (it.moveToNext()) {
                out.add(
                    PaymentRecord(
                        it.getLong(0),
                        it.getString(1) ?: "",
                        it.getString(2),
                        it.getString(3) ?: "",
                        it.getInt(4),
                        it.getString(5),
                        it.getDouble(6),
                        it.getDouble(7),
                        it.getDouble(8),
                        it.getDouble(9),
                        it.getLong(10),
                        it.getInt(11) == 1
                    )
                )
            }
        }
        return out
    }

    private val cols =
        "id, uuid, apt, name, people, period, amount, personal, elevator, other, ts, synced"

    fun recent(limit: Int = 1000): List<PaymentRecord> =
        readList("SELECT $cols FROM payment ORDER BY ts DESC LIMIT ?", arrayOf(limit.toString()))

    fun forPeriod(period: String): List<PaymentRecord> =
        readList("SELECT $cols FROM payment WHERE period = ? ORDER BY ts DESC", arrayOf(period))

    fun unsynced(): List<PaymentRecord> =
        readList("SELECT $cols FROM payment WHERE synced = 0 ORDER BY ts ASC", null)

    fun paidApartments(period: String): Set<String> {
        val set = HashSet<String>()
        val c = readableDatabase.rawQuery(
            "SELECT DISTINCT apt FROM payment WHERE period = ?", arrayOf(period)
        )
        c.use { while (it.moveToNext()) set.add(it.getString(0)) }
        return set
    }

    fun sumForPeriod(period: String): Pair<Int, Double> {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(amount), 0) FROM payment WHERE period = ?",
            arrayOf(period)
        )
        c.use { if (it.moveToNext()) return Pair(it.getInt(0), it.getDouble(1)) }
        return Pair(0, 0.0)
    }

    fun countUnsynced(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM payment WHERE synced = 0", null)
        c.use { if (it.moveToNext()) return it.getInt(0) }
        return 0
    }

    fun markSynced(uuids: List<String>) {
        if (uuids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement("UPDATE payment SET synced = 1 WHERE uuid = ?")
            for (u in uuids) {
                if (u.isBlank()) continue
                stmt.bindString(1, u)
                stmt.executeUpdateDelete()
                stmt.clearBindings()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
