package com.gulali.dein.service

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.gulali.dein.R
import com.gulali.dein.adapter.AdapterProductInCart
import com.gulali.dein.config.Permission
import com.gulali.dein.contracts.bluetooth.BluetoothStateListener
import com.gulali.dein.contracts.intents.ContractBluetoothUpdate
import com.gulali.dein.databinding.PaymentDisplayBinding
import com.gulali.dein.databinding.TransactionAddBinding
import com.gulali.dein.databinding.TransactionSuccessBinding
import com.gulali.dein.helper.BluetoothReceiver
import com.gulali.dein.helper.Helper
import com.gulali.dein.helper.Printer
import com.gulali.dein.helper.SearchProductByName
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.dto.DtoPercentNominal
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.dto.DtoTransactionItem
import com.gulali.dein.models.entities.EntityCart
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.entities.EntityTransactionItem
import com.gulali.dein.models.viewmodels.ViewModelTransaction
import com.gulali.dein.repositories.RepositoryCart
import com.gulali.dein.repositories.RepositoryOwner
import com.gulali.dein.repositories.RepositoryProduct
import com.gulali.dein.repositories.RepositoryTransaction
import org.koin.android.ext.android.inject
import java.util.UUID

class TransactionAdd : AppCompatActivity(), BluetoothStateListener {
    private lateinit var binding: TransactionAddBinding
    private lateinit var paymentDisplay: PaymentDisplayBinding
    private lateinit var transactionSuccess: TransactionSuccessBinding

    // DI
    private val permission: Permission by inject()
    private val helper: Helper by inject()
    private val constant: Constants by inject()
    private val repositoryTransaction: RepositoryTransaction by inject()
    private val repositoryProduct: RepositoryProduct by inject()
    private val repositoryCart: RepositoryCart by inject()
    private val repositoryOwner: RepositoryOwner by inject()

    // view model
    private val viewModelTransaction: ViewModelTransaction by inject()

    // alert dialog
    private var alertDialogForExit: AlertDialog? = null
    private var alertDialogForExitSuccess: AlertDialog? = null

    // state
    private var isPaymentSuccess = false

    // bluetooth for print
    private lateinit var requestBluetoothPermission: ActivityResultLauncher<String>
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var printer: Printer
    private var bluetoothReceiver: BluetoothReceiver? = null
    private lateinit var bluetoothFilter: IntentFilter
    private var alertDialogForShowTurnOnBluetooth: AlertDialog? = null

    // contract
    private lateinit var contractBluetoothSetting: ActivityResultLauncher<String>

    // barcode scanning
    private lateinit var scanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransactionAddBinding.inflate(layoutInflater).also {
            binding = it
            paymentDisplay = it.paymentDisplay
            transactionSuccess = it.transactionSuccess
            setContentView(binding.root)

            // clear cart
            repositoryCart.truncateDaoCart()
            viewModelTransaction.idTransaction = helper.generateTransactionID()
        }

        binding.searchProduct.addTextChangedListener(
            SearchProductByName(
                transactionID = viewModelTransaction.idTransaction,
                helper = helper,
                context = this@TransactionAdd,
                recyclerView = binding.productSearchView,
                repositoryProduct = repositoryProduct,
                repositoryCart = repositoryCart,
                showDialogForInputProductQty = ::dialogForInputProductQty
            )
        )

        binding.payout.setOnClickListener {
            // get status cart in database
            val statusCart = repositoryCart.getTotalPriceAndItem(viewModelTransaction.idTransaction)
            if (statusCart == null) {
                helper.generateTOA(this@TransactionAdd, "Something wrong with cart", true)
                return@setOnClickListener
            }
            if (statusCart.itemCount <= 0) {
                helper.generateTOA(this@TransactionAdd, "Please insert product before payout", true)
                return@setOnClickListener
            }
            viewModelTransaction.dtoTransaction.subTotalProduct = statusCart.totalAfterDiscount
            viewModelTransaction.dtoTransaction.totalItem = statusCart.itemCount

            // set item and total payment
            val totItemStr = "(${statusCart.itemCount} item)"
            paymentDisplay.supTotItem.text = totItemStr
            paymentDisplay.pSubTotal.text = helper.intToRupiah(viewModelTransaction.dtoTransaction.subTotalProduct)

            // open payment display
            binding.bodyTransactionAdd.visibility = View.GONE
            binding.paymentDisplay.bodyPayment.visibility = View.VISIBLE

            setValueForTotal()
        }

        requestBluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                printTransaction()
            } else {
                helper.generateTOA(this, "GPOS cannot print stuck because one of permission not allowed.\n\nYou can get instruction for more details in settings menu", true)
            }
        }

        contractBluetoothSetting = registerForActivityResult(ContractBluetoothUpdate(constant)) { isUpdate ->
            if (isUpdate) {
                if (!getOwner()) {
                    helper.generateTOA(this@TransactionAdd, "Failed get data owner", true)
                }
                printTransaction()
            } else {
                helper.generateTOA(this@TransactionAdd, "Printer not found", true)
            }
        }

        mediaPlayer = helper.initBeebSound(this@TransactionAdd)
        scanner = helper.initBarcodeScanner(this@TransactionAdd)

        binding.btnScanBc.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { result ->
                    // add bib sound
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                    }
                    val barcodeText = result.rawValue
                    val barcode = barcodeText ?: ""
                    val products = repositoryProduct.getProductByName(barcode)
                    if (products.isEmpty()) {
                        helper.generateTOA(this@TransactionAdd, "Product not found", true)
                    } else {
                        val product = products[0]
                        if (product.stock <= 0) return@addOnSuccessListener helper.generateTOA(this@TransactionAdd, "stock empty for ${product.name}", true)
                        if (repositoryCart.getProductById(viewModelTransaction.idTransaction, product.id) != null) {
                            helper.generateTOA(this@TransactionAdd, "Product already exist in the cart", true)
                            return@addOnSuccessListener
                        }
                        dialogForInputProductQty(product)
                    }
                }
                .addOnCanceledListener {
                    // Task canceled
                }
                .addOnFailureListener { err ->
                    helper.generateTOA(this, err.message.toString(), true)
                }
        }

        initPaymentDisplay()
        initProcessTransaction()
        initDialogExit(true)
        initDialogExit(false)
        initBackPressed()
    }

    override fun onStart() {
        super.onStart()
        if (!getOwner()) {
            helper.launchRegistration(this@TransactionAdd)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver)
        }

        mediaPlayer?.release()
    }

    private fun getOwner(): Boolean {
        val dataOwner = repositoryOwner.getOwner()
        return if (dataOwner.isNullOrEmpty()) {
            false
        } else {
            viewModelTransaction.owner = dataOwner[0]
            true
        }
    }

    private fun dialogForInputProductQty(product: DtoProduct) {
        // init entity item product
        val entityCart = EntityCart(
            transactionID = viewModelTransaction.idTransaction,
            img = product.img,
            stock = product.stock,
            product = DtoTransactionItem(
                productID= product.id,
                name= product.name,
                quantity= 1,
                price= product.price,
                discountPercent= 0.0,
                discountNominal= 0,
                totalBeforeDiscount= 0,
                totalAfterDiscount= 0,
            ),
            createdAt = helper.getCurrentDate(),
            unit = product.unit
        )

        // Reuse the builder and inflater
        val builder = AlertDialog.Builder(this@TransactionAdd)
        val dialogLayout = layoutInflater.inflate(R.layout.product_qty, null)
        val alertDialog = builder.setView(dialogLayout).show()

        // Get references to views
        val displayImage = dialogLayout.findViewById<ImageView>(R.id.product_image)
        val displayName = dialogLayout.findViewById<TextView>(R.id.product_name)
        val displayStock = dialogLayout.findViewById<TextView>(R.id.product_stock)
        val displayUnit = dialogLayout.findViewById<TextView>(R.id.product_unit)
        val displayPrice = dialogLayout.findViewById<TextView>(R.id.product_price)
        val priceBeforeDiscount = dialogLayout.findViewById<TextView>(R.id.anp_qty_totpr)
        val priceAfterDiscount = dialogLayout.findViewById<TextView>(R.id.tot_af_dis)
        val pdOk = dialogLayout.findViewById<Button>(R.id.btn_qty_ok)
        val pdCancel = dialogLayout.findViewById<Button>(R.id.btn_qty_cancel)

        // Get references input to views
        val pdQtyMin = dialogLayout.findViewById<Button>(R.id.anp_qty_min)
        val pdQtyPlus = dialogLayout.findViewById<Button>(R.id.anp_qty_plus)
        val pdQty = dialogLayout.findViewById<EditText>(R.id.anp_qty_tot)
        val pdDiscount = dialogLayout.findViewById<EditText>(R.id.anp_discount)

        // Set product data to views
        val uri = helper.getUriFromGallery(contentResolver, product.img)
        if (uri != null) {
            Glide.with(this@TransactionAdd)
                .load(uri)
                .into(displayImage)
        }
        displayName.text = product.name
        displayStock.text = product.stock.toString()
        displayUnit.text = product.unit
        displayPrice.text = helper.intToRupiah(product.price)
        helper.setPriceAfterDiscount(
            targetBeforeDiscount = priceBeforeDiscount,
            targetAfterDiscount =  priceAfterDiscount,
            discount = entityCart.product.discountPercent,
            price = entityCart.product.price,
            qty = entityCart.product.quantity,
            needRp = true,
            ctx = this@TransactionAdd
        )

        // Set click listener
        helper.initialSockListener(pdQtyMin, pdQtyPlus, pdQty)
        pdQty.addTextChangedListener(object : TextWatcher {
            var isFinish = false
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(inpt: Editable?) {
                if (isFinish) {
                    isFinish = false
                    pdQty.setSelection(pdQty.text.length)
                    return
                }
                if (inpt == null) return setPrice(false)
                val value = inpt.toString().toIntOrNull() ?: return setPrice(false)
                if (value <= 0) return setPrice(false)
                if (value > product.stock) {
                    entityCart.product.quantity = product.stock
                    setPrice(true)
                } else {
                    entityCart.product.quantity = value
                    setPrice(true)
                }
            }

            private fun setPrice(isValid: Boolean) {
                isFinish = true
                if (!isValid) {
                    entityCart.product.quantity = 1
                }
                helper.setPriceAfterDiscount(
                    targetBeforeDiscount = priceBeforeDiscount,
                    targetAfterDiscount =  priceAfterDiscount,
                    discount = entityCart.product.discountPercent,
                    price = entityCart.product.price,
                    qty = entityCart.product.quantity,
                    needRp = true,
                    ctx = this@TransactionAdd
                )
                pdQty.setText(entityCart.product.quantity.toString())
            }
        })
        pdDiscount.addTextChangedListener(object : TextWatcher {
            var isFinish = false
            var cp = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = pdDiscount.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                entityCart.product.discountPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    pdDiscount.setSelection(cp)
                    helper.setPriceAfterDiscount(
                        targetBeforeDiscount = priceBeforeDiscount,
                        targetAfterDiscount =  priceAfterDiscount,
                        discount = entityCart.product.discountPercent,
                        price = entityCart.product.price,
                        qty = entityCart.product.quantity,
                        needRp = true,
                        ctx = this@TransactionAdd
                    )
                    return
                }
                isFinish = true
                pdDiscount.setText(s.toString())
            }
        })
        pdOk.setOnClickListener {
            // set total after and before discount
            entityCart.product.discountNominal = helper.getDiscountNominal(entityCart.product.discountPercent, entityCart.product.price, entityCart.product.quantity)
            entityCart.product.totalBeforeDiscount = entityCart.product.price * entityCart.product.quantity
            entityCart.product.totalAfterDiscount = entityCart.product.totalBeforeDiscount - entityCart.product.discountNominal

            val resultSaveProductInCart = repositoryCart.pushItem(entityCart)
            if (resultSaveProductInCart.toInt() == 0) {
                helper.generateTOA(this@TransactionAdd, "Failed insert product to cart", true)
                return@setOnClickListener
            }
            val statusCart = repositoryCart.getTotalPriceAndItem(viewModelTransaction.idTransaction)
            if (statusCart == null) {
                helper.generateTOA(this@TransactionAdd, "Something wrong with cart", true)
            } else {
                val totItemValue = "(${statusCart.itemCount} item)"
                binding.totitem.text = totItemValue
                binding.totpricecart.text = helper.intToRupiah(statusCart.totalAfterDiscount)
                displayProductInCart()
                alertDialog.dismiss()
            }
        }

        pdCancel.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun displayProductInCart() {
        val products = repositoryCart.getProducts(viewModelTransaction.idTransaction)
        val adapter = AdapterProductInCart(products, helper, this@TransactionAdd)
        adapter.setOnItemClickListener(object : AdapterProductInCart.OnItemClickListener {
            override fun onItemClick(position: Int) {
                updateItemInCart(products[position])
            }
        })
        binding.cartProduct.adapter = adapter
    }

    private fun displayProductTransaction() {
        val products = repositoryCart.getProducts(viewModelTransaction.idTransaction)
        val adapter = AdapterProductInCart(products, helper, this@TransactionAdd)
        transactionSuccess.cartProduct.adapter = adapter
        paymentDisplay.bodyPayment.visibility = View.GONE
        transactionSuccess.main.visibility = View.VISIBLE

        // display detail transaction
        transactionSuccess.pSubTotal.text = helper.intToRupiah(viewModelTransaction.dtoTransaction.subTotalProduct)
        if (viewModelTransaction.dtoTransaction.discountPercent != 0.0) {
            transactionSuccess.pDisPercent.visibility = View.VISIBLE
            val disPercentStr = "(${viewModelTransaction.dtoTransaction.discountPercent} %)"
            transactionSuccess.pDisPercent.text = disPercentStr
        } else {
            transactionSuccess.pDisPercent.visibility = View.GONE
        }
        if (viewModelTransaction.dtoTransaction.discountNominal != 0) {
            val disNomStr = "- ${helper.intToRupiah(viewModelTransaction.dtoTransaction.discountNominal)}"
            transactionSuccess.pDisNominal.text = disNomStr
        } else {
            transactionSuccess.pDisNominal.text = "0"
        }
        if (viewModelTransaction.dtoTransaction.taxPercent != 0.0) {
            val taxPerStr = "(${viewModelTransaction.dtoTransaction.taxPercent} %)"
            transactionSuccess.pTaxPercent.text = taxPerStr
            transactionSuccess.pTaxPercent.visibility = View.VISIBLE
        } else {
            transactionSuccess.pTaxPercent.visibility = View.GONE
        }
        if (viewModelTransaction.dtoTransaction.taxNominal != 0) {
            transactionSuccess.pTaxNominal.text = helper.intToRupiah(viewModelTransaction.dtoTransaction.taxNominal)
        } else {
            transactionSuccess.pTaxNominal.text = "0"
        }
        if (viewModelTransaction.dtoTransaction.adm != 0) {
            transactionSuccess.pAdmNominal.text = helper.intToRupiah(viewModelTransaction.dtoTransaction.adm)
        } else {
            transactionSuccess.pAdmNominal.text = "0"
        }

        val totPayStr = "Rp ${helper.intToRupiah(viewModelTransaction.dtoTransaction.grandTotal)}"
        transactionSuccess.pTotalPayment.text = totPayStr

        val cashStr = "Rp ${helper.intToRupiah(viewModelTransaction.dtoTransaction.cash)}"
        transactionSuccess.pCash.text = cashStr

        val returnedStr = "Rp ${helper.intToRupiah(viewModelTransaction.dtoTransaction.cash - viewModelTransaction.dtoTransaction.grandTotal)}"
        transactionSuccess.pReturned.text = returnedStr
    }

    private fun updateItemInCart(entityCart: EntityCart) {
        // Reuse the builder and inflater
        val builder = AlertDialog.Builder(this@TransactionAdd)
        val dialogLayout = layoutInflater.inflate(R.layout.product_qty, null)
        val alertDialog = builder.setView(dialogLayout).show()

        // Get references to views
        val displayImage = dialogLayout.findViewById<ImageView>(R.id.product_image)
        val displayName = dialogLayout.findViewById<TextView>(R.id.product_name)
        val displayStock = dialogLayout.findViewById<TextView>(R.id.product_stock)
        val displayUnit = dialogLayout.findViewById<TextView>(R.id.product_unit)
        val displayPrice = dialogLayout.findViewById<TextView>(R.id.product_price)
        val priceBeforeDiscount = dialogLayout.findViewById<TextView>(R.id.anp_qty_totpr)
        val priceAfterDiscount = dialogLayout.findViewById<TextView>(R.id.tot_af_dis)
        val pdOk = dialogLayout.findViewById<Button>(R.id.btn_qty_ok)
        val updateStr = "Update"
        pdOk.text = updateStr
        val pdCancel = dialogLayout.findViewById<Button>(R.id.btn_qty_cancel)
        pdCancel.visibility = View.GONE
        val pdDelete = dialogLayout.findViewById<Button>(R.id.btn_qty_del)
        pdDelete.visibility = View.VISIBLE

        // Get references input to views
        val pdQtyMin = dialogLayout.findViewById<Button>(R.id.anp_qty_min)
        val pdQtyPlus = dialogLayout.findViewById<Button>(R.id.anp_qty_plus)
        val pdQty = dialogLayout.findViewById<EditText>(R.id.anp_qty_tot)
        val pdDiscount = dialogLayout.findViewById<EditText>(R.id.anp_discount)

        // Set click listener
        helper.initialSockListener(pdQtyMin, pdQtyPlus, pdQty)
        pdQty.addTextChangedListener(object : TextWatcher {
            var isFinish = false
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(inpt: Editable?) {
                if (isFinish) {
                    isFinish = false
                    pdQty.setSelection(pdQty.text.length)
                    return
                }
                if (inpt == null) return setPrice(false)
                val value = inpt.toString().toIntOrNull() ?: return setPrice(false)
                if (value <= 0) return setPrice(false)
                if (value > entityCart.stock) {
                    entityCart.product.quantity = entityCart.stock
                    setPrice(true)
                } else {
                    entityCart.product.quantity = value
                    setPrice(true)
                }
            }

            private fun setPrice(isValid: Boolean) {
                isFinish = true
                if (!isValid) {
                    entityCart.product.quantity = 1
                }
                helper.setPriceAfterDiscount(
                    targetBeforeDiscount = priceBeforeDiscount,
                    targetAfterDiscount =  priceAfterDiscount,
                    discount = entityCart.product.discountPercent,
                    price = entityCart.product.price,
                    qty = entityCart.product.quantity,
                    needRp = true,
                    ctx = this@TransactionAdd
                )
                pdQty.setText(entityCart.product.quantity.toString())
            }
        })
        pdDiscount.addTextChangedListener(object : TextWatcher {
            var isFinish = false
            var cp = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = pdDiscount.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                entityCart.product.discountPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    pdDiscount.setSelection(cp)
                    helper.setPriceAfterDiscount(
                        targetBeforeDiscount = priceBeforeDiscount,
                        targetAfterDiscount =  priceAfterDiscount,
                        discount = entityCart.product.discountPercent,
                        price = entityCart.product.price,
                        qty = entityCart.product.quantity,
                        needRp = true,
                        ctx = this@TransactionAdd
                    )
                    return
                }
                isFinish = true
                pdDiscount.setText(s.toString())
            }
        })

        // Set product data to views
        val uri = helper.getUriFromGallery(contentResolver, entityCart.img)
        if (uri != null) {
            Glide.with(this@TransactionAdd)
                .load(uri)
                .into(displayImage)
        }
        displayName.text = entityCart.product.name
        displayStock.text = entityCart.stock.toString()
        displayUnit.text = entityCart.unit
        displayPrice.text = helper.intToRupiah(entityCart.product.price)
        pdQty.setText(entityCart.product.quantity.toString())
        pdDiscount.setText(entityCart.product.discountPercent.toString())

        pdOk.setOnClickListener {
            // set total after and before discount
            entityCart.product.discountNominal = helper.getDiscountNominal(entityCart.product.discountPercent, entityCart.product.price, entityCart.product.quantity)
            entityCart.product.totalBeforeDiscount = entityCart.product.price * entityCart.product.quantity
            entityCart.product.totalAfterDiscount = entityCart.product.totalBeforeDiscount - entityCart.product.discountNominal

            val resultSaveProductInCart = repositoryCart.updateProduct(entityCart)
            if (resultSaveProductInCart == 0) {
                helper.generateTOA(this@TransactionAdd, "Failed update product in cart", true)
                return@setOnClickListener
            }
            updateStatusCart()
            alertDialog.dismiss()
        }

        pdCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        pdDelete.setOnClickListener {
            val result = repositoryCart.deleteProductInCart(entityCart)
            if (result == 0) {
                helper.generateTOA(this@TransactionAdd, "Failed delete product in cart", true)
                return@setOnClickListener
            }
            updateStatusCart()
            alertDialog.dismiss()
        }
    }

    private fun updateStatusCart() {
        val statusCart = repositoryCart.getTotalPriceAndItem(viewModelTransaction.idTransaction)
        if (statusCart == null) {
            helper.generateTOA(this@TransactionAdd, "Something wrong with cart", true)
        } else {
            val totItemValue = "(${statusCart.itemCount} item)"
            binding.totitem.text = totItemValue
            binding.totpricecart.text = helper.intToRupiah(statusCart.totalAfterDiscount)
            displayProductInCart()
        }
    }

    private fun initPaymentDisplay() {
        paymentDisplay.btnShowDesc.setOnClickListener {
            val s = "Show description"
            val h = "Hide description"
            if (paymentDisplay.containerTotal.visibility == View.GONE) {
                paymentDisplay.containerTotal.visibility = View.VISIBLE
                paymentDisplay.btnShowDesc.text = h
            } else {
                paymentDisplay.containerTotal.visibility = View.GONE
                paymentDisplay.btnShowDesc.text = s
            }
        }
        paymentDisplay.showNominal.setOnClickListener {
            val s = "Show nominal"
            val h = "Hide nominal"
            if (paymentDisplay.typeNominal.visibility == View.GONE) {
                paymentDisplay.typeNominal.visibility = View.VISIBLE
                paymentDisplay.showNominal.text = h
            } else {
                paymentDisplay.typeNominal.visibility = View.GONE
                paymentDisplay.showNominal.text = s
            }
        }

        paymentDisplay.pDisPercent.addTextChangedListener(SetPercent(
            edtPercent = paymentDisplay.pDisPercent,
            edtNominal = paymentDisplay.pDisNominal,
            vMdTransaction = viewModelTransaction,
            helper = helper,
            setValueForTotal = ::setValueForTotal,
            dataExchange = viewModelTransaction.exchangeDiscount,
            isDiscount = true
        ))

        paymentDisplay.pDisNominal.addTextChangedListener(SetNominal(
            edtPercent = paymentDisplay.pDisPercent,
            edtNominal = paymentDisplay.pDisNominal,
            vMdTransaction = viewModelTransaction,
            helper = helper,
            setValueForTotal = ::setValueForTotal,
            dataExchange = viewModelTransaction.exchangeDiscount,
            isDiscount = true
        ))

        paymentDisplay.pTaxPercent.addTextChangedListener(SetPercent(
            edtPercent = paymentDisplay.pTaxPercent,
            edtNominal = paymentDisplay.pTaxNominal,
            vMdTransaction = viewModelTransaction,
            helper = helper,
            setValueForTotal = ::setValueForTotal,
            dataExchange = viewModelTransaction.exchangeTax,
            isDiscount = false
        ))

        paymentDisplay.pTaxNominal.addTextChangedListener(SetNominal(
            edtPercent = paymentDisplay.pTaxPercent,
            edtNominal = paymentDisplay.pTaxNominal,
            vMdTransaction = viewModelTransaction,
            helper = helper,
            setValueForTotal = ::setValueForTotal,
            dataExchange = viewModelTransaction.exchangeTax,
            isDiscount = false
        ))

        paymentDisplay.pAdmNominal.addTextChangedListener(SetCurrency(
            isAdm = true,
            vMdTransaction = viewModelTransaction,
            helper = helper,
            editText = paymentDisplay.pAdmNominal,
            setValueForTotal = ::setValueForTotal
        ))

        paymentDisplay.btnSrb.setOnClickListener {
            viewModelTransaction.dtoTransaction.cash += 1000
            paymentDisplay.cashConsumer.setText(helper.intToRupiah(viewModelTransaction.dtoTransaction.cash))
        }

        paymentDisplay.btnDrb.setOnClickListener {
            viewModelTransaction.dtoTransaction.cash += 2000
            paymentDisplay.cashConsumer.setText(helper.intToRupiah(viewModelTransaction.dtoTransaction.cash))
        }

        paymentDisplay.btnLrb.setOnClickListener {
            viewModelTransaction.dtoTransaction.cash += 5000
            paymentDisplay.cashConsumer.setText(helper.intToRupiah(viewModelTransaction.dtoTransaction.cash))
        }

        paymentDisplay.btnSprb.setOnClickListener {
            viewModelTransaction.dtoTransaction.cash += 10000
            paymentDisplay.cashConsumer.setText(helper.intToRupiah(viewModelTransaction.dtoTransaction.cash))
        }

        paymentDisplay.btnDprb.setOnClickListener {
            viewModelTransaction.dtoTransaction.cash += 20000
            paymentDisplay.cashConsumer.setText(helper.intToRupiah(viewModelTransaction.dtoTransaction.cash))
        }

        paymentDisplay.btnLmrb.setOnClickListener {
            viewModelTransaction.dtoTransaction.cash += 50000
            paymentDisplay.cashConsumer.setText(helper.intToRupiah(viewModelTransaction.dtoTransaction.cash))
        }

        paymentDisplay.btnSrbrb.setOnClickListener {
            viewModelTransaction.dtoTransaction.cash += 100000
            paymentDisplay.cashConsumer.setText(helper.intToRupiah(viewModelTransaction.dtoTransaction.cash))
        }

        paymentDisplay.cashConsumer.setRawInputType(2)
        paymentDisplay.cashConsumer.addTextChangedListener(SetCurrency(
            isAdm = false,
            vMdTransaction = viewModelTransaction,
            helper = helper,
            editText = paymentDisplay.cashConsumer,
            setValueForTotal = ::setValueForTotal
        ))

        paymentDisplay.btnResetCash.setOnClickListener { paymentDisplay.cashConsumer.setText("0") }
        paymentDisplay.cancelPayment.setOnClickListener {
            // close payment display
            binding.bodyTransactionAdd.visibility = View.VISIBLE
            binding.paymentDisplay.bodyPayment.visibility = View.GONE
        }
        paymentDisplay.processPayment.setOnClickListener {
            if (viewModelTransaction.dtoTransaction.returned < 0) {
                helper.generateTOA(this@TransactionAdd, "Invalid Cash Nominal", true)
            } else {
                displayProductTransaction()
            }
        }
    }

    private fun initProcessTransaction() {
        transactionSuccess.tdtProcess.setOnClickListener {
            processPayment()
        }
    }

    private fun setValueForTotal() {
        val totalPaymentAfterDiscount = viewModelTransaction.dtoTransaction.subTotalProduct - viewModelTransaction.dtoTransaction.discountNominal
        val totalPaymentAfterTax = totalPaymentAfterDiscount + viewModelTransaction.dtoTransaction.taxNominal
        viewModelTransaction.dtoTransaction.grandTotal = totalPaymentAfterTax + viewModelTransaction.dtoTransaction.adm
        viewModelTransaction.dtoTransaction.returned = viewModelTransaction.dtoTransaction.cash - viewModelTransaction.dtoTransaction.grandTotal
        paymentDisplay.pTotalPayment.text = helper.intToRupiah(viewModelTransaction.dtoTransaction.grandTotal)
        paymentDisplay.cashReturned.text = helper.intToRupiah(viewModelTransaction.dtoTransaction.returned)
        if (viewModelTransaction.dtoTransaction.returned < 0) {
            paymentDisplay.cashReturned.setTextColor(ContextCompat.getColor(this@TransactionAdd, R.color.red))
        } else {
            paymentDisplay.cashReturned.setTextColor(ContextCompat.getColor(this@TransactionAdd, R.color.black))
        }
    }

    private fun initBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPaymentSuccess) {
                    alertDialogForExitSuccess?.show()
                    return
                }
                val statusCart = repositoryCart.getTotalPriceAndItem(viewModelTransaction.idTransaction) ?: return setResult()
                if (statusCart.itemCount <= 0) return setResult()
                if (binding.bodyTransactionAdd.visibility == View.VISIBLE) {
                    alertDialogForExit?.show()
                    return
                }
                if (paymentDisplay.bodyPayment.visibility == View.VISIBLE) {
                    paymentDisplay.bodyPayment.visibility = View.GONE
                    binding.bodyTransactionAdd.visibility = View.VISIBLE
                    return
                }

                if (transactionSuccess.main.visibility == View.VISIBLE) {
                    transactionSuccess.main.visibility = View.GONE
                    paymentDisplay.bodyPayment.visibility = View.VISIBLE
                    return
                }
                alertDialogForExit?.show()
            }
        }
        onBackPressedDispatcher.addCallback(this@TransactionAdd, callback)
    }

    private fun initDialogExit(isSuccess: Boolean) {
        val builder = AlertDialog.Builder(this@TransactionAdd)
        val dialogLayout: View = if (isSuccess) {
            layoutInflater.inflate(R.layout.dialog_exit_transaction_success, null)
        } else {
            layoutInflater.inflate(R.layout.dialog_exit_transaction, null)
        }
        val btnOk = dialogLayout.findViewById<Button>(R.id.exit_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.exit_no)
        builder.setView(dialogLayout)

        if (isSuccess) {
            alertDialogForExitSuccess = builder.create() // Create the AlertDialog
            alertDialogForExitSuccess?.setCanceledOnTouchOutside(false)
            alertDialogForExitSuccess?.setOnCancelListener{ return@setOnCancelListener }

            btnOk.setOnClickListener {
                alertDialogForExitSuccess?.dismiss()
                setResult()
            }
            btnCancel.setOnClickListener {
                alertDialogForExitSuccess?.dismiss()
            }
        } else {
            alertDialogForExit = builder.create() // Create the AlertDialog
            alertDialogForExit?.setCanceledOnTouchOutside(false)
            alertDialogForExit?.setOnCancelListener{ return@setOnCancelListener }

            btnOk.setOnClickListener {
                alertDialogForExit?.dismiss()
                setResult()
            }
            btnCancel.setOnClickListener {
                alertDialogForExit?.dismiss()
            }
        }
    }

    private fun processPayment() {
        if (viewModelTransaction.dtoTransaction.grandTotal <= 0) return setResult()
        val currentDate = helper.getCurrentDate()
        viewModelTransaction.date = DateTime(
            created = currentDate,
            updated = currentDate
        )
        val entityTransaction = EntityTransaction(
            id= viewModelTransaction.idTransaction,
            edited= false,
            dataTransaction= viewModelTransaction.dtoTransaction,
            date= viewModelTransaction.date
        )
        viewModelTransaction.transactionItem = mutableListOf()
        val itemTransaction: MutableList<EntityTransactionItem> = mutableListOf()
        val historyStock: MutableList<EntityHistoryStock> = mutableListOf()
        for (p in repositoryCart.getProducts(viewModelTransaction.idTransaction)) {
            viewModelTransaction.transactionItem.add(p.product)
            itemTransaction.add(
                EntityTransactionItem(
                    transactionID= viewModelTransaction.idTransaction,
                    product= p.product,
                    date= viewModelTransaction.date
                )
            )
            historyStock.add(
                EntityHistoryStock(
                    pID = p.product.productID,
                    inStock = 0,
                    outStock = p.product.quantity,
                    currentStock = p.stock - p.product.quantity,
                    purchase = 0,
                    transactionID = viewModelTransaction.idTransaction,
                    info = "",
                    date = viewModelTransaction.date
                )
            )
        }
        // save to database
        val result = repositoryTransaction.saveTransaction(
            dataTransaction= entityTransaction,
            dataTransactionItem= itemTransaction,
            dataHistoryStock= historyStock,
        )
        if (result.toInt() == 0) return helper.generateTOA(this@TransactionAdd, "failed save transaction", true)
        bluetoothManager = this@TransactionAdd.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        printer = Printer(viewModelTransaction.idTransaction, helper)
        bluetoothReceiver = BluetoothReceiver(this@TransactionAdd)
        bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, bluetoothFilter)
        showDialogTurnOnBluetooth()

        isPaymentSuccess = true
        helper.generateTOA(this@TransactionAdd, "transaction success", true)
        transactionSuccess.tdtProcess.visibility = View.GONE
        transactionSuccess.cntDIaog.visibility = View.VISIBLE
        transactionSuccess.paymentStatus.visibility = View.VISIBLE

        // header
        val dateTimeStr = helper.formatSpecificDate(helper.unixTimestampToDate(viewModelTransaction.date.created))
        transactionSuccess.dateOrder.text = dateTimeStr.date
        transactionSuccess.orederId.text = viewModelTransaction.idTransaction
        transactionSuccess.orderTime.text = dateTimeStr.time

        transactionSuccess.btnBackHome.setOnClickListener{ setResult() }
        transactionSuccess.btnPrintTrs.setOnClickListener { printTransaction() }
    }

    private fun printTransaction() {
        // check bluetooth
        val requestPermission = checkBluetoothPermission()
        if (checkBluetoothPermission() != "") return requestBluetoothPermission.launch(requestPermission)
        // print transaction
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            helper.generateTOA(this@TransactionAdd, "Your device not supported bluetooth", true)
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            alertDialogForShowTurnOnBluetooth?.show()
            return
        }
        if (viewModelTransaction.owner.bluetoothPaired == "") {
            contractBluetoothSetting.launch(constant.newActivity())
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != constant.granted()) {
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
        var isValidPairedDevice = false
        var deviceAddress = ""
        val pairedDevices = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            if (device.name != viewModelTransaction.owner.bluetoothPaired) continue

            // Pair with the printer using the deviceAddress
            // You can use this deviceAddress to establish a connection later
            deviceAddress = device.address.toString()
            isValidPairedDevice = true
            break
        }

        if (!isValidPairedDevice || deviceAddress == "") {
            return helper.generateTOA(this, "Cannot print struck because some data paired device not valid\n\nPlease make sure you have paired bluetooth device.", true)
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        val socket = device.createRfcommSocketToServiceRecord(uuid)
        try {
            socket.connect()
            val oS = socket.outputStream
            val dataPrint = printer.generateStruckPayment(
                date= viewModelTransaction.date,
                t= viewModelTransaction.dtoTransaction,
                o= viewModelTransaction.owner,
                p= viewModelTransaction.transactionItem)
            for (d in dataPrint) {
                oS.write(d)
            }
            oS.flush()
            oS.close()
            setResult()
        } catch (e: Exception) {
            helper.generateTOA(this, "Socket timeout! is printer on?", true)
            socket.close()
        }
    }

    private fun checkBluetoothPermission(): String {
        if (!permission.isBluetoothPermissionGranted()) return Manifest.permission.BLUETOOTH
        if (!permission.isBluetoothAdminPermissionGranted()) return Manifest.permission.BLUETOOTH_ADMIN
        if (!permission.isBluetoothAccessCoarseLocationPermissionGranted()) return Manifest.permission.ACCESS_COARSE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this@TransactionAdd, Manifest.permission.BLUETOOTH_CONNECT) != constant.granted()) return Manifest.permission.BLUETOOTH_CONNECT
        }
        return ""
    }

    private fun setResult() {
        val resultIntent = Intent().apply {
            // Set any data to be returned, if needed
            this.putExtra(constant.transactionID(), viewModelTransaction.idTransaction)
            this.putExtra(constant.isUpdate(), isPaymentSuccess)
        }
        setResult(if (isPaymentSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun showDialogTurnOnBluetooth() {
        val builder = AlertDialog.Builder(this@TransactionAdd)
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_open_bluetooth, null)
        val btnOk = dialogLayout.findViewById<Button>(R.id.dbt_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.dbt_no)
        builder.setView(dialogLayout)
        alertDialogForShowTurnOnBluetooth = builder.create() // Create the AlertDialog
        alertDialogForShowTurnOnBluetooth?.setCanceledOnTouchOutside(false)
        alertDialogForShowTurnOnBluetooth?.setOnCancelListener{
            alertDialogForShowTurnOnBluetooth?.dismiss()
            setResult()
        }
        btnOk.setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            this.startActivity(intent)
            alertDialogForShowTurnOnBluetooth?.dismiss() // Dismiss the dialog after starting the Bluetooth settings activity
        }
        btnCancel.setOnClickListener {
            alertDialogForShowTurnOnBluetooth?.dismiss()
        }
    }

    override fun onBluetoothConnected() {}

    override fun onBluetoothDisconnected() {
        if (isPaymentSuccess) {
            alertDialogForShowTurnOnBluetooth?.show()
        }
    }

    override fun onBluetoothStateError(state: String) {
        helper.generateTOA(this, state, true)
    }

    private class SetPercent(private val isDiscount: Boolean, private var edtPercent: EditText, private var edtNominal: EditText, private var vMdTransaction: ViewModelTransaction, private val helper: Helper, private var dataExchange: DtoPercentNominal, private val setValueForTotal: () -> Unit): TextWatcher {
        var cp = 0
        var isFinish = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { cp = edtPercent.selectionStart }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (dataExchange.isFromNominal) {
                dataExchange.isFromNominal = false
                return
            }
            if (isFinish) {
                dataExchange.isFromPercent = true
                isFinish = false
                edtPercent.setSelection(cp)
                if (isDiscount) {
                    vMdTransaction.dtoTransaction.discountNominal = getDiscountNominal(vMdTransaction.dtoTransaction.subTotalProduct, vMdTransaction.dtoTransaction.discountPercent)
                } else {
                    val subTotalProductAfterDiscount = vMdTransaction.dtoTransaction.subTotalProduct - vMdTransaction.dtoTransaction.discountNominal
                    vMdTransaction.dtoTransaction.taxNominal = getDiscountNominal(
                        subTotalProductAfterDiscount,
                        vMdTransaction.dtoTransaction.taxPercent
                    )
                }
                setValueForTotal()
                // set nominal discount
                val nStr = if (isDiscount) {
                    helper.intToRupiah(vMdTransaction.dtoTransaction.discountNominal)
                } else {
                    helper.intToRupiah(vMdTransaction.dtoTransaction.taxNominal)
                }
                edtNominal.setText(nStr)
                return
            }
            isFinish = true
            if (isDiscount) {
                vMdTransaction.dtoTransaction.discountPercent = helper.strToDouble(s.toString())
            } else {
                vMdTransaction.dtoTransaction.taxPercent = helper.strToDouble(s.toString())
            }
            edtPercent.setText(s.toString())
        }

        private fun getDiscountNominal(nominal: Int, discount: Double): Int {
            val discountPercentage = discount / 100
            val totalDiscount = discountPercentage * nominal
            return totalDiscount.toInt()
        }
    }

    private class SetNominal(private val isDiscount: Boolean, private var edtPercent: EditText, private var edtNominal: EditText, private var vMdTransaction: ViewModelTransaction, private val helper: Helper, private var dataExchange: DtoPercentNominal, private val setValueForTotal: () -> Unit): TextWatcher {
        var cp = 0
        var isFinish = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { cp = edtNominal.selectionStart }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (dataExchange.isFromPercent) {
                dataExchange.isFromPercent = false
                return
            }
            if (isFinish) {
                isFinish = false
                dataExchange.isFromNominal = true
                setSelectionEditText()
                if (isDiscount) {
                    vMdTransaction.dtoTransaction.discountPercent = 0.0
                } else {
                    vMdTransaction.dtoTransaction.taxPercent = 0.0
                }
                setValueForTotal()
                // set percent discount
                edtPercent.setText("0")
                return
            }
            isFinish = true
            if (isDiscount) {
                vMdTransaction.dtoTransaction.discountNominal = helper.rupiahToInt(s.toString())
                edtNominal.setText(helper.intToRupiah(vMdTransaction.dtoTransaction.discountNominal))
            } else {
                vMdTransaction.dtoTransaction.taxNominal = helper.rupiahToInt(s.toString())
                edtNominal.setText(helper.intToRupiah(vMdTransaction.dtoTransaction.taxNominal))
            }
        }

        private fun setSelectionEditText() {
            val selection: Int = if (isDiscount) {
                if (cp < vMdTransaction.dtoTransaction.discountNominal.toString().length) cp else helper.intToRupiah(vMdTransaction.dtoTransaction.discountNominal).length
            } else {
                if (cp < vMdTransaction.dtoTransaction.taxNominal.toString().length) cp else helper.intToRupiah(vMdTransaction.dtoTransaction.taxNominal).length
            }
            edtNominal.setSelection(selection)
        }
    }

    private class SetCurrency(private val isAdm: Boolean,private var vMdTransaction: ViewModelTransaction, private val helper: Helper, private var editText: EditText, private val setValueForTotal: () -> Unit) : TextWatcher {
        private var cp = 0
        private var isFinish = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { cp = editText.selectionStart }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (isFinish) {
                isFinish = false
                helper.setSelectionEditText(editText, cp)
                setValueForTotal()
                return
            }
            if (isAdm) {
                vMdTransaction.dtoTransaction.adm = helper.rupiahToInt(s.toString())
            } else {
                vMdTransaction.dtoTransaction.cash = helper.rupiahToInt(s.toString())
            }
            isFinish = true
            helper.setEditTextWithRupiahFormat(editText)
        }
    }
}