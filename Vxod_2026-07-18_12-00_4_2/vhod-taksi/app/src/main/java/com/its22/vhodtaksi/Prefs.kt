package com.its22.vhodtaksi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object Prefs {
    private const val P = "vhod_prefs"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)

    // --- Обект / бележка ---
    fun businessName(ctx: Context): String = sp(ctx).getString("biz", "ЕТАЖНА СОБСТВЕНОСТ") ?: "ЕТАЖНА СОБСТВЕНОСТ"
    fun setBusinessName(ctx: Context, v: String) = sp(ctx).edit().putString("biz", v).apply()

    fun subtitle(ctx: Context): String = sp(ctx).getString("subtitle", "") ?: ""
    fun setSubtitle(ctx: Context, v: String) = sp(ctx).edit().putString("subtitle", v).apply()

    fun footer(ctx: Context): String = sp(ctx).getString("footer", "Благодарим Ви!") ?: "Благодарим Ви!"
    fun setFooter(ctx: Context, v: String) = sp(ctx).edit().putString("footer", v).apply()

    fun currency(ctx: Context): String = sp(ctx).getString("cur", "€") ?: "€"
    fun setCurrency(ctx: Context, v: String) = sp(ctx).edit().putString("cur", v).apply()

    // --- Текущ период ---
    fun period(ctx: Context): String {
        val cur = sp(ctx).getString("period", null)
        if (cur != null) return cur
        return currentMonthLabel()
    }
    fun setPeriod(ctx: Context, v: String) {
        sp(ctx).edit().putString("period", v).apply()
        registerPeriod(ctx, v)
    }

    private fun currentMonthLabel(): String =
        SimpleDateFormat("'м.'MM.yyyy", Locale("bg", "BG")).format(Date())

    /** Всички съществуващи периоди, подредени най-нов най-отгоре */
    fun periods(ctx: Context): MutableList<String> {
        val raw = sp(ctx).getString("periods", null)
        val out = mutableListOf<String>()
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) out.add(arr.getString(i))
            } catch (e: Exception) { /* ignore */ }
        }
        if (out.isEmpty()) out.add(period(ctx))
        out.sortByDescending { periodKey(it) }
        return out
    }

    private fun registerPeriod(ctx: Context, p: String) {
        if (p.isBlank()) return
        val list = periods(ctx)
        if (!list.contains(p)) {
            list.add(p)
            val arr = JSONArray()
            for (x in list) arr.put(x)
            sp(ctx).edit().putString("periods", arr.toString()).apply()
        }
    }

    /** Число за подреждане: yyyyMM ако форматът е "м.MM.yyyy", иначе 0 */
    private fun periodKey(p: String): Int {
        val m = Regex("(\\d{2})\\D+(\\d{4})").find(p) ?: return 0
        val mm = m.groupValues[1].toIntOrNull() ?: return 0
        val yyyy = m.groupValues[2].toIntOrNull() ?: return 0
        return yyyy * 100 + mm
    }

    /** Предлага следващия месец спрямо даден период "м.MM.yyyy" */
    fun suggestNextPeriod(current: String): String {
        val m = Regex("(\\d{2})\\D+(\\d{4})").find(current) ?: return currentMonthLabel()
        var mm = m.groupValues[1].toIntOrNull() ?: return currentMonthLabel()
        var yyyy = m.groupValues[2].toIntOrNull() ?: return currentMonthLabel()
        mm += 1
        if (mm > 12) { mm = 1; yyyy += 1 }
        return "м." + String.format(Locale.US, "%02d", mm) + "." + yyyy
    }

    /** Създава нов период, като копира данните от текущия, и го прави активен */
    fun generateNewMonth(ctx: Context, newPeriod: String) {
        val cur = period(ctx)
        val a = apartments(ctx, cur)
        val e = expenses(ctx, cur)
        val i = incomes(ctx, cur)
        setApartments(ctx, newPeriod, a)
        setExpenses(ctx, newPeriod, e)
        setIncomes(ctx, newPeriod, i)
        setPeriod(ctx, newPeriod)
    }

    // --- Принтер ---
    fun printerMac(ctx: Context): String = sp(ctx).getString("mac", "") ?: ""
    fun printerName(ctx: Context): String = sp(ctx).getString("pname", "") ?: ""
    fun setPrinter(ctx: Context, mac: String, name: String) =
        sp(ctx).edit().putString("mac", mac).putString("pname", name).apply()

    fun paperDots(ctx: Context): Int = sp(ctx).getInt("dots", 576)
    fun setPaperDots(ctx: Context, v: Int) = sp(ctx).edit().putInt("dots", v).apply()

    // --- Сървър (MSSQL API) ---
    fun serverUrl(ctx: Context): String = sp(ctx).getString("surl", "") ?: ""
    fun setServerUrl(ctx: Context, v: String) = sp(ctx).edit().putString("surl", v).apply()

    fun serverToken(ctx: Context): String = sp(ctx).getString("stoken", "") ?: ""
    fun setServerToken(ctx: Context, v: String) = sp(ctx).edit().putString("stoken", v).apply()

    fun deviceId(ctx: Context): String {
        val cur = sp(ctx).getString("device", null)
        if (cur != null) return cur
        val id = "dev-" + UUID.randomUUID().toString().substring(0, 8)
        sp(ctx).edit().putString("device", id).apply()
        return id
    }

    // --- Апартаменти (по период) ---
    fun apartments(ctx: Context, period: String): MutableList<Apartment> {
        val raw = sp(ctx).getString("apts_$period", null)
            ?: (sp(ctx).getString("apts", null) ?: return defaultApartments())
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<Apartment>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Apartment(
                        o.getString("num"),
                        o.optString("name", ""),
                        o.getInt("people"),
                        o.getDouble("personal"),
                        o.getBoolean("elev"),
                        o.optString("phone", "")
                    )
                )
            }
            if (out.isEmpty()) defaultApartments() else out
        } catch (e: Exception) {
            defaultApartments()
        }
    }

    fun setApartments(ctx: Context, period: String, list: List<Apartment>) {
        val arr = JSONArray()
        for (a in list) {
            val o = JSONObject()
            o.put("num", a.number)
            o.put("name", a.name)
            o.put("people", a.people)
            o.put("personal", a.personal)
            o.put("elev", a.paysElevator)
            o.put("phone", a.phone)
            arr.put(o)
        }
        sp(ctx).edit().putString("apts_$period", arr.toString()).apply()
        registerPeriod(ctx, period)
    }

    // --- Разходи (по период) ---
    fun expenses(ctx: Context, period: String): MutableList<ExpenseItem> {
        val raw = sp(ctx).getString("exps_$period", null)
            ?: (sp(ctx).getString("exps", null) ?: return defaultExpenses())
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<ExpenseItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(ExpenseItem(o.getString("label"), o.getDouble("amount"), o.getBoolean("elev")))
            }
            out
        } catch (e: Exception) {
            defaultExpenses()
        }
    }

    fun setExpenses(ctx: Context, period: String, list: List<ExpenseItem>) {
        val arr = JSONArray()
        for (e in list) {
            val o = JSONObject()
            o.put("label", e.label)
            o.put("amount", e.amount)
            o.put("elev", e.elevator)
            arr.put(o)
        }
        sp(ctx).edit().putString("exps_$period", arr.toString()).apply()
        registerPeriod(ctx, period)
    }

    // --- Приходи (по период) ---
    fun incomes(ctx: Context, period: String): MutableList<IncomeItem> {
        val raw = sp(ctx).getString("incs_$period", null)
            ?: (sp(ctx).getString("incs", null) ?: return defaultIncomes())
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<IncomeItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(IncomeItem(o.getString("label"), o.getDouble("amount")))
            }
            out
        } catch (e: Exception) {
            defaultIncomes()
        }
    }

    fun setIncomes(ctx: Context, period: String, list: List<IncomeItem>) {
        val arr = JSONArray()
        for (i in list) {
            val o = JSONObject()
            o.put("label", i.label)
            o.put("amount", i.amount)
            arr.put(o)
        }
        sp(ctx).edit().putString("incs_$period", arr.toString()).apply()
        registerPeriod(ctx, period)
    }

    // --- Примерни данни (от твоята таблица) ---
    private fun defaultApartments(): MutableList<Apartment> {
        val d = listOf(
            Triple("1", 3, 4.00), Triple("2", 2, 4.00), Triple("3", 0, 4.00),
            Triple("4", 1, 2.00), Triple("5", 3, 3.00), Triple("6", 2, 4.00),
            Triple("7", 4, 2.00), Triple("8", 2, 3.00), Triple("9", 2, 4.00),
            Triple("10", 2, 2.00), Triple("11", 2, 3.00), Triple("12", 2, 4.00),
            Triple("13", 1, 2.00), Triple("14", 2, 3.50), Triple("15", 0, 4.00),
            Triple("16", 1, 2.00), Triple("17", 1, 3.00), Triple("18", 2, 4.00),
            Triple("19", 2, 2.00), Triple("20", 1, 3.00), Triple("21", 2, 4.00),
            Triple("22", 0, 2.00), Triple("23", 2, 3.00)
        )
        val out = mutableListOf<Apartment>()
        for (t in d) {
            val num = t.first
            val paysElev = (num.toIntOrNull() ?: 99) > 5
            out.add(Apartment(num, "", t.second, t.third, paysElev))
        }
        return out
    }

    private fun defaultExpenses(): MutableList<ExpenseItem> = mutableListOf(
        ExpenseItem("Асансьор ток", 25.0, true),
        ExpenseItem("Такса асансьор", 67.0, true),
        ExpenseItem("Почистване", 41.0, false),
        ExpenseItem("Касиер", 15.0, false),
        ExpenseItem("Домоуправител", 15.0, false),
        ExpenseItem("Други", 0.0, false)
    )

    private fun defaultIncomes(): MutableList<IncomeItem> = mutableListOf(
        IncomeItem("Наем на помещение", 100.0)
    )
}
