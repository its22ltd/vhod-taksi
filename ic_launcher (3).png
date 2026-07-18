package com.its22.vhodtaksi

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Receipts {
    private val BG = Locale("bg", "BG")
    private val dfFull = SimpleDateFormat("dd.MM.yyyy HH:mm", BG)

    /** Бележка за плащане на месечна такса от апартамент */
    fun payment(
        ctx: Context,
        apt: Apartment,
        due: Calc.Due,
        rate: Calc.Result,
        period: String,
        ts: Long,
        signature: android.graphics.Bitmap? = null
    ): List<Escpos.Line> {
        val cur = Prefs.currency(ctx)
        val L = ArrayList<Escpos.Line>()
        L.add(Escpos.Line(Prefs.businessName(ctx), size = 40f, bold = true, align = Escpos.Align.CENTER, extra = 12f))
        val sub = Prefs.subtitle(ctx)
        if (sub.isNotBlank()) L.add(Escpos.Line(sub, size = 24f, align = Escpos.Align.CENTER))
        L.add(Escpos.Line("БЕЛЕЖКА ЗА ПЛАЩАНЕ", size = 26f, bold = true, align = Escpos.Align.CENTER, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("Период: " + period, size = 26f, bold = true))
        L.add(Escpos.Line("Дата: " + dfFull.format(Date(ts)), size = 22f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("Апартамент: " + apt.number, size = 30f, bold = true, extra = 8f))
        if (apt.name.isNotBlank()) L.add(Escpos.Line("Живущ: " + apt.name, size = 24f))
        L.add(Escpos.Line("Брой хора: " + apt.people, size = 24f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line(text = "Персонална сума:", rightText = money2(due.personal) + " " + cur, size = 24f))
        if (apt.paysElevator && due.elevatorShare > 0.0) {
            L.add(
                Escpos.Line(
                    text = "Асансьор (" + apt.people + " x " + money2(rate.ratePerElevator) + "):",
                    rightText = money2(due.elevatorShare) + " " + cur, size = 24f
                )
            )
        }
        L.add(
            Escpos.Line(
                text = "Други (" + apt.people + " x " + money2(rate.ratePerOther) + "):",
                rightText = money2(due.otherShare) + " " + cur, size = 24f
            )
        )
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line(text = "ОБЩО:", rightText = money2(due.total) + " " + cur, size = 34f, bold = true, extra = 14f))
        L.add(Escpos.Line(separator = true, size = 24f))
        if (signature != null) {
            L.add(Escpos.Line("Подпис:", size = 22f))
            L.add(Escpos.Line(image = signature, extra = 8f))
        } else {
            L.add(Escpos.Line("Подпис: ____________________", size = 24f, extra = 16f))
        }
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("НЕ Е ФИСКАЛЕН ДОКУМЕНТ", size = 24f, bold = true, align = Escpos.Align.CENTER, extra = 10f))
        val f = Prefs.footer(ctx)
        if (f.isNotBlank()) L.add(Escpos.Line(f, size = 24f, align = Escpos.Align.CENTER, extra = 10f))
        return L
    }

    /** Справка за периода: финанси + кой апартамент е платил/дължи */
    fun fullReport(
        ctx: Context,
        period: String,
        apts: List<Apartment>,
        r: Calc.Result,
        paid: Set<String>,
        collectedCount: Int,
        collectedSum: Double
    ): List<Escpos.Line> {
        val cur = Prefs.currency(ctx)
        val L = ArrayList<Escpos.Line>()
        L.add(Escpos.Line(Prefs.businessName(ctx), size = 34f, bold = true, align = Escpos.Align.CENTER, extra = 8f))
        L.add(Escpos.Line("СПРАВКА", size = 30f, bold = true, align = Escpos.Align.CENTER))
        L.add(Escpos.Line("Период: " + period, size = 24f, align = Escpos.Align.CENTER, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line(text = "Разходи общо:", rightText = money2(r.totalExpenses) + " " + cur, size = 24f))
        L.add(Escpos.Line(text = "Приходи общо:", rightText = money2(r.totalIncome) + " " + cur, size = 24f))
        L.add(Escpos.Line(text = "Дял асансьор/чов.:", rightText = money2(r.ratePerElevator) + " " + cur, size = 22f))
        L.add(Escpos.Line(text = "Дял други/чов.:", rightText = money2(r.ratePerOther) + " " + cur, size = 22f, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))

        var dueTotal = 0.0
        var unpaidTotal = 0.0
        var unpaidCount = 0
        for (a in apts) {
            val d = Calc.due(a, r)
            dueTotal = Calc.round2(dueTotal + d.total)
            val isPaid = paid.contains(a.number)
            val mark = if (isPaid) "  платено" else "  ДЪЛЖИ"
            if (!isPaid) {
                unpaidTotal = Calc.round2(unpaidTotal + d.total)
                unpaidCount += 1
            }
            L.add(Escpos.Line(text = "Ап. " + a.number + mark, rightText = money2(d.total) + " " + cur, size = 22f))
        }
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line(text = "Дължимо общо:", rightText = money2(dueTotal) + " " + cur, size = 24f))
        L.add(Escpos.Line(text = "Събрано:", rightText = money2(collectedSum) + " " + cur, size = 28f, bold = true))
        L.add(Escpos.Line("Платили: " + collectedCount + " от " + apts.size + " ап.", size = 22f))
        L.add(Escpos.Line(text = "Неплатено (" + unpaidCount + " ап.):", rightText = money2(unpaidTotal) + " " + cur, size = 26f, bold = true, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("Не е фискален документ", size = 22f, align = Escpos.Align.CENTER, extra = 10f))
        return L
    }
}
