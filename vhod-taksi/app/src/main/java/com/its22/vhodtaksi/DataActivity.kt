package com.its22.vhodtaksi

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Екран "Данни" - апартаменти, разходи и приходи се въвеждат като текст,
 * по един ред (както в таблица). Приложението смята дела на човек и дължимото.
 */
class DataActivity : AppCompatActivity() {

    private lateinit var etPeriod: EditText
    private lateinit var etApts: EditText
    private lateinit var etExps: EditText
    private lateinit var etIncs: EditText
    private lateinit var tvPreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)
        title = "Данни и сметка"

        etPeriod = findViewById(R.id.etPeriod)
        etApts = findViewById(R.id.etApts)
        etExps = findViewById(R.id.etExps)
        etIncs = findViewById(R.id.etIncs)
        tvPreview = findViewById(R.id.tvPreview)

        etPeriod.setText(Prefs.period(this))
        etApts.setText(aptsToText(Prefs.apartments(this)))
        etExps.setText(expsToText(Prefs.expenses(this)))
        etIncs.setText(incsToText(Prefs.incomes(this)))

        findViewById<MaterialButton>(R.id.btnPreview).setOnClickListener { updatePreview() }
        findViewById<MaterialButton>(R.id.btnSaveData).setOnClickListener { save() }
        updatePreview()
    }

    private fun save() {
        val apts = parseApts(etApts.text.toString())
        if (apts.isEmpty()) {
            toast("Добавете поне един апартамент")
            return
        }
        Prefs.setApartments(this, apts)
        Prefs.setExpenses(this, parseExps(etExps.text.toString()))
        Prefs.setIncomes(this, parseIncs(etIncs.text.toString()))
        val period = etPeriod.text.toString().trim().ifEmpty { Prefs.period(this) }
        Prefs.setPeriod(this, period)
        toast("Запазено")
        updatePreview()
        finish()
    }

    private fun updatePreview() {
        val apts = parseApts(etApts.text.toString())
        val exps = parseExps(etExps.text.toString())
        val incs = parseIncs(etIncs.text.toString())
        val cur = Prefs.currency(this)
        if (apts.isEmpty()) {
            tvPreview.text = "Няма апартаменти."
            return
        }
        val r = Calc.compute(apts, exps, incs)
        val totalDue = Calc.totalDue(apts, r)
        val sb = StringBuilder()
        sb.append("Разходи общо: ").append(money2(r.totalExpenses)).append(" ").append(cur)
        sb.append("  (асансьор ").append(money2(r.elevatorSum)).append(" + други ").append(money2(r.otherSum)).append(")\n")
        sb.append("Приходи общо: ").append(money2(r.totalIncome)).append(" ").append(cur).append("\n")
        sb.append("Хора: общо ").append(r.peopleTotal).append(", с асансьор ").append(r.peopleElevator).append("\n")
        sb.append("Дял/човек — асансьор: ").append(money2(r.ratePerElevator)).append(" ").append(cur)
        sb.append(", други: ").append(money2(r.ratePerOther)).append(" ").append(cur).append("\n")
        sb.append("Дължимо от всички: ").append(money2(totalDue)).append(" ").append(cur).append("\n\n")
        sb.append("Примерно дължимо:\n")
        for (a in apts.take(5)) {
            val d = Calc.due(a, r)
            sb.append("Ап. ").append(a.number).append(": ").append(money2(d.total)).append(" ").append(cur).append("\n")
        }
        if (apts.size > 5) sb.append("...")
        tvPreview.text = sb.toString()
    }

    // ---- сериализация към текст ----
    private fun aptsToText(list: List<Apartment>): String =
        list.joinToString("\n") {
            it.number + " ; " + it.name + " ; " + it.people + " ; " + money2(it.personal) +
                " ; " + (if (it.paysElevator) "да" else "не")
        }

    private fun expsToText(list: List<ExpenseItem>): String =
        list.joinToString("\n") {
            it.label + " ; " + money2(it.amount) + " ; " + (if (it.elevator) "асансьор" else "други")
        }

    private fun incsToText(list: List<IncomeItem>): String =
        list.joinToString("\n") { it.label + " ; " + money2(it.amount) }

    // ---- парсване от текст ----
    private fun num(s: String): Double = s.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    private fun int(s: String): Int = s.trim().toIntOrNull() ?: 0

    private fun parseApts(text: String): List<Apartment> {
        val out = ArrayList<Apartment>()
        for (raw in text.split("\n")) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val p = line.split(";")
            val number = p.getOrNull(0)?.trim() ?: ""
            if (number.isEmpty()) continue
            val name = p.getOrNull(1)?.trim() ?: ""
            val people = int(p.getOrNull(2) ?: "0")
            val personal = num(p.getOrNull(3) ?: "0")
            val elevRaw = (p.getOrNull(4) ?: "да").trim().lowercase()
            val elev = !(elevRaw.startsWith("н") || elevRaw.startsWith("n") || elevRaw == "0")
            out.add(Apartment(number, name, people, personal, elev))
        }
        return out
    }

    private fun parseExps(text: String): List<ExpenseItem> {
        val out = ArrayList<ExpenseItem>()
        for (raw in text.split("\n")) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val p = line.split(";")
            val label = p.getOrNull(0)?.trim() ?: ""
            if (label.isEmpty()) continue
            val amount = num(p.getOrNull(1) ?: "0")
            val grp = (p.getOrNull(2) ?: "други").trim().lowercase()
            val elev = grp.startsWith("а") || grp.startsWith("a")
            out.add(ExpenseItem(label, amount, elev))
        }
        return out
    }

    private fun parseIncs(text: String): List<IncomeItem> {
        val out = ArrayList<IncomeItem>()
        for (raw in text.split("\n")) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val p = line.split(";")
            val label = p.getOrNull(0)?.trim() ?: ""
            if (label.isEmpty()) continue
            val amount = num(p.getOrNull(1) ?: "0")
            out.add(IncomeItem(label, amount))
        }
        return out
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
