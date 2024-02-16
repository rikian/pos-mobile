package com.gulali.dein.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gulali.dein.R

class AdapterStringItem(private val context: Context, private val items: List<String>): RecyclerView.Adapter<AdapterStringItem.ViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var pDevice: TextView = itemView.findViewById(R.id.device_paired)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.string_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int { return items.size }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }

        holder.pDevice.text = items[position]
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}