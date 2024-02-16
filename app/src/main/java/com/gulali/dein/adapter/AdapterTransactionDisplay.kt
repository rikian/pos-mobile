package com.gulali.dein.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gulali.dein.R
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.entities.EntityTransaction

class AdapterTransactionDisplay(
    private val context: Context,
    private val helper: Helper,
    private val isSearch: Boolean,
    private val transactions: List<EntityTransaction>
): RecyclerView.Adapter<AdapterTransactionDisplay.TransactionDisplayViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null

    inner class TransactionDisplayViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var tNumber: TextView = view.findViewById(R.id.transaction_number)
        var tID: TextView = view.findViewById(R.id.id_payment)
        var tItem: TextView = view.findViewById(R.id.transaction_item)
        var tTotal: TextView = view.findViewById(R.id.total_payment)
        var tCreate: TextView = view.findViewById(R.id.payment_created)
        var isNew: TextView = view.findViewById(R.id.is_new)
        val payTime: TextView = view.findViewById(R.id.pay_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionDisplayViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false)
        val viewHolder = TransactionDisplayViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }

        return TransactionDisplayViewHolder(view)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: TransactionDisplayViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }

        if (!isSearch) {
            if (position == 0) {
                holder.isNew.visibility = View.VISIBLE
            }
        }

        val tr = transactions[position]
        val num = position + 1
        val item = "(${tr.dataTransaction.totalItem} item)"
        val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(tr.date.created))

        holder.tNumber.text = num.toString()
        holder.tID.text = tr.id
        holder.tItem.text = item
        holder.tTotal.text = helper.intToRupiah(tr.dataTransaction.grandTotal)
        holder.tCreate.text = dateTime.date
        holder.payTime.text = dateTime.time
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}