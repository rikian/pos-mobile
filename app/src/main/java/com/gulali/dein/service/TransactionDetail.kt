package com.gulali.dein.service

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gulali.dein.adapter.AdapterProductCartDetail
import com.gulali.dein.databinding.TransactionDetailBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.viewmodels.ViewModelTransactionDetail
import com.gulali.dein.repositories.RepositoryOwner
import com.gulali.dein.repositories.RepositoryTransaction
import com.gulali.dein.repositories.RepositoryTransactionItem
import org.koin.android.ext.android.inject

class TransactionDetail : AppCompatActivity() {
    private lateinit var binding: TransactionDetailBinding

    // di
    private val helper: Helper by inject()
    private val viewModelTransactionDetail: ViewModelTransactionDetail by inject()
    private val constant: Constants by inject()
    private val repositoryOwner: RepositoryOwner by inject()
    private val repositoryTransaction: RepositoryTransaction by inject()
    private val repositoryTransactionItem: RepositoryTransactionItem by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransactionDetailBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(it.root)
        }
        if (!getOwner()) return finish()
        viewModelTransactionDetail.idTransaction = intent.getStringExtra(constant.transactionID()) ?: ""
        if (viewModelTransactionDetail.idTransaction == "") return finish()
        viewModelTransactionDetail.transaction = repositoryTransaction.getTransactionByID(viewModelTransactionDetail.idTransaction)
        if (viewModelTransactionDetail.transaction.id == "") return finish()
        viewModelTransactionDetail.transactionItem = repositoryTransactionItem.getTransactionItemById(viewModelTransactionDetail.idTransaction)

        setDataToView()
    }

    private fun getOwner(): Boolean {
        val dataOwner = repositoryOwner.getOwner()
        return if (dataOwner.isNullOrEmpty()) {
            false
        } else {
            viewModelTransactionDetail.owner = dataOwner[0]
            true
        }
    }

    private fun setDataToView() {
        val adapter = AdapterProductCartDetail(
            products = viewModelTransactionDetail.transactionItem,
            helper = helper,
            context = this@TransactionDetail
        )
        adapter.setOnItemClickListener(object : AdapterProductCartDetail.OnItemClickListener {
            override fun onItemClick(position: Int) {}
        })
        binding.cartProduct.adapter = adapter
        // set view
        val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(viewModelTransactionDetail.transaction.date.created))
        binding.dateOrder.text = dateTime.date
        binding.orederId.text = viewModelTransactionDetail.idTransaction
        binding.orderTime.text = dateTime.time

        // set data paid
        binding.pSubTotal.text = helper.intToRupiah(viewModelTransactionDetail.transaction.dataTransaction.subTotalProduct)
        if (viewModelTransactionDetail.transaction.dataTransaction.discountPercent > 0.0) {
            val textDiscPercent = "(${viewModelTransactionDetail.transaction.dataTransaction.discountPercent} %)"
            binding.pDisPercent.text = textDiscPercent
            binding.pDisPercent.visibility = View.VISIBLE
        }
        if (viewModelTransactionDetail.transaction.dataTransaction.discountNominal > 0) {
            val textDiscNominal = "- ${helper.intToRupiah(viewModelTransactionDetail.transaction.dataTransaction.discountNominal)}"
            binding.pDisNominal.text = textDiscNominal
        } else {
            binding.pDisNominal.text = "0"
        }

        // set data tax
        if (viewModelTransactionDetail.transaction.dataTransaction.taxPercent > 0.0) {
            val textTaxPercent = "(${viewModelTransactionDetail.transaction.dataTransaction.taxPercent} %)"
            binding.pTaxPercent.text = textTaxPercent
            binding.pTaxPercent.visibility = View.VISIBLE
        }
        if (viewModelTransactionDetail.transaction.dataTransaction.taxNominal > 0) {
            val textTaxNominal = helper.intToRupiah(viewModelTransactionDetail.transaction.dataTransaction.taxNominal)
            binding.pTaxNominal.text = textTaxNominal
        } else {
            binding.pTaxNominal.text = "0"
        }

        // set adm
        if (viewModelTransactionDetail.transaction.dataTransaction.adm > 0) {
            binding.pAdmNominal.text = helper.intToRupiah(viewModelTransactionDetail.transaction.dataTransaction.adm)
        } else {
            binding.pAdmNominal.text = "0"
        }
        val textGrandTotal = "Rp${helper.intToRupiah(viewModelTransactionDetail.transaction.dataTransaction.grandTotal)}"
        val textCash = "Rp${helper.intToRupiah(viewModelTransactionDetail.transaction.dataTransaction.cash)}"
        val textReturned = "Rp${helper.intToRupiah(viewModelTransactionDetail.transaction.dataTransaction.returned)}"
        binding.pTotalPayment.text = textGrandTotal
        binding.pCash.text = textCash
        binding.pReturned.text = textReturned
    }
}