package com.gulali.dein.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.gulali.dein.contracts.intents.ContractProductStock
import com.gulali.dein.contracts.intents.ContractProductUpdate
import com.gulali.dein.databinding.ProductDetailBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.contract.DtoContractProductDetail
import com.gulali.dein.models.viewmodels.ViewModelProductDetail
import com.gulali.dein.repositories.RepositoryProduct
import com.gulali.dein.repositories.RepositoryTransactionItem
import org.koin.android.ext.android.inject

class ProductDetail : AppCompatActivity() {
    private lateinit var binding: ProductDetailBinding

    // DI
    private val helper: Helper by inject()
    private val constant: Constants by inject()
    private val repositoryProduct: RepositoryProduct by inject()
    private val repositoryTransactionItem: RepositoryTransactionItem by inject()

    // contract
    private lateinit var contractProductUpdate: ActivityResultLauncher<Int>
    private lateinit var contractProductStock: ActivityResultLauncher<Int>

    // view-model
    private val viewModelProductDetail: ViewModelProductDetail by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProductDetailBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        viewModelProductDetail.productID = intent.getIntExtra(constant.productID(), 0)
        if (viewModelProductDetail.productID == 0) {
            finish()
            return
        }

        viewModelProductDetail.soldOut = repositoryTransactionItem.getSoldOut(viewModelProductDetail.productID)

        contractProductUpdate = registerForActivityResult(ContractProductUpdate(constant)) { result -> handlerResultIntent(result) }
        contractProductStock = registerForActivityResult(ContractProductStock(constant)) { result -> handlerResultIntent(result) }

        binding.viewUpd.setOnClickListener{ contractProductUpdate.launch(viewModelProductDetail.productID) }
        binding.viewAddStock.setOnClickListener { contractProductStock.launch(viewModelProductDetail.productID) }
        binding.btnHstory.setOnClickListener {
            Intent(this@ProductDetail, StockHistory::class.java).also {
                it.putExtra(constant.productID(), viewModelProductDetail.productID)
                startActivity(it)
            }
        }
        binding.btnDescrp.setOnClickListener {
            if (binding.detailInfo.visibility == View.GONE) {
                binding.detailInfo.visibility = View.VISIBLE
            } else {
                binding.detailInfo.visibility = View.GONE
            }
        }

        passingDataToView()
        initBackPressed()
    }

    private fun handlerResultIntent(result: DtoContractProductDetail?) {
        if (result != null) {
            if (result.isUpdate) {
                viewModelProductDetail.isUpdate = true
                passingDataToView()
            }
        }
    }

    private fun passingDataToView() {
        val product = repositoryProduct.getProductByID(viewModelProductDetail.productID)
        if (product == null) {
            setResult()
        } else {
            viewModelProductDetail.dataProduct = product
            // passing data to view
            binding.pvName.text = helper.capitaliseEachWord(viewModelProductDetail.dataProduct.name)
            binding.pvPrice.text = helper.intToRupiah(viewModelProductDetail.dataProduct.price)
            binding.pvStock.text = viewModelProductDetail.dataProduct.stock.toString()
            binding.tmplSoldOut.text = viewModelProductDetail.soldOut.toString()
            binding.detailInfoMsg.text = viewModelProductDetail.dataProduct.info

            val uri = helper.getUriFromGallery(contentResolver, viewModelProductDetail.dataProduct.img)
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .into(binding.imageView2)
            }
        }
    }

    private fun setResult() {
        val resultIntent = Intent().apply {
            // Set any data to be returned, if needed
            this.putExtra(constant.productID(), viewModelProductDetail.productID)
            this.putExtra(constant.isUpdate(), viewModelProductDetail.isUpdate)
        }
        setResult(if (viewModelProductDetail.isUpdate) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun initBackPressed() {
        val callback = object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { setResult() } }
        onBackPressedDispatcher.addCallback(this@ProductDetail, callback)
    }
}