package com.its22.vhodtaksi

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Табличен екран за данните на текущия период - редактираш поле и всички "Дължи"
 * се преизчисляват веднага. При запис изисква име и телефон, ако липсват.
 */
class DataActivity : AppCompatActivity() {

    private class AptRow(
        var num: String, var name: String, var phone: String,
        var people: Int, var personal: Double, var elev: Boolean
    )
    private class ExpRow(var label: String, var amount: Double, var elev: Boolean)
    private class IncRow(var label: String, var amount: Double)

    private lateinit var period: String
    private val rows = mutableListOf<AptRow>()
    private val exps = mutableListOf<ExpRow>()
    private val incs = mutableListOf<IncRow>()
    private val dueViews = ArrayList<TextView>()

    private lateinit var aptTable: TableLayout
    private lateinit var expTable: TableLayout
    private lateinit var tvPreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)
        period = Prefs.period(this)
        title = "Данни — " + period

        Prefs.apartments(this, period).forEach { rows.add(AptRow(it.number, it.name, it.phone, it.people, it.personal, it.paysElevator)) }
        Prefs.expenses(this, period).forEach { exps.add(ExpRow(it.label, it.amount, it.elevator)) }
        Prefs.incomes(this, period).forEach { incs.add(IncRow(it.label, it.amount)) }

        aptTable = findViewById(R.id.aptTable)
        expTable = findViewById(R.id.expTable)
        tvPreview = findViewById(R.id.tvPreview)

        findViewById<MaterialButton>(R.id.btnAddApt).setOnClickListener {
            rows.add(AptRow((rows.size + 1).toString(), "", "", 0, 0.0, true)); renderApt(); recompute()
        }
        findViewById<MaterialButton>(R.id.btnAddExp).setOnClickListener {
            exps.add(ExpRow("Ново перо", 0.0, false)); renderExp(); recompute()
        }
        findViewById<MaterialButton>(R.id.btnSaveData).setOnClickListener { onSave() }

        renderApt()
        renderExp()
        recompute()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun headerCell(text: String, wdp: Int): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextColor(0xFFFFFFFF.toInt())
        t.setBackgroundColor(0xFF00796B.toInt())
        t.gravity = Gravity.CENTER
        t.setPadding(dp(6), dp(8), dp(6), dp(8))
        t.textSize = 12f
        if (wdp > 0) t.width = dp(wdp)
        return t
    }

    private fun editCell(value: String, inputType: Int, wdp: Int, onChange: (String) -> Unit): EditText {
        val e = EditText(this)
        e.setText(value)
        e.inputType = inputType
        e.setPadding(dp(4), dp(6), dp(4), dp(6))
        e.textSize = 13f
        e.gravity = Gravity.CENTER
        e.setBackgroundColor(0xFFF7F7F7.toInt())
        if (wdp > 0) e.width = dp(wdp)
        e.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { onChange(e.text.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        return e
    }

    private fun delCell(onClick: () -> Unit): TextView {
        val d = TextView(this)
        d.text = "✕"
        d.setTextColor(0xFFC62828.toInt())
        d.gravity = Gravity.CENTER
        d.width = dp(34)
        d.setPadding(dp(4), dp(6), dp(4), dp(6))
        d.setOnClickListener { onClick() }
        return d
    }

    private fun renderApt() {
        aptTable.removeAllViews()
        dueViews.clear()
        val hr = TableRow(this)
        hr.addView(headerCell("№", 44))
        hr.addView(headerCell("Име", 120))
        hr.addView(headerCell("Тел.", 110))
        hr.addView(headerCell("Хора", 52))
        hr.addView(headerCell("Перс.", 60))
        hr.addView(headerCell("Асан", 46))
        hr.addView(headerCell("Дължи", 76))
        hr.addView(headerCell("", 34))
        aptTable.addView(hr)

        for (i in rows.indices) {
            val r = rows[i]
            val tr = TableRow(this)
            tr.addView(editCell(r.num, InputType.TYPE_CLASS_TEXT, 44) { r.num = it })
            tr.addView(editCell(r.name, InputType.TYPE_CLASS_TEXT, 120) { r.name = it })
            tr.addView(editCell(r.phone, InputType.TYPE_CLASS_PHONE, 110) { r.phone = it })
            tr.addView(editCell(r.people.toString(), InputType.TYPE_CLASS_NUMBER, 52) { r.people = it.toIntOrNull() ?: 0; recompute() })
            tr.addView(editCell(money2(r.personal), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, 60) { r.personal = it.replace(',', '.').toDoubleOrNull() ?: 0.0; recompute() })
            val cb = CheckBox(this)
            cb.isChecked = r.elev
            cb.setOnCheckedChangeListener { _, checked -> r.elev = checked; recompute() }
            tr.addView(cb)
            val due = TextView(this)
            due.gravity = Gravity.CENTER
            due.setPadding(dp(4), dp(6), dp(4), dp(6))
            due.textSize = 13f
            due.width = dp(76)
            due.setTypeface(due.typeface, android.graphics.Typeface.BOLD)
            tr.addView(due)
            dueViews.add(due)
            tr.addView(delCell { rows.removeAt(i); renderApt(); recompute() })
            aptTable.addView(tr)
        }
    }

    private fun renderExp() {
        expTable.removeAllViews()
        val hr = TableRow(this)
        hr.addView(headerCell("Перо", 140))
        hr.addView(headerCell("Сума", 70))
        hr.addView(headerCell("Асан?", 50))
        hr.addView(headerCell("", 34))
        expTable.addView(hr)
        for (i in exps.indices) {
            val x = exps[i]
            val tr = TableRow(this)
            tr.addView(editCell(x.label, InputType.TYPE_CLASS_TEXT, 140) { x.label = it })
            tr.addView(editCell(money2(x.amount), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, 70) { x.amount = it.replace(',', '.').toDoubleOrNull() ?: 0.0; recompute() })
            val cb = CheckBox(this)
            cb.isChecked = x.elev
            cb.setOnCheckedChangeListener { _, checked -> x.elev = checked; recompute() }
            tr.addView(cb)
            tr.addView(delCell { exps.removeAt(i); renderExp(); recompute() })
            expTable.addView(tr)
        }
    }

    private fun toApts(): List<Apartment> = rows.map { Apartment(it.num, it.name, it.people, it.personal, it.elev, it.phone) }
    private fun toExps(): List<ExpenseItem> = exps.map { ExpenseItem(it.label, it.amount, it.elev) }
    private fun toIncs(): List<IncomeItem> = incs.map { IncomeItem(it.label, it.amount) }

    private fun recompute() {
        val apts = toApts()
        val r = Calc.compute(apts, toExps(), toIncs())
        for (i in apts.indices) {
            if (i < dueViews.size) dueViews[i].text = money2(Calc.due(apts[i], r).total)
        }
        val cur = Prefs.currency(this)
        val td = Calc.totalDue(apts, r)
        tvPreview.text = "Разходи: " + money2(r.totalExpenses) + " " + cur +
            "  (асансьор " + money2(r.elevatorSum) + " + други " + money2(r.otherSum) + ")\n" +
            "Хора: " + r.peopleTotal + " (с асансьор " + r.peopleElevator + ")\n" +
            "Дял/чов — асансьор " + money2(r.ratePerElevator) + ", други " + money2(r.ratePerOther) + "\n" +
            "Дължимо от всички: " + money2(td) + " " + cur
    }

    private fun onSave() {
        if (rows.isEmpty()) { toast("Добавете поне един апартамент"); return }
        val idx = rows.indexOfFirst { it.name.trim().isBlank() || it.phone.trim().isBlank() }
        if (idx >= 0) { promptMissing(idx); return }
        Prefs.setApartments(this, period, rows.map { Apartment(it.num.trim(), it.name.trim(), it.people, it.personal, it.elev, it.phone.trim()) })
        Prefs.setExpenses(this, period, exps.map { ExpenseItem(it.label.trim(), it.amount, it.elev) })
        Prefs.setIncomes(this, period, incs.map { IncomeItem(it.label.trim(), it.amount) })
        toast("Запазено")
        finish()
    }

    private fun promptMissing(idx: Int) {
        val r = rows[idx]
        val missing = rows.count { it.name.trim().isBlank() || it.phone.trim().isBlank() }
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val p = dp(16)
        box.setPadding(p, dp(4), p, 0)
        val info = TextView(this)
        info.text = "Още " + missing + " ап. без данни. Попълни преди запис:"
        info.setTextColor(0xFFC62828.toInt())
        info.textSize = 13f
        box.addView(info)
        val nameE = EditText(this)
        nameE.hint = "Име на живущия"
        nameE.setText(r.name)
        nameE.inputType = InputType.TYPE_CLASS_TEXT
        box.addView(nameE)
        val phoneE = EditText(this)
        phoneE.hint = "Телефон"
        phoneE.setText(r.phone)
        phoneE.inputType = InputType.TYPE_CLASS_PHONE
        box.addView(phoneE)

        AlertDialog.Builder(this)
            .setTitle("Ап. " + r.num + " — липсват данни")
            .setView(box)
            .setPositiveButton("Запази и напред") { _, _ ->
                val nm = nameE.text.toString().trim()
                val ph = phoneE.text.toString().trim()
                if (nm.isBlank() || ph.isBlank()) {
                    toast("Въведете и име, и телефон")
                    promptMissing(idx)
                    return@setPositiveButton
                }
                r.name = nm
                r.phone = ph
                renderApt()
                recompute()
                onSave()
            }
            .setNegativeButton("По-късно", null)
            .show()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
