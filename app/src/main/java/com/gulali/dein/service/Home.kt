package com.gulali.dein.service

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.gulali.dein.R
import com.gulali.dein.adapter.AdapterProductDisplay
import com.gulali.dein.adapter.AdapterTransactionDisplay
import com.gulali.dein.contracts.intents.ContractProductAdd
import com.gulali.dein.contracts.intents.ContractProductDetail
import com.gulali.dein.contracts.intents.ContractTransactionAdd
import com.gulali.dein.contracts.intents.ContractTransactionDetails
import com.gulali.dein.databinding.HomeBinding
import com.gulali.dein.databinding.ProductDisplayBinding
import com.gulali.dein.databinding.SettingBinding
import com.gulali.dein.databinding.TransactionDisplayBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.viewmodels.VIewModelHome
import com.gulali.dein.repositories.RepositoryOwner
import com.gulali.dein.repositories.RepositoryProduct
import com.gulali.dein.repositories.RepositoryTransaction
import org.koin.android.ext.android.inject

class Home: AppCompatActivity() {
    private lateinit var binding: HomeBinding
    private lateinit var tdBinding: TransactionDisplayBinding
    private lateinit var pdBinding: ProductDisplayBinding
    private lateinit var stBinding: SettingBinding

    // DI
    private val helper: Helper by inject()
    private val constant: Constants by inject()
    private val repositoryProduct: RepositoryProduct by inject()
    private val repositoryTransaction: RepositoryTransaction by inject()
    private val repositoryOwner: RepositoryOwner by inject()
    private val viewModelHome: VIewModelHome by inject()

    // adapter
    private lateinit var adapterProductDisplay: AdapterProductDisplay

    // contract intent
    private var doubleBackToExitPressedOnce = false
    private lateinit var contractProductDetail: ActivityResultLauncher<Int>
    private lateinit var contractProductAdd: ActivityResultLauncher<String>
    private lateinit var contractTransactionAdd: ActivityResultLauncher<String>
    private lateinit var contractTransactionDetail: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomeBinding.inflate(layoutInflater).also {
            setContentView(it.root)
            binding = it
            tdBinding = binding.transactionDisplay
            pdBinding = binding.productsDisplay
            stBinding = binding.settingsDisplay
        }
        initListenerBottomNavigationView()
        initProduct()
        initTransaction()
        initBackPressed()
        initSetting()
    }

    override fun onStart() {
        super.onStart()
        val dataOwner = repositoryOwner.getOwner()
        if (dataOwner.isNullOrEmpty()) {
            helper.launchRegistration(this@Home)
        } else {
            viewModelHome.owner = dataOwner
        }
    }

    private fun initTransaction() {
        initAdapterTransactionDisplay()
        contractTransactionAdd = registerForActivityResult(ContractTransactionAdd(constant)) { result ->
            if (result != null && result.isUpdate) {
                initAdapterTransactionDisplay()
                initAdapterProductDisplay()
            }
        }
        contractTransactionDetail = registerForActivityResult(ContractTransactionDetails(constant)) { isUpdate ->
            if (isUpdate) {

            }
        }

        tdBinding.btnNTransaction.setOnClickListener { contractTransactionAdd.launch(constant.newActivity()) }
    }
    private fun initProduct() {
        initAdapterProductDisplay()
        contractProductAdd = registerForActivityResult(ContractProductAdd(constant)) { result ->
            if (result != 0) {
                initAdapterProductDisplay()
            }
        }
        contractProductDetail = registerForActivityResult(ContractProductDetail(constant)) { result ->
            if (result != null && result.isUpdate) {
                initAdapterProductDisplay()
            }
        }
        pdBinding.btnAddProduct.setOnClickListener{ contractProductAdd.launch(constant.newActivity()) }
    }

    private fun initSetting() {
        stBinding.stEditBuetooth.setOnClickListener {
            Intent(this@Home, BluetoothSetting::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun initListenerBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.transaction -> {
                    stBinding.settingDisplay.visibility = View.GONE
                    pdBinding.productsDisplay.visibility = View.GONE
                    tdBinding.transactionDisplay.visibility = View.VISIBLE
                }
                R.id.products -> {
                    stBinding.settingDisplay.visibility = View.GONE
                    pdBinding.productsDisplay.visibility = View.VISIBLE
                    tdBinding.transactionDisplay.visibility = View.GONE
                }
                R.id.settings -> {
                    stBinding.settingDisplay.visibility = View.VISIBLE
                    pdBinding.productsDisplay.visibility = View.GONE
                    tdBinding.transactionDisplay.visibility = View.GONE
                }
                else -> {
                    return@setOnItemSelectedListener false
                }
            }
            return@setOnItemSelectedListener true
        }
    }
    private fun initAdapterProductDisplay() {
        val products = repositoryProduct.getProducts()
        val adapter = AdapterProductDisplay(products, helper, this@Home)
        pdBinding.productList.adapter = adapter
        adapter.setOnItemClickListener(object : AdapterProductDisplay.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val product = products[position]
                contractProductDetail.launch(product.id)
            }
        })
        pdBinding.nestedProductView.smoothScrollTo(0, 0)
        pdBinding.productList.scrollToPosition(0)
    }
    private fun initAdapterTransactionDisplay() {
        val transactions = repositoryTransaction.getTransaction(0, 10)
        val adapter = AdapterTransactionDisplay(this@Home, helper, false, transactions)
        tdBinding.transactionList.adapter = adapter
        adapter.setOnItemClickListener(object : AdapterTransactionDisplay.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val t = transactions[position]
                contractTransactionDetail.launch(t.id)
            }
        })
        tdBinding.nsvTransaction.smoothScrollTo(0,0)
        tdBinding.transactionList.scrollToPosition(0)
    }
    private fun initBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finish()
                } else {
                    doubleBackToExitPressedOnce = true
                    helper.generateTOA(this@Home, "Press back again to exit", true)
                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000) // Change this value to adjust the time threshold
                }
            }
        }
        onBackPressedDispatcher.addCallback(this@Home, callback)
    }
}