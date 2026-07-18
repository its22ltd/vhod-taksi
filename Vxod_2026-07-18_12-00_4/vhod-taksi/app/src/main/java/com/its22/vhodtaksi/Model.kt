package com.its22.vhodtaksi

import android.content.Context
import java.util.Locale

/** Версия на приложението - показва се в Настройки, за да се вижда коя версия е инсталирана */
const val APP_VERSION = "Vxod · 2026-07-18 12:00 · билд 4"

/** Апартамент от входа */
data class Apartment(
    val number: String,
    val name: String,
    val people: Int,
    val personal: Double,
    val paysElevator: Boolean,
    val phone: String = ""
)

/** Разходно перо (пример: "Почистване 41 евро", група "други") */
data class ExpenseItem(
    val label: String,
    val amount: Double,
    val elevator: Boolean // true = влиза в дела за асансьор, false = в "други"
)

/** Приход (пример: "Наем на помещение 100 евро") */
data class IncomeItem(
    val label: String,
    val amount: Double
)

fun money2(v: Double): String = String.format(Locale.US, "%.2f", v)

fun eur(ctx: Context, v: Double): String = money2(v) + " " + Prefs.currency(ctx)
