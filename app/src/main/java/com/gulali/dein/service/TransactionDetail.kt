package com.gulali.dein.service

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gulali.dein.R
import com.gulali.dein.adapter.AdapterProductCartDetail
import com.gulali.dein.config.Permission
import com.gulali.dein.contracts.bluetooth.BluetoothStateListener
import com.gulali.dein.contracts.intents.ContractBluetoothUpdate
import com.gulali.dein.databinding.TransactionDetailBinding
import com.gulali.dein.helper.BluetoothReceiver
import com.gulali.dein.helper.Helper
import com.gulali.dein.helper.Printer
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.DtoTransactionItem
import com.gulali.dein.models.viewmodels.ViewModelTransactionDetail
import com.gulali.dein.repositories.RepositoryOwner
import com.gulali.dein.repositories.RepositoryTransaction
import com.gulali.dein.repositories.RepositoryTransactionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

class TransactionDetail : AppCompatActivity(), BluetoothStateListener {
    private lateinit var binding: TransactionDetailBinding

    // di
    private val permission: Permission by inject()
    private val helper: Helper by inject()
    private val viewModelTransactionDetail: ViewModelTransactionDetail by inject()
    private val constant: Constants by inject()
    private val repositoryOwner: RepositoryOwner by inject()
    private val repositoryTransaction: RepositoryTransaction by inject()
    private val repositoryTransactionItem: RepositoryTransactionItem by inject()

    // bluetooth for print
    private lateinit var requestBluetoothPermission: ActivityResultLauncher<String>
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var printer: Printer
    private var bluetoothReceiver: BluetoothReceiver? = null
    private lateinit var bluetoothFilter: IntentFilter
    private var alertDialogForShowTurnOnBluetooth: AlertDialog? = null

    // contract
    private lateinit var contractBluetoothSetting: ActivityResultLauncher<String>

    // share
    private lateinit var contractReadExternalStorage: ActivityResultLauncher<String>
    private lateinit var contractPickImage: ActivityResultLauncher<Intent>

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

        requestBluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                printStruct()
            } else {
                helper.generateTOA(this, "GPOS cannot print stuck because one of permission not allowed.\n\nYou can get instruction for more details in settings menu", true)
            }
        }
        bluetoothManager = this@TransactionDetail.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        printer = Printer(viewModelTransactionDetail.idTransaction, helper)
        bluetoothReceiver = BluetoothReceiver(this@TransactionDetail)
        bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, bluetoothFilter)
        showDialogTurnOnBluetooth()

        contractBluetoothSetting = registerForActivityResult(ContractBluetoothUpdate(constant)) { isUpdate ->
            if (isUpdate) {
                if (!getOwner()) {
                    helper.generateTOA(this@TransactionDetail, "Failed get data owner", true)
                }
                printStruct()
            } else {
                helper.generateTOA(this@TransactionDetail, "Printer not found", true)
            }
        }

        binding.tdtPrint.setOnClickListener { printStruct() }

        contractPickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data

                // Replace 'Your sharing content here' with the actual text content you want to share
                val shareContent = "Your sharing content here"

                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareContent)
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    type = "image/jpeg" // Set the correct MIME type for your image
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
        }

        contractReadExternalStorage = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // set notify for open setting application
                return@registerForActivityResult
            } else {
                pickImage()
            }
        }

        binding.btnShare.setOnClickListener {
            val screenshotPayment = captureViewBitmap(binding.wrStruckPayment) ?: return@setOnClickListener helper.generateTOA(this@TransactionDetail, "failed generate struct payment", true)
            val paymentName = "${viewModelTransactionDetail.idTransaction}.jpeg"
            // save image to gallery
            val resultSaveImage: Boolean = saveImageToLocalStorage(screenshotPayment, paymentName)
            if (!resultSaveImage) {
                helper.generateTOA(this@TransactionDetail, "failed generate struct payment", true)
                return@setOnClickListener
            }
            // get image from gallery
            val imageUri: Uri? = getUriFromGallery(paymentName)

            // Replace 'Your sharing content here' with the actual text content you want to share
            val shareContent = "This receipt is valid proof of payment from my app"
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareContent)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = "image/jpeg" // Set the correct MIME type for your image
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            shareResultLauncher.launch(sendIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        val paymentName = "${viewModelTransactionDetail.idTransaction}.jpeg"
        // Delete image from gallery
        deleteImageFromGallery(paymentName)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver)
        }

        val paymentName = "${viewModelTransactionDetail.idTransaction}.jpeg"
        // Delete image from gallery
        deleteImageFromGallery(paymentName)
    }

    private val shareResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        val paymentName = "${viewModelTransactionDetail.idTransaction}.jpeg"
        // Delete image from gallery
        deleteImageFromGallery(paymentName)
    }

    private fun deleteImageFromGallery(fileName: String) {
        try {
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            val contentResolver: ContentResolver = this@TransactionDetail.contentResolver

            // Get the image URI
            val imageUri: Uri? = getUriFromGallery(fileName)

            // Delete the image
            if (imageUri != null) {
                contentResolver.delete(imageUri, selection, selectionArgs)
            }
        } catch (e: Exception) {
            // Handle exception if needed
            helper.generateTOA(this@TransactionDetail, "failed delete struct payment", true)
        }
    }

    private fun getUriFromGallery(fileName: String): Uri? {
        var imageUri: Uri? = null
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndex(MediaStore.Images.Media._ID)
                val imageId = it.getLong(idColumn)
                imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId.toString())
            }
        }

        return imageUri
    }

    private fun captureViewBitmap(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun saveImageToLocalStorage(bitmap: Bitmap, uniqueFileName: String): Boolean {
        try {
            val appName = getString(R.string.app_name)
            val timestamp = System.currentTimeMillis()
            //Tell the media scanner about the new file so that it is immediately available to the user.
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, timestamp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$appName")
                values.put(MediaStore.Images.Media.IS_PENDING, true)
                values.put(MediaStore.Images.Media.DISPLAY_NAME, uniqueFileName)
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                val outputStream = contentResolver.openOutputStream(uri) ?: return false
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
                outputStream.close()
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
            } else {
                val imageFileFolder = File(
                    Environment.getExternalStorageDirectory().toString() + '/' + appName)
                if (!imageFileFolder.exists()) {
                    imageFileFolder.mkdirs()
                }
                val imageFile = File(imageFileFolder, uniqueFileName)
                val outputStream: OutputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
                outputStream.close()
                values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun pickImage() {
        if (!permission.isReadExternalStoragePermissionGranted()) {
            contractReadExternalStorage.launch(constant.readExternalStorage())
            return
        }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        contractPickImage.launch(intent)
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

    private fun printStruct() {
        // check bluetooth
        val requestPermission = checkBluetoothPermission()
        if (checkBluetoothPermission() != "") return requestBluetoothPermission.launch(requestPermission)
        // print transaction
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            helper.generateTOA(this@TransactionDetail, "Your device not supported bluetooth", true)
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            alertDialogForShowTurnOnBluetooth?.show()
            return
        }
        if (viewModelTransactionDetail.owner.bluetoothPaired == "") {
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
            if (device.name != viewModelTransactionDetail.owner.bluetoothPaired) continue

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
        val transactionItem = mutableListOf<DtoTransactionItem>()
        for (ti in viewModelTransactionDetail.transactionItem) {
            transactionItem.add(ti.product)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket.connect()
                val oS = socket.outputStream
                val dataPrint = printer.generateStruckPayment(
                    date= viewModelTransactionDetail.transaction.date,
                    t= viewModelTransactionDetail.transaction.dataTransaction,
                    o= viewModelTransactionDetail.owner,
                    p= transactionItem
                )
                for (d in dataPrint) {
                    withContext(Dispatchers.IO) {
                        oS.write(d)
                    }
                }
                withContext(Dispatchers.IO) {
                    oS.flush()
                }
                withContext(Dispatchers.IO) {
                    oS.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    helper.generateTOA(this@TransactionDetail, "Socket timeout! is the printer on?", true)
                }
            } finally {
                socket.close()
            }
        }
    }

    private fun checkBluetoothPermission(): String {
        if (!permission.isBluetoothPermissionGranted()) return Manifest.permission.BLUETOOTH
        if (!permission.isBluetoothAdminPermissionGranted()) return Manifest.permission.BLUETOOTH_ADMIN
        if (!permission.isBluetoothAccessCoarseLocationPermissionGranted()) return Manifest.permission.ACCESS_COARSE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this@TransactionDetail, Manifest.permission.BLUETOOTH_CONNECT) != constant.granted()) return Manifest.permission.BLUETOOTH_CONNECT
        }
        return ""
    }

    private fun showDialogTurnOnBluetooth() {
        val builder = AlertDialog.Builder(this@TransactionDetail)
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_open_bluetooth, null)
        val btnOk = dialogLayout.findViewById<Button>(R.id.dbt_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.dbt_no)
        builder.setView(dialogLayout)
        alertDialogForShowTurnOnBluetooth = builder.create() // Create the AlertDialog
        alertDialogForShowTurnOnBluetooth?.setCanceledOnTouchOutside(false)
        alertDialogForShowTurnOnBluetooth?.setOnCancelListener{
            alertDialogForShowTurnOnBluetooth?.dismiss()
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
        alertDialogForShowTurnOnBluetooth?.show()
    }

    override fun onBluetoothStateError(state: String) {}
}