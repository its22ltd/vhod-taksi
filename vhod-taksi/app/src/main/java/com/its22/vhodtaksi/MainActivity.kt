package com.its22.vhodtaksi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var tvSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureBtPermissions()

        tvSummary = findViewById(R.id.tvSummary)
        rv = findViewById(R.id.rvApts)
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        findViewById<MaterialButton>(R.id.btnData).setOnClickListener {
            startActivity(Intent(this, DataActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnReport).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val apts = Prefs.apartments(this)
        val exps = Prefs.expenses(this)
        val incs = Prefs.incomes(this)
        val period = Prefs.period(this)
        val rate = Calc.compute(apts, exps, incs)
        val paid = Db(this).paidApartments(period)

        val rows = apts.map { a -> ApartmentAdapter.Row(a, Calc.due(a, rate), paid.contains(a.number)) }
        rv.adapter = ApartmentAdapter(rows, Prefs.currency(this)) { row ->
            if (row.paid) reprintDialog(row, rate, period)
            else collectDialog(row, rate, period)
        }

        val (cnt, sum) = Db(this).sumForPeriod(period)
        title = Prefs.businessName(this)
        tvSummary.text = "Период: " + period + "\nСъбрано: " + eur(this, sum) +
            "   |   Платили: " + cnt + " от " + apts.size + " ап."
    }

    private fun collectDialog(row: ApartmentAdapter.Row, rate: Calc.Result, period: String) {
        val d = row.due
        val a = row.apt
        val cur = Prefs.currency(this)
        val sb = StringBuilder()
        sb.append("Персонална сума: ").append(money2(d.personal)).append(" ").append(cur).append("\n")
        if (a.paysElevator && d.elevatorShare > 0.0)
            sb.append("Асансьор (").append(a.people).append(" x ").append(money2(rate.ratePerElevator))
                .append("): ").append(money2(d.elevatorShare)).append(" ").append(cur).append("\n")
        sb.append("Други (").append(a.people).append(" x ").append(money2(rate.ratePerOther))
            .append("): ").append(money2(d.otherShare)).append(" ").append(cur).append("\n\n")
        sb.append("ОБЩО: ").append(money2(d.total)).append(" ").append(cur)

        AlertDialog.Builder(this)
            .setTitle("Ап. " + a.number + (if (a.name.isNotBlank()) " — " + a.name else ""))
            .setMessage(sb.toString())
            .setPositiveButton("Плати и печат") { _, _ -> collectAndPrint(row, rate, period) }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun reprintDialog(row: ApartmentAdapter.Row, rate: Calc.Result, period: String) {
        AlertDialog.Builder(this)
            .setTitle("Ап. " + row.apt.number + " — вече платено")
            .setMessage("Сумата " + eur(this, row.due.total) + " вече е отбелязана за " + period + ".\nДа отпечатам ли бележката отново?")
            .setPositiveButton("Печат отново") { _, _ ->
                printLines(Receipts.payment(this, row.apt, row.due, rate, period, System.currentTimeMillis()))
            }
            .setNegativeButton("Затвори", null)
            .show()
    }

    private fun collectAndPrint(row: ApartmentAdapter.Row, rate: Calc.Result, period: String) {
        val a = row.apt
        val d = row.due
        val ts = System.currentTimeMillis()
        val rec = PaymentRecord(
            id = 0,
            uuid = UUID.randomUUID().toString(),
            apt = a.number,
            name = a.name,
            people = a.people,
            period = period,
            amount = d.total,
            personal = d.personal,
            elevatorShare = d.elevatorShare,
            otherShare = d.otherShare,
            ts = ts,
            synced = false
        )
        Db(this).insert(rec)
        refresh()
        printLines(Receipts.payment(this, a, d, rate, period, ts))
        trySyncSilently()
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
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Грешка при печат: " + (e.message ?: "") + "\n(Плащането е записано - виж Отчет за повторен печат)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun trySyncSilently() {
        if (Prefs.serverUrl(this).isBlank()) return
        Thread {
            try {
                Sync.push(this)
            } catch (e: Exception) {
                // тихо - ще се синхронизира по-късно (ръчно от Настройки)
            }
        }.start()
    }

    private fun ensureBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val perms = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val need = perms.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (need) ActivityCompat.requestPermissions(this, perms, 1)
        }
    }
}
