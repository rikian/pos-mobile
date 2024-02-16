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

class AdapterProductDisplay(
    private val products: MutableList<DtoProduct>,
    private val helper: Helper,
    private val context: Context
) : RecyclerView.Adapter<AdapterProductDisplay.ProductViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var pId: TextView = view.findViewById(R.id.id_product)
        var pName: TextView = view.findViewById(R.id.pname_1)
        var pImg: ImageView = view.findViewById(R.id.pimg_1)
        var pStock: TextView = view.findViewById(R.id.pstock_1)
        var stDesc: TextView = view.findViewById(R.id.st_desc)
        var pPrice: TextView = view.findViewById(R.id.pprice_1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterProductDisplay.ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
        val viewHolder = ProductViewHolder(view)
        viewHolder.itemView.setOnClickListener { itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition) }
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }
        val stockStr = "Stock"

        val product = products[position]
        holder.pId.text = product.id.toString()
        holder.pName.text = product.name
        holder.stDesc.text = stockStr
        holder.stDesc.textColors.defaultColor
        holder.pStock.text = product.stock.toString()
        holder.pPrice.text = helper.intToRupiah(product.price)
        val uri = helper.getUriFromGallery(context.contentResolver, product.img)
        if (uri != null) {
            Glide.with(context)
                .load(uri)
                .into(holder.pImg)
        }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}