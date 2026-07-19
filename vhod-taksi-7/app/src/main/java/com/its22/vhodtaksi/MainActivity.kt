package com.its22.vhodtaksi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var btnPeriod: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureBtPermissions()

        tvSummary = findViewById(R.id.tvSummary)
        btnPeriod = findViewById(R.id.btnPeriod)
        rv = findViewById(R.id.rvApts)
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        btnPeriod.setOnClickListener { showPeriodDialog() }
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
        val period = Prefs.period(this)
        val apts = Prefs.apartments(this, period)
        val exps = Prefs.expenses(this, period)
        val incs = Prefs.incomes(this, period)
        val rate = Calc.compute(apts, exps, incs)
        val paid = Db(this).paidApartments(period)

        val rows = apts.map { a -> ApartmentAdapter.Row(a, Calc.due(a, rate), paid.contains(a.number)) }
        rv.adapter = ApartmentAdapter(rows, Prefs.currency(this)) { row ->
            if (row.paid) reprintDialog(row, rate, period)
            else beforeCollect(row, rate, period)
        }

        val (cnt, sum) = Db(this).sumForPeriod(period)
        title = Prefs.businessName(this)
        btnPeriod.text = "Месец: " + period + "  ▾"
        tvSummary.text = "Събрано: " + eur(this, sum) + "   |   Платили: " + cnt + " от " + apts.size + " ап."
    }

    private fun showPeriodDialog() {
        val periods = Prefs.periods(this)
        val items = ArrayList<String>()
        for (p in periods) items.add("📅  " + p)
        val editIdx = items.size
        items.add("✎  Редактирай данните за " + Prefs.period(this))
        items.add("＋  Генерирай нов месец")
        val arr = items.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Месец")
            .setItems(arr) { _, which ->
                when {
                    which < periods.size -> {
                        Prefs.setPeriod(this, periods[which])
                        refresh()
                    }
                    which == editIdx -> startActivity(Intent(this, DataActivity::class.java))
                    else -> newMonthDialog()
                }
            }
            .show()
    }

    private fun newMonthDialog() {
        val cur = Prefs.period(this)
        val suggested = Prefs.suggestNextPeriod(cur)
        val et = EditText(this)
        et.setText(suggested)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad / 2, pad, 0)
        box.addView(et)

        AlertDialog.Builder(this)
            .setTitle("Нов месец")
            .setMessage("Ще се копират апартаментите и разходите от " + cur + ". После можеш да нанесеш промени. Всички ще са отбелязани като неплатили.")
            .setView(box)
            .setPositiveButton("Създай и редактирай") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isEmpty()) { toast("Въведете месец"); return@setPositiveButton }
                if (Prefs.periods(this).contains(name)) {
                    toast("Такъв месец вече съществува — избери го от списъка")
                    return@setPositiveButton
                }
                Prefs.generateNewMonth(this, name)
                refresh()
                startActivity(Intent(this, DataActivity::class.java))
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun beforeCollect(row: ApartmentAdapter.Row, rate: Calc.Result, period: String) {
        val a = row.apt
        if (a.name.trim().isBlank() || a.phone.trim().isBlank()) {
            promptAptData(row, rate, period)
        } else {
            collectDialog(row, rate, period)
        }
    }

    private fun promptAptData(row: ApartmentAdapter.Row, rate: Calc.Result, period: String) {
        val a = row.apt
        val pad = (16 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad / 2, pad, 0)
        val info = TextView(this)
        info.text = "Липсват данни — попълни преди плащане:"
        info.textSize = 13f
        box.addView(info)
        val nameE = EditText(this)
        nameE.hint = "Име на живущия"
        nameE.setText(a.name)
        nameE.inputType = InputType.TYPE_CLASS_TEXT
        box.addView(nameE)
        val phoneE = EditText(this)
        phoneE.hint = "Телефон"
        phoneE.setText(a.phone)
        phoneE.inputType = InputType.TYPE_CLASS_PHONE
        box.addView(phoneE)

        AlertDialog.Builder(this)
            .setTitle("Ап. " + a.number + " — данни")
            .setView(box)
            .setPositiveButton("Запази и продължи") { _, _ ->
                val nm = nameE.text.toString().trim()
                val ph = phoneE.text.toString().trim()
                if (nm.isBlank() || ph.isBlank()) {
                    toast("Въведете и име, и телефон")
                    promptAptData(row, rate, period)
                    return@setPositiveButton
                }
                val list = Prefs.apartments(this, period).toMutableList()
                val idx = list.indexOfFirst { it.number == a.number }
                val updated = a.copy(name = nm, phone = ph)
                if (idx >= 0) list[idx] = updated else list.add(updated)
                Prefs.setApartments(this, period, list)
                refresh()
                collectDialog(ApartmentAdapter.Row(updated, row.due, false), rate, period)
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun collectDialog(row: ApartmentAdapter.Row, rate: Calc.Result, period: String) {
        val d = row.due
        val a = row.apt
        val cur = Prefs.currency(this)
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()

        val sb = StringBuilder()
        sb.append("Персонална сума: ").append(money2(d.personal)).append(" ").append(cur).append("\n")
        if (a.paysElevator && d.elevatorShare > 0.0)
            sb.append("Асансьор (").append(a.people).append(" x ").append(money2(rate.ratePerElevator))
                .append("): ").append(money2(d.elevatorShare)).append(" ").append(cur).append("\n")
        sb.append("Други (").append(a.people).append(" x ").append(money2(rate.ratePerOther))
            .append("): ").append(money2(d.otherShare)).append(" ").append(cur).append("\n\n")
        sb.append("ОБЩО:  ").append(money2(d.total)).append(" ").append(cur)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad / 2, pad, 0)

        val tv = TextView(this)
        tv.text = sb.toString()
        tv.textSize = 15f
        box.addView(tv)

        // Допълнително перо (по избор)
        val etExtraLabel = EditText(this)
        etExtraLabel.hint = "Допълнително (напр. чип за врата)"
        etExtraLabel.inputType = InputType.TYPE_CLASS_TEXT
        val elLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        elLp.topMargin = (10 * density).toInt()
        etExtraLabel.layoutParams = elLp
        box.addView(etExtraLabel)

        val etExtraAmount = EditText(this)
        etExtraAmount.hint = "Сума на допълнителното"
        etExtraAmount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        box.addView(etExtraAmount)

        val tvTotal = TextView(this)
        tvTotal.textSize = 18f
        tvTotal.setTypeface(tvTotal.typeface, android.graphics.Typeface.BOLD)
        val ttLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        ttLp.topMargin = (8 * density).toInt()
        tvTotal.layoutParams = ttLp
        box.addView(tvTotal)

        fun currentExtra(): Double = etExtraAmount.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
        fun updateTotal() {
            tvTotal.text = "ОБЩО за плащане: " + money2(d.total + currentExtra()) + " " + cur
        }
        updateTotal()
        etExtraAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateTotal() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        val hint = TextView(this)
        hint.text = "Подпис на живущия (с пръст):"
        hint.textSize = 13f
        val hLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        hLp.topMargin = (12 * density).toInt()
        hint.layoutParams = hLp
        box.addView(hint)

        val sig = SignatureView(this)
        val sLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (260 * density).toInt()
        )
        sLp.topMargin = (4 * density).toInt()
        sig.layoutParams = sLp
        val bg = GradientDrawable()
        bg.setColor(Color.WHITE)
        bg.setStroke((2 * density).toInt(), Color.GRAY)
        sig.background = bg
        box.addView(sig)

        val clear = Button(this)
        clear.text = "Изчисти подписа"
        clear.setOnClickListener { sig.clear() }
        box.addView(clear)

        val scroll = ScrollView(this)
        scroll.addView(box)

        AlertDialog.Builder(this)
            .setTitle("Ап. " + a.number + (if (a.name.isNotBlank()) " — " + a.name else ""))
            .setView(scroll)
            .setPositiveButton("Плати и печат") { _, _ ->
                collect(row, rate, period, true, if (sig.hasContent) sig.getBitmap() else null,
                    etExtraLabel.text.toString().trim(), currentExtra())
            }
            .setNeutralButton("Плати без печат") { _, _ ->
                collect(row, rate, period, false, if (sig.hasContent) sig.getBitmap() else null,
                    etExtraLabel.text.toString().trim(), currentExtra())
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private fun reprintDialog(row: ApartmentAdapter.Row, rate: Calc.Result, period: String) {
        AlertDialog.Builder(this)
            .setTitle("Ап. " + row.apt.number + " — вече платено")
            .setMessage("Сумата " + eur(this, row.due.total) + " вече е отбелязана за " + period + ".\nДа отпечатам ли бележката отново?")
            .setPositiveButton("Печат отново") { _, _ ->
                val rec = Db(this).paymentFor(period, row.apt.number)
                val sig = if (rec != null) Signatures.load(this, rec.uuid) else null
                printLines(Receipts.payment(this, row.apt, row.due, rate, period, rec?.ts ?: System.currentTimeMillis(), Prefs.expenses(this, period), rec?.extraLabel ?: "", rec?.extraAmount ?: 0.0, sig))
            }
            .setNegativeButton("Затвори", null)
            .show()
    }

    private fun collect(
        row: ApartmentAdapter.Row,
        rate: Calc.Result,
        period: String,
        print: Boolean,
        signature: Bitmap?,
        extraLabel: String,
        extraAmount: Double
    ) {
        val a = row.apt
        val d = row.due
        val ts = System.currentTimeMillis()
        val total = Calc.round2(d.total + extraAmount)
        val rec = PaymentRecord(
            id = 0,
            uuid = UUID.randomUUID().toString(),
            apt = a.number,
            name = a.name,
            people = a.people,
            period = period,
            amount = total,
            personal = d.personal,
            elevatorShare = d.elevatorShare,
            otherShare = d.otherShare,
            ts = ts,
            synced = false,
            extraLabel = extraLabel,
            extraAmount = extraAmount
        )
        Db(this).insert(rec)
        Signatures.save(this, rec.uuid, signature)
        refresh()
        if (print) {
            printLines(Receipts.payment(this, a, d, rate, period, ts, Prefs.expenses(this, period), extraLabel, extraAmount, signature))
        } else {
            Toast.makeText(this, "Отбелязано като платено (без печат)", Toast.LENGTH_SHORT).show()
        }
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
                        "Грешка при печат: " + (e.message ?: "") + "\n(Плащането е записано - виж Справка за повторен печат)",
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
                // тихо
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

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
