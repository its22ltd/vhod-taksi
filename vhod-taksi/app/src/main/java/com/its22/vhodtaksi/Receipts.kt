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
        ts: Long
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
        // разбивка
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
        L.add(Escpos.Line("Подпис: ____________________", size = 24f, extra = 16f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("НЕ Е ФИСКАЛЕН ДОКУМЕНТ", size = 24f, bold = true, align = Escpos.Align.CENTER, extra = 10f))
        val f = Prefs.footer(ctx)
        if (f.isNotBlank()) L.add(Escpos.Line(f, size = 24f, align = Escpos.Align.CENTER, extra = 10f))
        return L
    }

    /** Отчет за периода */
    fun report(
        ctx: Context,
        period: String,
        r: Calc.Result,
        exps: List<ExpenseItem>,
        incs: List<IncomeItem>,
        totalDue: Double,
        collectedCount: Int,
        collectedSum: Double,
        aptCount: Int
    ): List<Escpos.Line> {
        val cur = Prefs.currency(ctx)
        val L = ArrayList<Escpos.Line>()
        L.add(Escpos.Line(Prefs.businessName(ctx), size = 36f, bold = true, align = Escpos.Align.CENTER, extra = 10f))
        L.add(Escpos.Line("ОТЧЕТ", size = 32f, bold = true, align = Escpos.Align.CENTER))
        L.add(Escpos.Line("Период: " + period, size = 26f, align = Escpos.Align.CENTER, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("РАЗХОДИ", size = 24f, bold = true))
        for (e in exps) {
            val tag = if (e.elevator) " (асансьор)" else ""
            L.add(Escpos.Line(text = e.label + tag + ":", rightText = money2(e.amount) + " " + cur, size = 22f))
        }
        L.add(Escpos.Line(text = "Общо разходи:", rightText = money2(r.totalExpenses) + " " + cur, size = 26f, bold = true, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("ПРИХОДИ", size = 24f, bold = true))
        for (i in incs) {
            L.add(Escpos.Line(text = i.label + ":", rightText = money2(i.amount) + " " + cur, size = 22f))
        }
        L.add(Escpos.Line(text = "Общо приходи:", rightText = money2(r.totalIncome) + " " + cur, size = 24f, bold = true, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line(text = "Дял асансьор/чов.:", rightText = money2(r.ratePerElevator) + " " + cur, size = 22f))
        L.add(Escpos.Line(text = "Дял други/чов.:", rightText = money2(r.ratePerOther) + " " + cur, size = 22f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line(text = "Дължимо общо:", rightText = money2(totalDue) + " " + cur, size = 24f))
        L.add(Escpos.Line(text = "Събрано:", rightText = money2(collectedSum) + " " + cur, size = 28f, bold = true, extra = 8f))
        L.add(Escpos.Line("Платили: " + collectedCount + " от " + aptCount + " ап.", size = 22f))
        L.add(Escpos.Line(text = "Разлика (събрано-разходи):", rightText = money2(collectedSum - r.totalExpenses) + " " + cur, size = 22f, extra = 8f))
        L.add(Escpos.Line(separator = true, size = 24f))
        L.add(Escpos.Line("Не е фискален документ", size = 22f, align = Escpos.Align.CENTER, extra = 10f))
        return L
    }
}
