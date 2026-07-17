package com.its22.vhodtaksi

import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReportActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var allRows: List<StatusAdapter.Row>
    private lateinit var rate: Calc.Result
    private lateinit var period: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        title = "Справка"

        period = Prefs.period(this)
        val apts = Prefs.apartments(this, period)
        val exps = Prefs.expenses(this, period)
        val incs = Prefs.incomes(this, period)
        rate = Calc.compute(apts, exps, incs)
        val paid = Db(this).paidApartments(period)
        val (cnt, sum) = Db(this).sumForPeriod(period)
        val cur = Prefs.currency(this)

        val sorted = apts.sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
        allRows = sorted.map { a -> StatusAdapter.Row(a, Calc.due(a, rate), paid.contains(a.number)) }

        val dueTotal = Calc.totalDue(apts, rate)
        val unpaidRows = allRows.filter { !it.paid }
        val unpaidTotal = Calc.round2(unpaidRows.sumOf { it.due.total })

        val sb = StringBuilder()
        sb.append("Период: ").append(period).append("\n")
        sb.append("Дължимо общо: ").append(money2(dueTotal)).append(" ").append(cur).append("\n")
        sb.append("Събрано: ").append(money2(sum)).append(" ").append(cur)
        sb.append("  (").append(cnt).append(" от ").append(apts.size).append(" ап.)\n")
        sb.append("Неплатено: ").append(money2(unpaidTotal)).append(" ").append(cur)
        sb.append("  (").append(unpaidRows.size).append(" ап.)")
        findViewById<TextView>(R.id.tvReport).text = sb.toString()

        rv = findViewById(R.id.rvStatus)
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        bind(false)

        val cb = findViewById<CheckBox>(R.id.cbUnpaid)
        cb.setOnCheckedChangeListener { _, checked -> bind(checked) }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPrintReport)
            .setOnClickListener {
                printLines(Receipts.fullReport(this, period, apts, rate, paid, cnt, sum))
            }
    }

    private fun bind(onlyUnpaid: Boolean) {
        val rows = if (onlyUnpaid) allRows.filter { !it.paid } else allRows
        rv.adapter = StatusAdapter(rows, Prefs.currency(this)) { row -> onRow(row) }
    }

    private fun onRow(row: StatusAdapter.Row) {
        val cur = Prefs.currency(this)
        val a = row.apt
        val d = row.due
        val sb = StringBuilder()
        sb.append("Персонална: ").append(money2(d.personal)).append(" ").append(cur).append("\n")
        if (d.elevatorShare > 0.0)
            sb.append("Асансьор: ").append(money2(d.elevatorShare)).append(" ").append(cur).append("\n")
        sb.append("Други: ").append(money2(d.otherShare)).append(" ").append(cur).append("\n")
        sb.append("ОБЩО: ").append(money2(d.total)).append(" ").append(cur).append("\n\n")
        sb.append(if (row.paid) "Статус: ПЛАТЕНО" else "Статус: НЕ Е ПЛАТЕНО")

        val b = AlertDialog.Builder(this)
            .setTitle("Ап. " + a.number + (if (a.name.isNotBlank()) " — " + a.name else ""))
            .setMessage(sb.toString())
            .setNegativeButton("Затвори", null)
        if (row.paid) {
            b.setPositiveButton("Печат отново") { _, _ ->
                printLines(Receipts.payment(this, a, d, rate, period, System.currentTimeMillis()))
            }
        }
        b.show()
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
