package com.gulali.dein.helper

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.RecyclerView
import com.gulali.dein.adapter.AdapterProductSearchTransaction
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.repositories.RepositoryCart
import com.gulali.dein.repositories.RepositoryProduct

class SearchProductByName(
    private val transactionID: String,
    private val helper: Helper,
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val repositoryProduct: RepositoryProduct,
    private val repositoryCart: RepositoryCart,
    private val showDialogForInputProductQty: (product: DtoProduct) -> Unit
): TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    @SuppressLint("NotifyDataSetChanged")
    override fun afterTextChanged(s: Editable?) {
        if (s == null) return
        val query = s.toString().trim().lowercase()
        val products = repositoryProduct.getProductByName(query)
        val adapter = initAdapter(products)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun initAdapter(products: MutableList<DtoProduct>): AdapterProductSearchTransaction {
        val adapter = AdapterProductSearchTransaction(helper, context, products)
        adapter.setOnItemClickListener(object : AdapterProductSearchTransaction.OnItemClickListener {
            override fun onItemClick(position: Int) {
                // check stock
                val product = products[position]
                if (product.stock <= 0) {
                    helper.generateTOA(context, "Stock Empty", true)
                    return
                }
                if (repositoryCart.getProductById(transactionID, product.id) != null) {
                    helper.generateTOA(context, "Product already exist in the cart", true)
                    return
                }

                showDialogForInputProductQty(product)
            }
        })

        return adapter
    }
}