package com.its22.vhodtaksi

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        title = "Отчет"

        val apts = Prefs.apartments(this)
        val exps = Prefs.expenses(this)
        val incs = Prefs.incomes(this)
        val period = Prefs.period(this)
        val cur = Prefs.currency(this)
        val r = Calc.compute(apts, exps, incs)
        val totalDue = Calc.totalDue(apts, r)
        val db = Db(this)
        val (cnt, sum) = db.sumForPeriod(period)

        val sb = StringBuilder()
        sb.append("Период: ").append(period).append("\n")
        sb.append("Разходи общо: ").append(money2(r.totalExpenses)).append(" ").append(cur).append("\n")
        sb.append("Приходи общо: ").append(money2(r.totalIncome)).append(" ").append(cur).append("\n")
        sb.append("Дял/чов. — асансьор: ").append(money2(r.ratePerElevator)).append(" ").append(cur)
        sb.append(", други: ").append(money2(r.ratePerOther)).append(" ").append(cur).append("\n")
        sb.append("Дължимо общо: ").append(money2(totalDue)).append(" ").append(cur).append("\n")
        sb.append("Събрано: ").append(money2(sum)).append(" ").append(cur)
        sb.append("  (").append(cnt).append(" от ").append(apts.size).append(" ап.)\n")
        sb.append("Разлика (събрано-разходи): ").append(money2(sum - r.totalExpenses)).append(" ").append(cur)
        val un = db.countUnsynced()
        if (un > 0) sb.append("\nНесинхронизирани: ").append(un)
        findViewById<TextView>(R.id.tvReport).text = sb.toString()

        val rv = findViewById<RecyclerView>(R.id.rvPayments)
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        rv.adapter = PaymentAdapter(db.forPeriod(period), cur) { rec -> reprint(rec) }

        findViewById<MaterialButton>(R.id.btnPrintReport).setOnClickListener {
            printLines(Receipts.report(this, period, r, exps, incs, totalDue, cnt, sum, apts.size))
        }
    }

    private fun reprint(rec: PaymentRecord) {
        AlertDialog.Builder(this)
            .setTitle("Ап. " + rec.apt + " — повторен печат?")
            .setMessage("Сума: " + eur(this, rec.amount) + "  (" + rec.period + ")")
            .setPositiveButton("Печат") { _, _ ->
                val apt = Apartment(rec.apt, rec.name, rec.people, rec.personal, rec.elevatorShare > 0.0)
                val due = Calc.Due(rec.personal, rec.elevatorShare, rec.otherShare)
                val ratePerElev = if (rec.people > 0) rec.elevatorShare / rec.people else 0.0
                val ratePerOther = if (rec.people > 0) rec.otherShare / rec.people else 0.0
                val rate = Calc.Result(0.0, 0.0, 0.0, 0.0, 0, 0, ratePerElev, ratePerOther)
                printLines(Receipts.payment(this, apt, due, rate, rec.period, rec.ts))
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun printLines(lines: List<Escpos.Line>) {
        val dots = Prefs.paperDots(this)
        val mac = Prefs.printerMac(this)
        Toast.makeText(this, "Печат...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val bmp = Escpos.buildReceiptBitmap(dots, lines)
                BtPrinter.printBytes(mac, Escpos.bitmapToEscPos(bmp))
                runOnUiThread { Toast.makeText(this, "Готово", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Грешка: " + (e.message ?: ""), Toast.LENGTH_LONG).show() }
            }
        }.start()
    }
}
