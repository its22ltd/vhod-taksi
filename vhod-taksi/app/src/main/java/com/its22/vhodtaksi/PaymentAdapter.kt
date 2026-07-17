package com.its22.vhodtaksi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentAdapter(
    private val items: List<PaymentRecord>,
    private val currency: String,
    private val onClick: (PaymentRecord) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.VH>() {

    private val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("bg", "BG"))

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val sub: TextView = v.findViewById(R.id.tvSub)
        val amt: TextView = v.findViewById(R.id.tvAmt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        val nm = if (r.name.isNotBlank()) " • " + r.name else ""
        holder.title.text = "Ап. " + r.apt + nm
        holder.sub.text = df.format(Date(r.ts)) + "  •  " + r.period + (if (r.synced) "  •  ✓" else "")
        holder.amt.text = money2(r.amount) + " " + currency
        holder.itemView.setOnClickListener { onClick(r) }
    }

    override fun getItemCount(): Int = items.size
}
