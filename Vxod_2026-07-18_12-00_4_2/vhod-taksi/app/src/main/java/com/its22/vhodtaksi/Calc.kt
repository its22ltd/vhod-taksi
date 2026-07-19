package com.its22.vhodtaksi

/**
 * Сметката на входа - точно като Excel-а:
 *  - Разходите се делят на две групи: "асансьор" и "други".
 *  - Дял за асансьор на човек = сбор_асансьор / брой_хора_плащащи_асансьор
 *  - Дял други на човек        = сбор_други / общ_брой_хора
 *  - Всеки апартамент дължи = персонална сума
 *                             + хора * дял_други
 *                             + (ако плаща асансьор) хора * дял_асансьор
 */
object Calc {

    data class Result(
        val elevatorSum: Double,
        val otherSum: Double,
        val totalExpenses: Double,
        val totalIncome: Double,
        val peopleTotal: Int,
        val peopleElevator: Int,
        val ratePerElevator: Double,
        val ratePerOther: Double
    )

    data class Due(
        val personal: Double,
        val elevatorShare: Double,
        val otherShare: Double
    ) {
        val total: Double get() = round2(personal + elevatorShare + otherShare)
    }

    fun compute(
        apts: List<Apartment>,
        exps: List<ExpenseItem>,
        incs: List<IncomeItem>
    ): Result {
        val elevatorSum = exps.filter { it.elevator }.sumOf { it.amount }
        val otherSum = exps.filter { !it.elevator }.sumOf { it.amount }
        val totalIncome = incs.sumOf { it.amount }
        val peopleTotal = apts.sumOf { it.people }
        val peopleElevator = apts.filter { it.paysElevator }.sumOf { it.people }
        val ratePerElevator = if (peopleElevator > 0) elevatorSum / peopleElevator else 0.0
        val ratePerOther = if (peopleTotal > 0) otherSum / peopleTotal else 0.0
        return Result(
            elevatorSum, otherSum, elevatorSum + otherSum, totalIncome,
            peopleTotal, peopleElevator, ratePerElevator, ratePerOther
        )
    }

    fun due(apt: Apartment, r: Result): Due {
        val elev = if (apt.paysElevator) apt.people * r.ratePerElevator else 0.0
        val other = apt.people * r.ratePerOther
        return Due(round2(apt.personal), round2(elev), round2(other))
    }

    /** Сбор на всички дължими суми (за отчета) */
    fun totalDue(apts: List<Apartment>, r: Result): Double =
        round2(apts.sumOf { due(it, r).total })

    fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0
}
