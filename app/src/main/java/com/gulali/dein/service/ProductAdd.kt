package com.gulali.dein.service

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.gulali.dein.R
import com.gulali.dein.config.Permission
import com.gulali.dein.contracts.intents.ContractCategoryAdd
import com.gulali.dein.contracts.intents.ContractTakePicture
import com.gulali.dein.contracts.intents.ContractUnitAdd
import com.gulali.dein.databinding.ProductAddBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.helper.SetCurrency
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.entities.EntityCategory
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityUnit
import com.gulali.dein.models.viewmodels.ViewModelProductAdd
import com.gulali.dein.repositories.RepositoryCategory
import com.gulali.dein.repositories.RepositoryProduct
import com.gulali.dein.repositories.RepositoryUnit
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

class ProductAdd : AppCompatActivity() {
    private lateinit var binding: ProductAddBinding

    // contract permission
    private lateinit var contractAccessCamera: ActivityResultLauncher<String>
    // private late init var contractWriteExternalStorage: ActivityResultLauncher<String>
    private lateinit var contractReadExternalStorage: ActivityResultLauncher<String>

    // contract intent
    private lateinit var contractTakePicture: ActivityResultLauncher<Uri>
    private lateinit var contractPickImage: ActivityResultLauncher<Intent>
    private lateinit var contractUnitAdd: ActivityResultLauncher<String>
    private lateinit var contractCategoryAdd: ActivityResultLauncher<String>

    // DI
    private val permission: Permission by inject()
    private val helper: Helper by inject()
    private val constant: Constants by inject()
    private val repositoryCategory: RepositoryCategory by inject()
    private val repositoryUnit: RepositoryUnit by inject()
    private val repositoryProduct: RepositoryProduct by inject()

    // view-model
    private val viewModelProductAdd: ViewModelProductAdd by inject()

    // feature
    private lateinit var barcodeScanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null

    // image
    private var imageFile: File? = null
    private var correctedBitmap: Bitmap? = null
    private var imageNameFromGallery: String? = null

    // List spinner
    private lateinit var units: List<EntityUnit>
    private lateinit var categories: List<EntityCategory>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProductAddBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        // initial contract permission
        contractAccessCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // set notify for open setting application
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                return@registerForActivityResult
            }

            openCamera()
        }

        contractReadExternalStorage = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // set notify for open setting application
                return@registerForActivityResult
            } else {
                pickImage()
            }
        }

        // initial contract intent
        contractCategoryAdd = registerForActivityResult(ContractCategoryAdd(constant)) { isDataChanged ->
            if (!isDataChanged) {
                binding.spinnerCategory.setSelection(0)
                return@registerForActivityResult
            }

            createNewListCategory()
        }

        contractUnitAdd = registerForActivityResult(ContractUnitAdd(constant)) { isDataChanged ->
            if (!isDataChanged) {
                binding.spinner.setSelection(0)
                return@registerForActivityResult
            }

            createNewListUnit()
        }

        contractTakePicture = registerForActivityResult(ContractTakePicture()) { isSuccessful ->
            if (!isSuccessful) {
                return@registerForActivityResult
            } else {
                handlePictureTaken()
            }
        }

        contractPickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageNameFromGallery = imageUri?.let {
                    helper.getFileNameFromUri(contentResolver, it)
                } ?: return@registerForActivityResult

                // Set the image URI to the ImageView using Glide for efficient loading
                Glide.with(this)
                    .load(imageUri)
                    .into(binding.imgPrevProduct)

                // set null the correct bitmap
                correctedBitmap = null
            }
        }

        barcodeScanner = helper.initBarcodeScanner(this@ProductAdd)
        mediaPlayer = helper.initBeebSound(this@ProductAdd)

        // initial button listener
        binding.btnTakeImgProduct.setOnClickListener { openCamera() }

        binding.btnGetImage.setOnClickListener { pickImage() }

        binding.btnScanBc2.setOnClickListener {
            barcodeScanner.startScan()
                .addOnSuccessListener { result ->
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                    }
                    binding.inptBarcode.setText(result.rawValue)
                }
                .addOnCanceledListener {}
                .addOnFailureListener {}
        }

        helper.initialSockListener(binding.anpQtyMin, binding.anpQtyPlus, binding.anpQtyTot)

        binding.anpEdtPurchase.setRawInputType(2)
        binding.anpEdtPurchase.addTextChangedListener(SetCurrency(helper, binding.anpEdtPurchase))

        binding.anpPriceA.setRawInputType(2)
        binding.anpPriceA.addTextChangedListener(SetCurrency(helper, binding.anpPriceA))

        binding.btnSaveProduct.setOnClickListener { saveProduct() }

        createNewListUnit()
        createNewListCategory()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaPlayer when the activity is destroyed
        mediaPlayer?.release()
    }

    private fun pickImage() {
        if (!permission.isReadExternalStoragePermissionGranted()) {
            contractReadExternalStorage.launch(constant.readExternalStorage())
            return
        }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        contractPickImage.launch(intent)
    }

    private fun saveProduct() {
        try {
            // check image
            if (correctedBitmap != null) {
                viewModelProductAdd.entityProduct.image = "${UUID.randomUUID()}.jpeg"
            }

            if (imageNameFromGallery != null && imageNameFromGallery != "") {
                viewModelProductAdd.entityProduct.image = imageNameFromGallery as String
            }

            // check barcode
            viewModelProductAdd.entityProduct.barcode = binding.inptBarcode.text.toString()

            // check name
            val productName = binding.edtProductName.text.toString().trim().lowercase()
            if (productName == "") {
                helper.generateTOA(this, "Product name cannot be empty", true)
                return
            }
            viewModelProductAdd.entityProduct.name = productName

            // check stock
            var stock = binding.anpQtyTot.text.toString().toIntOrNull()
            if (stock == null) {
                stock = 0
            }
            viewModelProductAdd.entityProduct.stock = stock

            // check unit
            val unitName = binding.spinner.selectedItem.toString()
            if (unitName == "") {
                helper.generateTOA(this@ProductAdd, "Invalid unit name", false)
                return
            }
            for (unit in units) {
                if (unit.name == unitName) {
                    viewModelProductAdd.entityProduct.unit = unit.id
                    break
                }
            }
            if (viewModelProductAdd.entityProduct.unit == 0) {
                helper.generateTOA(this@ProductAdd, "Invalid unit name", false)
                return
            }

            // check category
            val categoryName = binding.spinnerCategory.selectedItem.toString()
            if (categoryName == "") {
                helper.generateTOA(this@ProductAdd, "Invalid category name", false)
                return
            }
            for (category in categories) {
                if (category.name == categoryName) {
                    viewModelProductAdd.entityProduct.category = category.id
                    break
                }
            }
            if (viewModelProductAdd.entityProduct.category == 0) {
                helper.generateTOA(this@ProductAdd, "Invalid category name", false)
            }

            // check purchase
            viewModelProductAdd.entityProduct.purchase = helper.rupiahToInt(binding.anpEdtPurchase.text.toString())

            if (viewModelProductAdd.entityProduct.purchase == 0) {
                helper.generateTOA(this, "Purchase cannot be empty", false)
                return
            }

            // check price
            viewModelProductAdd.entityProduct.price = helper.rupiahToInt(binding.anpPriceA.text.toString())
            if (viewModelProductAdd.entityProduct.price == 0) {
                helper.generateTOA(this, "Price cannot be empty", false)
                return
            }

            // info
            viewModelProductAdd.entityProduct.info = binding.description.text.toString()

            val currentDate = helper.getCurrentDate()
            viewModelProductAdd.entityProduct.date.updated = currentDate
            viewModelProductAdd.entityProduct.date.created = currentDate

            viewModelProductAdd.entityHistoryStock = EntityHistoryStock(
                pID= viewModelProductAdd.idProduct,
                inStock= viewModelProductAdd.entityProduct.stock,
                outStock= 0,
                currentStock= viewModelProductAdd.entityProduct.stock,
                purchase= viewModelProductAdd.entityProduct.purchase,
                transactionID= "",
                info= viewModelProductAdd.entityProduct.info,
                date= viewModelProductAdd.entityProduct.date
            )

            viewModelProductAdd.idProduct = repositoryProduct.insertProduct(
                viewModelProductAdd.entityProduct,
                viewModelProductAdd.entityHistoryStock
            ).toInt()

            // save product to database
            if (viewModelProductAdd.idProduct == 0) {
                helper.generateTOA(this, "Failed save data product to database", false)
                return
            }

            // save image to gallery if correct bitmap not null
            if (correctedBitmap != null) {
                saveImageToLocalStorage(contentResolver, getString(R.string.app_name), correctedBitmap, viewModelProductAdd.entityProduct.image)
            }

            helper.generateTOA(this, "data product save to database", false)
            setResult()
            finish()
        } catch (e: Exception) {
            helper.generateTOA(this, "failed!!\n\nerror: ${e.message}", false)
        }
    }

    private fun setResult() {
        val status = viewModelProductAdd.idProduct != 0
        val resultIntent = Intent().apply {
            // Set any data to be returned, if needed
            this.putExtra(constant.productID(), viewModelProductAdd.idProduct)
        }
        setResult(if (status) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun openCamera() {
        // check permission
        if (!permission.isCameraPermissionGranted()) {
            contractAccessCamera.launch(constant.camera())
            return
        }

        // Create a temporary file to store the image
        imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${helper.generateUniqueImgName()}.jpeg")
        imageFile?.let { file ->
            val fileUri = FileProvider.getUriForFile(this@ProductAdd, constant.getAuthority(), file)
            contractTakePicture.launch(fileUri)
        }
    }

    private fun handlePictureTaken() {
        // Load the captured image from the file
        correctedBitmap = imageFile?.let { BitmapFactory.decodeFile(it.absolutePath) } ?: return
        if (correctedBitmap!!.byteCount > 50000000) {
            helper.generateTOA(this, "Image too large", false)
            // Delete the local file
            imageFile?.delete()
            imageFile = null
            return
        }
        // Rotate the image based on its orientation
        correctedBitmap = rotateImageIfRequired(correctedBitmap, imageFile?.absolutePath ?: "")

        // Set preview image
        binding.imgPrevProduct.setImageBitmap(correctedBitmap)

        // Delete the local file
        imageFile?.delete()
        imageFile = null

        // Set the image name from the gallery to an empty string or null
        imageNameFromGallery = null
    }

    private fun rotateImageIfRequired(bitmap: Bitmap?, imagePath: String): Bitmap? {
        if (bitmap == null) {
            return null
        }
        val ei = ExifInterface(imagePath)
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> this.rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> this.rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> this.rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap?, angle: Float): Bitmap? {
        if (bitmap == null) {
            return null
        }
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun createNewListUnit() {
        units = repositoryUnit.getUnits()
        val listUnit = mutableListOf<String>()
        listUnit.add("")
        if (units.isNotEmpty()) {
            for (unit in units) {
                listUnit.add(unit.name)
            }
        }
        listUnit.add(constant.createNew())
        val ad: ArrayAdapter<String> = ArrayAdapter<String>(
            this@ProductAdd,
            android.R.layout.simple_spinner_item,
            listUnit
        )
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = ad
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedUnit = listUnit[position]
                if (selectedUnit == constant.createNew()) {
                    // Open a new activity to create a new unit
                    contractUnitAdd.launch(constant.newActivity())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun createNewListCategory() {
        categories = repositoryCategory.getCategories()
        val listCategory = mutableListOf<String>()
        listCategory.add("")
        if (categories.isNotEmpty()) {
            for (category in categories) {
                listCategory.add(category.name)
            }
        }
        listCategory.add(constant.createNew())
        val ad: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listCategory)
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = ad
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = listCategory[position]
                if (selectedCategory == constant.createNew()) {
                    // Open a new activity to create a new unit
                    contractCategoryAdd.launch(constant.newActivity())
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun saveImageToLocalStorage(contentResolver: ContentResolver, appName: String, bitmap: Bitmap?, uniqueFileName: String): Boolean {
        try {
            if (bitmap == null) {
                return false
            }
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
}