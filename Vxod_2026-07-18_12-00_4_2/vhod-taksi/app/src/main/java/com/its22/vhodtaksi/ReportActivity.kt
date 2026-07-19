package com.its22.vhodtaksi

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var exps: List<ExpenseItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        title = "Справка"

        period = Prefs.period(this)
        val apts = Prefs.apartments(this, period)
        exps = Prefs.expenses(this, period)
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
        sb.append("СПРАВКА — ").append(period).append("\n\n")
        sb.append("Дължимо общо:  ").append(money2(dueTotal)).append(" ").append(cur).append("\n")
        sb.append("Събрано:  ").append(money2(sum)).append(" ").append(cur)
        sb.append("   (").append(cnt).append(" платили)\n")
        sb.append("Остава да съберете:  ").append(money2(unpaidTotal)).append(" ").append(cur)
        sb.append("   (").append(unpaidRows.size).append(" неплатили)")
        findViewById<TextView>(R.id.tvReport).text = sb.toString()

        rv = findViewById(R.id.rvStatus)
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        val cb = findViewById<CheckBox>(R.id.cbUnpaid)
        cb.isChecked = true
        cb.setOnCheckedChangeListener { _, checked -> bind(checked) }
        bind(true)

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
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad / 2, pad, 0)

        val tv = TextView(this)
        val sb = StringBuilder()
        val elevNames = exps.filter { it.elevator && it.amount > 0.0 }.joinToString(", ") { it.label }
        val otherNames = exps.filter { !it.elevator && it.amount > 0.0 }.joinToString(", ") { it.label }
        sb.append("Брой хора: ").append(a.people).append("\n")
        sb.append("Персонална сума: ").append(money2(d.personal)).append(" ").append(cur).append("\n")
        if (a.paysElevator && d.elevatorShare > 0.0) {
            sb.append("Асансьор (").append(a.people).append(" x ").append(money2(rate.ratePerElevator))
                .append("): ").append(money2(d.elevatorShare)).append(" ").append(cur).append("\n")
            if (elevNames.isNotBlank()) sb.append("   вкл.: ").append(elevNames).append("\n")
        }
        sb.append("Други (").append(a.people).append(" x ").append(money2(rate.ratePerOther))
            .append("): ").append(money2(d.otherShare)).append(" ").append(cur).append("\n")
        if (otherNames.isNotBlank()) sb.append("   вкл.: ").append(otherNames).append("\n")
        sb.append("ОБЩО:  ").append(money2(d.total)).append(" ").append(cur).append("\n\n")
        sb.append(if (row.paid) "Статус: ПЛАТЕНО ✓" else "Статус: НЕ Е ПЛАТЕНО")
        tv.text = sb.toString()
        tv.textSize = 15f
        box.addView(tv)

        var rec: PaymentRecord? = null
        var sig: android.graphics.Bitmap? = null
        if (row.paid) {
            rec = Db(this).paymentFor(period, a.number)
            sig = if (rec != null) Signatures.load(this, rec.uuid) else null

            if (rec != null && rec.extraAmount != 0.0) {
                val exTv = TextView(this)
                val lbl = if (rec.extraLabel.isNotBlank()) rec.extraLabel else "Допълнително"
                exTv.text = "\n" + lbl + ": " + money2(rec.extraAmount) + " " + cur +
                    "\nПлатено общо: " + money2(rec.amount) + " " + cur
                exTv.textSize = 15f
                box.addView(exTv)
            }

            val hint = TextView(this)
            hint.text = "Подпис:"
            hint.textSize = 13f
            val hLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hLp.topMargin = (12 * density).toInt()
            hint.layoutParams = hLp
            box.addView(hint)

            if (sig != null) {
                val iv = ImageView(this)
                iv.setImageBitmap(sig)
                iv.adjustViewBounds = true
                val ivLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                ivLp.topMargin = (4 * density).toInt()
                iv.layoutParams = ivLp
                val bg = GradientDrawable()
                bg.setColor(Color.WHITE)
                bg.setStroke((2 * density).toInt(), Color.GRAY)
                iv.background = bg
                val ip = (4 * density).toInt()
                iv.setPadding(ip, ip, ip, ip)
                box.addView(iv)
            } else {
                val no = TextView(this)
                no.text = "(няма записан подпис)"
                no.textSize = 13f
                box.addView(no)
            }
        }

        val scroll = ScrollView(this)
        scroll.addView(box)

        val b = AlertDialog.Builder(this)
            .setTitle("Ап. " + a.number + (if (a.name.isNotBlank()) " — " + a.name else ""))
            .setView(scroll)
            .setNegativeButton("Затвори", null)
        if (row.paid) {
            val recF = rec
            val sigF = sig
            b.setPositiveButton("Печат отново") { _, _ ->
                printLines(Receipts.payment(this, a, d, rate, period, recF?.ts ?: System.currentTimeMillis(), exps, recF?.extraLabel ?: "", recF?.extraAmount ?: 0.0, sigF))
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
