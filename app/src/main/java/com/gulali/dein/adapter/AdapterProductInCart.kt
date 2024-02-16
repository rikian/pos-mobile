package com.gulali.dein.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.gulali.dein.R
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.entities.EntityCart

class AdapterProductInCart(
    private val products: MutableList<EntityCart>,
    private val helper: Helper,
    private val context: Context
): RecyclerView.Adapter<AdapterProductInCart.ProductViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var pName: TextView = view.findViewById(R.id.pname_cart)
        var pPrice: TextView = view.findViewById(R.id.pprice_cart)
        var pQty: TextView = view.findViewById(R.id.pstock_cart)
        val displayDiscount: ConstraintLayout = view.findViewById(R.id.c_discount)
        val displayDiscountPercent: TextView = view.findViewById(R.id.dis_percent)
        var pPriceBeforeDiscount: TextView = view.findViewById(R.id.pp_tot_cart)
        var pPriceAfterDiscount: TextView = view.findViewById(R.id.pp_tot_cart_dis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_cart, parent, false)
        val viewHolder = ProductViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        return products.size
    }

    override fun onBindViewHolder(h: ProductViewHolder, position: Int) {
        h.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }

        val p = products[position]
        h.pName.text = p.product.name
        h.pPrice.text = helper.intToRupiah(p.product.price)
        h.pQty.text = p.product.quantity.toString()

        helper.setPriceAfterDiscount(
            h.pPriceBeforeDiscount,
            h.pPriceAfterDiscount,
            p.product.discountPercent,
            p.product.price,
            p.product.quantity,
            false,
            context
        )

        if (p.product.discountPercent > 0.0) {
            h.displayDiscount.visibility = View.VISIBLE
            h.displayDiscountPercent.visibility = View.VISIBLE
            val disStr = "(${p.product.discountPercent} %)"
            h.displayDiscountPercent.text = disStr
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}