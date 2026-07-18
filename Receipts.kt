package com.its22.vhodtaksi

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Визуален екран за данните на текущия период:
 * апартаменти, разходи и приходи се редактират чрез полета и бутони.
 */
class DataActivity : AppCompatActivity() {

    private lateinit var period: String
    private lateinit var apts: MutableList<Apartment>
    private lateinit var exps: MutableList<ExpenseItem>
    private lateinit var incs: MutableList<IncomeItem>

    private lateinit var containerApts: LinearLayout
    private lateinit var containerExps: LinearLayout
    private lateinit var containerIncs: LinearLayout
    private lateinit var tvPreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        period = Prefs.period(this)
        title = "Данни — " + period
        apts = Prefs.apartments(this, period)
        exps = Prefs.expenses(this, period)
        incs = Prefs.incomes(this, period)

        containerApts = findViewById(R.id.containerApts)
        containerExps = findViewById(R.id.containerExps)
        containerIncs = findViewById(R.id.containerIncs)
        tvPreview = findViewById(R.id.tvPreview)

        findViewById<MaterialButton>(R.id.btnAddApt).setOnClickListener { editApartment(-1) }
        findViewById<MaterialButton>(R.id.btnAddExp).setOnClickListener { editExpense(-1) }
        findViewById<MaterialButton>(R.id.btnAddInc).setOnClickListener { editIncome(-1) }
        findViewById<MaterialButton>(R.id.btnSaveData).setOnClickListener { save() }

        renderAll()
    }

    private fun renderAll() {
        renderApts()
        renderExps()
        renderIncs()
        updatePreview()
    }

    // ---------- редове ----------
    private fun makeRow(text: String, onClick: () -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val pad = dp(10)
        row.setPadding(pad, pad, pad, pad)
        row.isClickable = true
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tv.layoutParams = lp
        val edit = TextView(this)
        edit.text = "✎"
        edit.textSize = 20f
        row.addView(tv)
        row.addView(edit)
        row.setOnClickListener { onClick() }
        row.setBackgroundResource(android.R.drawable.list_selector_background)
        return row
    }

    private fun renderApts() {
        containerApts.removeAllViews()
        for (i in apts.indices) {
            val a = apts[i]
            val nm = if (a.name.isNotBlank()) a.name + " • " else ""
            val elev = if (a.paysElevator) "асансьор:да" else "асансьор:не"
            val txt = "Ап. " + a.number + "   " + nm + a.people + " хора • " +
                money2(a.personal) + " перс. • " + elev
            containerApts.addView(makeRow(txt) { editApartment(i) })
        }
    }

    private fun renderExps() {
        containerExps.removeAllViews()
        for (i in exps.indices) {
            val e = exps[i]
            val grp = if (e.elevator) "асансьор" else "други"
            val txt = e.label + "   " + money2(e.amount) + " • " + grp
            containerExps.addView(makeRow(txt) { editExpense(i) })
        }
    }

    private fun renderIncs() {
        containerIncs.removeAllViews()
        for (i in incs.indices) {
            val inc = incs[i]
            val txt = inc.label + "   " + money2(inc.amount)
            containerIncs.addView(makeRow(txt) { editIncome(i) })
        }
    }

    // ---------- диалози ----------
    private fun editApartment(index: Int) {
        val existing = index >= 0
        val a = if (existing) apts[index] else Apartment("", "", 0, 0.0, true)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val pad = dp(16)
        box.setPadding(pad, pad / 2, pad, 0)

        val etNum = field(box, "Номер на апартамент", a.number, InputType.TYPE_CLASS_TEXT)
        val etName = field(box, "Име (по избор)", a.name, InputType.TYPE_CLASS_TEXT)
        val etPeople = field(box, "Брой хора", a.people.toString(), InputType.TYPE_CLASS_NUMBER)
        val etPersonal = field(
            box, "Персонална сума", money2(a.personal),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val sw = Switch(this)
        sw.text = "Плаща асансьор"
        sw.isChecked = a.paysElevator
        sw.textSize = 16f
        val swLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        swLp.topMargin = dp(10)
        sw.layoutParams = swLp
        box.addView(sw)

        val b = AlertDialog.Builder(this)
            .setTitle(if (existing) "Редакция на апартамент" else "Нов апартамент")
            .setView(wrapScroll(box))
            .setPositiveButton("Запази") { _, _ ->
                val num = etNum.text.toString().trim()
                if (num.isEmpty()) { toast("Въведете номер"); return@setPositiveButton }
                val newA = Apartment(
                    num,
                    etName.text.toString().trim(),
                    intOf(etPeople.text.toString()),
                    dblOf(etPersonal.text.toString()),
                    sw.isChecked
                )
                if (existing) apts[index] = newA else apts.add(newA)
                renderAll()
            }
            .setNegativeButton("Отказ", null)
        if (existing) b.setNeutralButton("Изтрий") { _, _ -> apts.removeAt(index); renderAll() }
        b.show()
    }

    private fun editExpense(index: Int) {
        val existing = index >= 0
        val e = if (existing) exps[index] else ExpenseItem("", 0.0, false)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val pad = dp(16)
        box.setPadding(pad, pad / 2, pad, 0)

        val etLabel = field(box, "Перо (напр. Почистване)", e.label, InputType.TYPE_CLASS_TEXT)
        val etAmount = field(
            box, "Сума", money2(e.amount),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val sw = Switch(this)
        sw.text = "Влиза в дела за асансьор"
        sw.isChecked = e.elevator
        sw.textSize = 16f
        val swLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        swLp.topMargin = dp(10)
        sw.layoutParams = swLp
        box.addView(sw)

        val b = AlertDialog.Builder(this)
            .setTitle(if (existing) "Редакция на разход" else "Нов разход")
            .setView(wrapScroll(box))
            .setPositiveButton("Запази") { _, _ ->
                val label = etLabel.text.toString().trim()
                if (label.isEmpty()) { toast("Въведете име на перото"); return@setPositiveButton }
                val newE = ExpenseItem(label, dblOf(etAmount.text.toString()), sw.isChecked)
                if (existing) exps[index] = newE else exps.add(newE)
                renderAll()
            }
            .setNegativeButton("Отказ", null)
        if (existing) b.setNeutralButton("Изтрий") { _, _ -> exps.removeAt(index); renderAll() }
        b.show()
    }

    private fun editIncome(index: Int) {
        val existing = index >= 0
        val inc = if (existing) incs[index] else IncomeItem("", 0.0)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val pad = dp(16)
        box.setPadding(pad, pad / 2, pad, 0)

        val etLabel = field(box, "Приход (напр. Наем)", inc.label, InputType.TYPE_CLASS_TEXT)
        val etAmount = field(
            box, "Сума", money2(inc.amount),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        val b = AlertDialog.Builder(this)
            .setTitle(if (existing) "Редакция на приход" else "Нов приход")
            .setView(wrapScroll(box))
            .setPositiveButton("Запази") { _, _ ->
                val label = etLabel.text.toString().trim()
                if (label.isEmpty()) { toast("Въведете име"); return@setPositiveButton }
                val newI = IncomeItem(label, dblOf(etAmount.text.toString()))
                if (existing) incs[index] = newI else incs.add(newI)
                renderAll()
            }
            .setNegativeButton("Отказ", null)
        if (existing) b.setNeutralButton("Изтрий") { _, _ -> incs.removeAt(index); renderAll() }
        b.show()
    }

    // ---------- запис ----------
    private fun save() {
        if (apts.isEmpty()) { toast("Добавете поне един апартамент"); return }
        Prefs.setApartments(this, period, apts)
        Prefs.setExpenses(this, period, exps)
        Prefs.setIncomes(this, period, incs)
        toast("Запазено")
        finish()
    }

    private fun updatePreview() {
        val cur = Prefs.currency(this)
        if (apts.isEmpty()) { tvPreview.text = "Няма апартаменти."; return }
        val r = Calc.compute(apts, exps, incs)
        val totalDue = Calc.totalDue(apts, r)
        val sb = StringBuilder()
        sb.append("Разходи: ").append(money2(r.totalExpenses)).append(" ").append(cur)
        sb.append("  (асансьор ").append(money2(r.elevatorSum))
        sb.append(" + други ").append(money2(r.otherSum)).append(")\n")
        sb.append("Приходи: ").append(money2(r.totalIncome)).append(" ").append(cur).append("\n")
        sb.append("Хора: ").append(r.peopleTotal).append(" (с асансьор ").append(r.peopleElevator).append(")\n")
        sb.append("Дял/чов. — асансьор ").append(money2(r.ratePerElevator))
        sb.append(", други ").append(money2(r.ratePerOther)).append("\n")
        sb.append("Дължимо от всички: ").append(money2(totalDue)).append(" ").append(cur)
        tvPreview.text = sb.toString()
    }

    // ---------- помощни ----------
    private fun field(parent: LinearLayout, hint: String, value: String, inputType: Int): EditText {
        val label = TextView(this)
        label.text = hint
        label.textSize = 13f
        val lLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lLp.topMargin = dp(8)
        label.layoutParams = lLp
        parent.addView(label)
        val et = EditText(this)
        et.setText(value)
        et.inputType = inputType
        parent.addView(et)
        return et
    }

    private fun wrapScroll(v: View): View {
        val sc = android.widget.ScrollView(this)
        sc.addView(v)
        return sc
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun intOf(s: String): Int = s.trim().toIntOrNull() ?: 0
    private fun dblOf(s: String): Double = s.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
