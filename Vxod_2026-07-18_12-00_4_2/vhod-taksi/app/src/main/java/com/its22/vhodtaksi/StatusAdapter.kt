package com.its22.vhodtaksi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StatusAdapter(
    private val rows: List<Row>,
    private val currency: String,
    private val onClick: (Row) -> Unit
) : RecyclerView.Adapter<StatusAdapter.VH>() {

    data class Row(val apt: Apartment, val due: Calc.Due, val paid: Boolean)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val apt: TextView = v.findViewById(R.id.tvApt)
        val sub: TextView = v.findViewById(R.id.tvSub)
        val due: TextView = v.findViewById(R.id.tvDue)
        val paid: TextView = v.findViewById(R.id.tvPaid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_apartment, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = rows[position]
        val nm = if (r.apt.name.isNotBlank()) "  " + r.apt.name else ""
        holder.apt.text = "Ап. " + r.apt.number + nm
        holder.sub.text = "перс " + money2(r.due.personal) +
            " + асан " + money2(r.due.elevatorShare) +
            " + др " + money2(r.due.otherShare)
        holder.due.text = money2(r.due.total) + " " + currency
        if (r.paid) {
            holder.paid.text = "ПЛАТЕНО ✓"
            holder.paid.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            holder.paid.text = "НЕ Е ПЛАТЕНО"
            holder.paid.setTextColor(Color.parseColor("#C62828"))
        }
        holder.itemView.setOnClickListener { onClick(r) }
    }

    override fun getItemCount(): Int = rows.size
}
