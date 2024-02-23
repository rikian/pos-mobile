package com.gulali.dein.service

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
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
import com.gulali.dein.helper.SetCurrency
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.DtoTransactionWithCount
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.viewmodels.VIewModelHome
import com.gulali.dein.repositories.RepositoryOwner
import com.gulali.dein.repositories.RepositoryProduct
import com.gulali.dein.repositories.RepositoryTransaction
import org.koin.android.ext.android.inject
import java.util.Calendar
import kotlin.math.ceil

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

    private lateinit var viewModelHome: VIewModelHome

    // contract intent
    private var doubleBackToExitPressedOnce = false
    private lateinit var contractProductDetail: ActivityResultLauncher<Int>
    private lateinit var contractProductAdd: ActivityResultLauncher<String>
    private lateinit var contractTransactionAdd: ActivityResultLauncher<String>
    private lateinit var contractTransactionDetail: ActivityResultLauncher<String>

    // scanner feature
    private lateinit var barcodeScanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null

    // alert dialog
    private var transactionDialogFilter: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomeBinding.inflate(layoutInflater).also {
            setContentView(it.root)
            viewModelHome = VIewModelHome()
            binding = it
            tdBinding = binding.transactionDisplay
            pdBinding = binding.productsDisplay
            stBinding = binding.settingsDisplay

            barcodeScanner = helper.initBarcodeScanner(this@Home)
            mediaPlayer = helper.initBeebSound(this@Home)

            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, view)
                val isImeVisible = insetsCompat.isVisible(WindowInsetsCompat.Type.ime())
                // below line, do the necessary stuff:
                binding.bottomNavigationView.visibility = if (isImeVisible) View.GONE else View.VISIBLE
                view.onApplyWindowInsets(insets)
            }

            val moduleInstallRequest = ModuleInstallRequest.newBuilder()
                .addApi(barcodeScanner) //Add the scanner client to the module install request
                .build()
            val moduleInstallClient = ModuleInstall.getClient(this)
            moduleInstallClient.installModules(moduleInstallRequest).addOnFailureListener { ex ->
                helper.generateTOA(this, ex.message.toString(), true)
            }
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    private fun initTransaction() {
        initFilterTransaction()
        contractTransactionAdd = registerForActivityResult(ContractTransactionAdd(constant)) { result ->
            if (result != null && result.isUpdate) {
                refreshTransaction()
                initAdapterProductDisplay()
            }
        }
        contractTransactionDetail = registerForActivityResult(ContractTransactionDetails(constant)) { isUpdate ->
            if (isUpdate) {
                return@registerForActivityResult
            }
        }
        tdBinding.btnNTransaction.setOnClickListener { contractTransactionAdd.launch(constant.newActivity()) }
        tdBinding.refreshTransaction.setOnRefreshListener {
            refreshTransaction()
            tdBinding.refreshTransaction.isRefreshing = false
        }

        // filter
        tdBinding.filterTransaction.setOnClickListener { transactionDialogFilter?.show() }
        tdBinding.pgNextTr.setOnClickListener {
            if (viewModelHome.transactionFilter.index + 1 >= viewModelHome.transactionTotalPage) return@setOnClickListener
            viewModelHome.transactionFilter.index += 1
            displayNewTransaction(false)
            helper.hideKeyboard(it, this@Home)
        }
        tdBinding.pgPrevTr.setOnClickListener {
            if (viewModelHome.transactionFilter.index + 1 < 0) return@setOnClickListener
            viewModelHome.transactionFilter.index -= 1
            displayNewTransaction(false)
            helper.hideKeyboard(it, this@Home)
        }
        tdBinding.trBtnJumpTo.setOnClickListener {
            val target = helper.strToInt(tdBinding.trJumpTo.text.toString())
            if (target <= 0 || target > viewModelHome.transactionTotalPage || target == viewModelHome.transactionFilter.index + 1) return@setOnClickListener helper.generateTOA(this@Home, "Invalid page", true)
            viewModelHome.transactionFilter.index = target - 1
            displayNewTransaction(false)
            helper.hideKeyboard(it, this@Home)
        }

        refreshTransaction()
    }

    private fun refreshTransaction() {
        viewModelHome.isFromTransactionFilter = false
        viewModelHome.transactionFilter.index = 0
        displayNewTransaction(true)
    }

    private fun initFilterTransaction() {
        val dialogLayout = layoutInflater.inflate(R.layout.transaction_filter,null)
        val totalStart: EditText = dialogLayout.findViewById(R.id.f_tot_start)
        val totalEnd: EditText = dialogLayout.findViewById(R.id.f_tot_end)
        val dateStart = dialogLayout.findViewById<EditText>(R.id.f_date_start)
        val dateEnd = dialogLayout.findViewById<EditText>(R.id.f_date_end)
        val btnApply: Button = dialogLayout.findViewById(R.id.btn_ftr_appy)
        val btnCancel: Button = dialogLayout.findViewById(R.id.btn_ftr_cancel)
        val btnReset: Button = dialogLayout.findViewById(R.id.btn_ftr_reset)
        totalStart.setRawInputType(2)
        totalEnd.setRawInputType(2)
        // validation filter
        if (viewModelHome.transactionFilter.totalStart > 0) {
            totalStart.setText(helper.intToRupiah(viewModelHome.transactionFilter.totalStart))
        }
        if (viewModelHome.transactionFilter.totalEnd > 0) {
            if (viewModelHome.transactionFilter.totalEnd != 999999999) {
                totalEnd.setText(helper.intToRupiah(viewModelHome.transactionFilter.totalEnd))
            }
        }
        totalStart.addTextChangedListener(SetCurrency(helper, totalStart))
        totalEnd.addTextChangedListener(SetCurrency(helper, totalEnd))
        dateStart.setOnClickListener{filterDate(dateStart)}
        dateEnd.setOnClickListener{filterDate(dateEnd)}
        btnApply.setOnClickListener apply@{
            val totalEndInt = helper.rupiahToInt(totalEnd.text.toString())
            val dateEndLong = helper.parseToEndDateUnix(dateEnd.text.toString())

            viewModelHome.transactionFilter.totalStart = helper.rupiahToInt(totalStart.text.toString())
            viewModelHome.transactionFilter.totalEnd = if (totalEndInt <= 0) 999999999 else totalEndInt
            viewModelHome.transactionFilter.dateStart = helper.parseDateStrToUnix(dateStart.text.toString())
            viewModelHome.transactionFilter.dateEnd = if (dateEndLong <= 0L) helper.getCurrentEndDate() else dateEndLong

            // validation
            if (viewModelHome.transactionFilter.totalStart == 0 && viewModelHome.transactionFilter.totalEnd == 0 && viewModelHome.transactionFilter.dateStart == 0L && viewModelHome.transactionFilter.dateStart == 0L) {
                helper.generateTOA(this, "Invalid value", true)
                return@apply
            }

            if (viewModelHome.transactionFilter.totalStart != 0 && viewModelHome.transactionFilter.totalEnd != 0) {
                if (viewModelHome.transactionFilter.totalStart >= viewModelHome.transactionFilter.totalEnd) {
                    helper.generateTOA(this@Home, "Total start must be less than to total end", true)
                    return@apply
                }
            }

            if (viewModelHome.transactionFilter.dateStart != 0L && viewModelHome.transactionFilter.dateEnd != 0L) {
                if (viewModelHome.transactionFilter.dateStart >= viewModelHome.transactionFilter.dateEnd) {
                    helper.generateTOA(this@Home, "Date start must be less than to date end", true)
                    return@apply
                }
            }
            viewModelHome.transactionFilter.index = 0
            viewModelHome.isFromTransactionFilter = true
            displayNewTransaction(false)
            helper.hideKeyboard(it, this@Home)
            closeDialogFilterTransaction()
        }
        btnCancel.setOnClickListener {closeDialogFilterTransaction()}
        btnReset.setOnClickListener {
            refreshTransaction()
            helper.hideKeyboard(it, this@Home)
            closeDialogFilterTransaction()
        }
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogLayout)
        transactionDialogFilter = builder.create()
        transactionDialogFilter?.setCanceledOnTouchOutside(false)
        transactionDialogFilter?.setOnCancelListener{return@setOnCancelListener}
    }

    private fun closeDialogFilterTransaction() {
        transactionDialogFilter?.dismiss()
    }

    private fun displayNewTransaction(needNewLabel: Boolean) {
        val adapter: AdapterTransactionDisplay
        val transactions: DtoTransactionWithCount = if (!viewModelHome.isFromTransactionFilter) {
            repositoryTransaction.getDataTransactionWithCountAndSum(
                index = viewModelHome.transactionFilter.index,
                limit = viewModelHome.transactionFilter.limit
            )
        } else {
            repositoryTransaction.getDataTransactionFilterWithCountAndSum(viewModelHome.transactionFilter)
        }

        viewModelHome.transactionTotalPage = getTotalPage(transactions.count.count, viewModelHome.transactionFilter.limit)
        setValueTransaction(viewModelHome.transactionFilter.index, viewModelHome.transactionTotalPage)

        adapter = AdapterTransactionDisplay(this@Home, helper, needNewLabel, transactions.list)
        initAdapterTransactionDisplay(adapter, transactions.list)

        // set total transaction
        tdBinding.totalTransaction1.text = transactions.count.count.toString()
        tdBinding.totalTransaction2.text = transactions.count.count.toString()
    }

    private fun filterDate(e: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this@Home,
            { _: DatePicker, pYear: Int, pMonth: Int, dayOfMonth: Int ->
                // Update the EditText with the selected date
                val selectedDate = "$dayOfMonth/${pMonth + 1}/$pYear"
                e.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun setValueTransaction(currentPage: Int, totalPage: Int) {
        val currentPageStr = if (currentPage <= 0) "1" else "${currentPage + 1}"
        val totalPageStr = totalPage.toString()
        tdBinding.t65Up1.text = currentPageStr
        tdBinding.t65Up2.text = totalPageStr
        tdBinding.t65Down1.text = currentPageStr
        tdBinding.t65Down2.text = totalPageStr

        // set default
        tdBinding.cpDownContainer.visibility = View.GONE

        // check if total page greater than 1
        if (totalPage > 1) {
            tdBinding.cpDownContainer.visibility = View.VISIBLE
        }
    }

    private fun getTotalPage(totalData: Int, pageSize: Int): Int {
        return ceil(totalData.toDouble() / pageSize).toInt()
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
        stBinding.btnPersonalInfo.setOnClickListener {
            if (stBinding.cPersonalInfo.visibility == View.GONE) {
                stBinding.cPersonalInfo.visibility = View.VISIBLE
                stBinding.arrowRight.rotation += 90F
            } else {
                stBinding.cPersonalInfo.visibility = View.GONE
                stBinding.arrowRight.rotation -= 90F
            }
        }
        stBinding.btnPayment.setOnClickListener {
            if (stBinding.bodyPaySet.visibility == View.GONE) {
                stBinding.bodyPaySet.visibility = View.VISIBLE
                stBinding.arrowRight2.rotation += 90F
            } else {
                stBinding.bodyPaySet.visibility = View.GONE
                stBinding.arrowRight2.rotation -= 90F
            }
        }
        stBinding.btnBluetooth.setOnClickListener {
            if (stBinding.cBluetooth.visibility == View.GONE) {
                stBinding.cBluetooth.visibility = View.VISIBLE
                stBinding.arrowRight3.rotation += 90F
            } else {
                stBinding.cBluetooth.visibility = View.GONE
                stBinding.arrowRight3.rotation -= 90F
            }
        }
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
    private fun initAdapterTransactionDisplay(adapter: AdapterTransactionDisplay, transactions: MutableList<EntityTransaction>) {
        tdBinding.shimmerLayout.startShimmer()
        tdBinding.shimmerLayout.visibility = View.VISIBLE
        tdBinding.mainTransaction.visibility = View.GONE
        tdBinding.nsvTransaction.smoothScrollTo(0,0)
        tdBinding.transactionList.scrollToPosition(0)
        tdBinding.transactionList.adapter = adapter
        adapter.setOnItemClickListener(object : AdapterTransactionDisplay.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val t = transactions[position]
                contractTransactionDetail.launch(t.id)
            }
        })
        tdBinding.transactionList.postDelayed({
            // Stop the shimmer effect after the data is updated
            tdBinding.shimmerLayout.stopShimmer()
            tdBinding.shimmerLayout.visibility = View.GONE
            tdBinding.mainTransaction.visibility = View.VISIBLE
        }, 500)
    }
    private fun initBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (stBinding.settingDisplay.visibility == View.VISIBLE) {
                    binding.bottomNavigationView.selectedItemId = R.id.products
                    return
                }
                if (pdBinding.productsDisplay.visibility == View.VISIBLE) {
                    binding.bottomNavigationView.selectedItemId = R.id.transaction
                    return
                }
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