package com.gulali.dein.service

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.gulali.dein.R
import com.gulali.dein.databinding.ProductUpdateStockBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.helper.SetCurrency
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.viewmodels.ViewModelProductStock
import com.gulali.dein.repositories.RepositoryProduct
import org.koin.android.ext.android.inject

class ProductUpdateStock : AppCompatActivity() {
    private lateinit var binding: ProductUpdateStockBinding

    // DI
    private val helper: Helper by inject()
    private val constant: Constants by inject()
    private val repositoryProduct: RepositoryProduct by inject()

    // view model
    private val viewModelProductStock: ViewModelProductStock by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProductUpdateStockBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        viewModelProductStock.productID = intent.getIntExtra(constant.productID(), 0)
        if (viewModelProductStock.productID == 0) return finish()
        viewModelProductStock.dataProduct = repositoryProduct.getProductByID(viewModelProductStock.productID) ?: return finish()
        initDisplay()
    }

    private fun initDisplay() {
        binding.anpQtyTot.setRawInputType(2)
        binding.anpEdtPurchase.setRawInputType(2)
        binding.crStock.text = viewModelProductStock.dataProduct.stock.toString()
        binding.increase.isChecked = true
        helper.initialSockListener(binding.anpQtyMin, binding.anpQtyPlus, binding.anpQtyTot)
        binding.anpQtyTot.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (binding.increase.isChecked) {
                    val textInc = "+ ${binding.anpQtyTot.text}"
                    binding.issetto.text = textInc
                } else {
                    val textDesc = "- ${binding.anpQtyTot.text}"
                    binding.issetto.text = textDesc
                }
                if (p0 != null) {
                    binding.anpQtyTot.setSelection(p0.length)
                }
            }
        })
        binding.anpEdtPurchase.addTextChangedListener(SetCurrency(helper, binding.anpEdtPurchase))
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.increase -> {
                    val textInc = "+ ${binding.anpQtyTot.text}"
                    binding.issetto.text = textInc
                }
                R.id.decrease -> {
                    val textDesc = "- ${binding.anpQtyTot.text}"
                    binding.issetto.text = textDesc
                }
            }
        }

        binding.stockOk.setOnClickListener {
            updateStock()
        }
    }

    private fun updateStock() {
        val stock = helper.strToInt(binding.anpQtyTot.text.toString())
        if (binding.increase.isChecked) {
            viewModelProductStock.entityHistoryStock.inStock = stock
            viewModelProductStock.entityHistoryStock.outStock = 0
            viewModelProductStock.entityHistoryStock.currentStock = viewModelProductStock.dataProduct.stock + stock
        } else {
            if (viewModelProductStock.dataProduct.stock - stock < 0) {
                return helper.generateTOA(this@ProductUpdateStock, "Invalid stock value", true)
            }
            viewModelProductStock.entityHistoryStock.inStock = 0
            viewModelProductStock.entityHistoryStock.outStock = stock
            viewModelProductStock.entityHistoryStock.currentStock = viewModelProductStock.dataProduct.stock - stock
        }
        viewModelProductStock.entityHistoryStock.pID = viewModelProductStock.productID
        viewModelProductStock.entityHistoryStock.transactionID = ""
        viewModelProductStock.entityHistoryStock.purchase = helper.rupiahToInt(binding.anpEdtPurchase.text.toString())
        viewModelProductStock.entityHistoryStock.info = binding.description.text.toString()
        val date = helper.getCurrentDate()
        viewModelProductStock.entityHistoryStock.date.created = date
        viewModelProductStock.entityHistoryStock.date.updated = date

        // update stock in database
        viewModelProductStock.isUpdate = repositoryProduct.updateProductStock(viewModelProductStock.entityHistoryStock)

        if (viewModelProductStock.isUpdate) {
            setResult()
        } else {
            helper.generateTOA(this@ProductUpdateStock, "failed update stock", true)
        }
    }

    private fun setResult() {
        val resultIntent = Intent().apply {
            // Set any data to be returned, if needed
            this.putExtra(constant.productID(), viewModelProductStock.productID)
            this.putExtra(constant.isUpdate(), viewModelProductStock.isUpdate)
        }
        setResult(if (viewModelProductStock.isUpdate) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
        finish()
    }
}