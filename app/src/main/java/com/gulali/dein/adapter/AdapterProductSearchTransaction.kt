package com.gulali.dein.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gulali.dein.R
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.dto.DtoProduct

class AdapterProductSearchTransaction(
    private val helper: Helper,
    private val ctx: Context,
    private val products: MutableList<DtoProduct>
): RecyclerView.Adapter<AdapterProductSearchTransaction.ProductViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var pId: TextView = view.findViewById(R.id.ps_id)
        var pName: TextView = view.findViewById(R.id.ps_name)
        var pImg: ImageView = view.findViewById(R.id.ps_image)
        var pStock: TextView = view.findViewById(R.id.ps_stock)
        var pPrice: TextView = view.findViewById(R.id.ps_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(ctx).inflate(R.layout.product_search, parent, false)
        val viewHolder = ProductViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }

        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        return products.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }
        val product = products[position]
        holder.pId.text = product.id.toString()
        holder.pName.text = product.name
        holder.pStock.text = product.stock.toString()
        holder.pPrice.text = helper.intToRupiah(product.price)
        val uri = helper.getUriFromGallery(ctx.contentResolver, product.img)
        if (uri != null) {
            Glide.with(ctx)
                .load(uri)
                .into(holder.pImg)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}