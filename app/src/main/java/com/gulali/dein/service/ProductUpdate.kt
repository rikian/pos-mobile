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
import com.gulali.dein.databinding.ProductUpdateBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.helper.SetCurrency
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.entities.EntityCategory
import com.gulali.dein.models.entities.EntityProduct
import com.gulali.dein.models.entities.EntityUnit
import com.gulali.dein.models.viewmodels.ViewModelProductUpdate
import com.gulali.dein.repositories.RepositoryCategory
import com.gulali.dein.repositories.RepositoryProduct
import com.gulali.dein.repositories.RepositoryUnit
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

class ProductUpdate : AppCompatActivity() {
    private lateinit var binding: ProductUpdateBinding

    // DI
    private val permission: Permission by inject()
    private val helper: Helper by inject()
    private val constant: Constants by inject()
    private val repositoryCategory: RepositoryCategory by inject()
    private val repositoryUnit: RepositoryUnit by inject()
    private val repositoryProduct: RepositoryProduct by inject()

    // contract permission
    private lateinit var contractAccessCamera: ActivityResultLauncher<String>
    private lateinit var contractReadExternalStorage: ActivityResultLauncher<String>

    // contract intent
    private lateinit var contractTakePicture: ActivityResultLauncher<Uri>
    private lateinit var contractPickImage: ActivityResultLauncher<Intent>
    private lateinit var contractUnitAdd: ActivityResultLauncher<String>
    private lateinit var contractCategoryAdd: ActivityResultLauncher<String>

    // feature
    private lateinit var barcodeScanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null

    // view-model
    private val viewModelProductUpdate: ViewModelProductUpdate by inject()

    // List spinner
    private lateinit var units: List<EntityUnit>
    private lateinit var categories: List<EntityCategory>

    // image
    private var imageFile: File? = null
    private var correctedBitmap: Bitmap? = null
    private var imageNameFromGallery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProductUpdateBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        viewModelProductUpdate.productID = intent.getIntExtra(constant.productID(), 0)
        if (viewModelProductUpdate.productID == 0) return finish()
        viewModelProductUpdate.dataProduct = repositoryProduct.getProductByID(viewModelProductUpdate.productID) ?: return finish()

        // passing data to view
        binding.inptBarcode.setText(viewModelProductUpdate.dataProduct.barcode)
        binding.edtProductName.setText(viewModelProductUpdate.dataProduct.name)
        binding.anpEdtPurchase.setText(helper.intToRupiah(viewModelProductUpdate.dataProduct.purchase))
        binding.anpPriceA.setText(helper.intToRupiah(viewModelProductUpdate.dataProduct.price))
        val uri = helper.getUriFromGallery(contentResolver, viewModelProductUpdate.dataProduct.img)
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .into(binding.imgPrevProduct)
        }

        // initial contract permission
        contractAccessCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // set notify for open setting application
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uriImg = Uri.fromParts("package", packageName, null)
                intent.data = uriImg
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

        // initial button listener
        barcodeScanner = helper.initBarcodeScanner(this@ProductUpdate)
        mediaPlayer = helper.initBeebSound(this@ProductUpdate)

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

        binding.anpEdtPurchase.setRawInputType(2)
        binding.anpEdtPurchase.addTextChangedListener(SetCurrency(helper, binding.anpEdtPurchase))

        binding.anpPriceA.setRawInputType(2)
        binding.anpPriceA.addTextChangedListener(SetCurrency(helper, binding.anpPriceA))

        createNewListUnit()
        createNewListCategory()

        binding.upOk.setOnClickListener { updateProduct() }
    }

    private fun updateProduct() {
        try {
            // set default data product
            viewModelProductUpdate.entityProduct = EntityProduct(
                id= viewModelProductUpdate.productID,
                image= viewModelProductUpdate.dataProduct.img,
                name= viewModelProductUpdate.dataProduct.name,
                category= 0,
                barcode= viewModelProductUpdate.dataProduct.barcode,
                stock= viewModelProductUpdate.dataProduct.stock,
                unit= 0,
                purchase= viewModelProductUpdate.dataProduct.purchase,
                price= viewModelProductUpdate.dataProduct.price,
                info= viewModelProductUpdate.dataProduct.info,
                date= DateTime(
                    created = viewModelProductUpdate.dataProduct.created,
                    updated = viewModelProductUpdate.dataProduct.updated
                )
            )

            // check image
            if (correctedBitmap != null) {
                viewModelProductUpdate.entityProduct.image = "${UUID.randomUUID()}.jpeg"
            }

            if (imageNameFromGallery != null && imageNameFromGallery != "") {
                viewModelProductUpdate.entityProduct.image = imageNameFromGallery as String
            }

            // check barcode
            viewModelProductUpdate.entityProduct.barcode = binding.inptBarcode.text.toString()

            // check name
            val productName = binding.edtProductName.text.toString().trim().lowercase()
            if (productName == "") {
                helper.generateTOA(this, "Product name cannot be empty", true)
                return
            }
            viewModelProductUpdate.entityProduct.name = productName

            // check unit
            val unitName = binding.spinner.selectedItem.toString()
            if (unitName == "") {
                helper.generateTOA(this@ProductUpdate, "Invalid unit name", false)
                return
            }
            for (unit in units) {
                if (unit.name == unitName) {
                    viewModelProductUpdate.entityProduct.unit = unit.id
                    break
                }
            }
            if (viewModelProductUpdate.entityProduct.unit == 0) {
                helper.generateTOA(this@ProductUpdate, "Invalid unit name", false)
                return
            }

            // check category
            val categoryName = binding.spinnerCategory.selectedItem.toString()
            if (categoryName == "") {
                helper.generateTOA(this@ProductUpdate, "Invalid category name", false)
                return
            }
            for (category in categories) {
                if (category.name == categoryName) {
                    viewModelProductUpdate.entityProduct.category = category.id
                    break
                }
            }
            if (viewModelProductUpdate.entityProduct.category == 0) {
                helper.generateTOA(this@ProductUpdate, "Invalid category name", false)
            }

            // check purchase
            viewModelProductUpdate.entityProduct.purchase = helper.rupiahToInt(binding.anpEdtPurchase.text.toString())

            if (viewModelProductUpdate.entityProduct.purchase == 0) {
                helper.generateTOA(this, "Purchase cannot be empty", false)
                return
            }

            // check price
            viewModelProductUpdate.entityProduct.price = helper.rupiahToInt(binding.anpPriceA.text.toString())
            if (viewModelProductUpdate.entityProduct.price == 0) {
                helper.generateTOA(this, "Price cannot be empty", false)
                return
            }

            // info
            viewModelProductUpdate.entityProduct.info = binding.description.text.toString()

            val currentDate = helper.getCurrentDate()
            viewModelProductUpdate.entityProduct.date.updated = currentDate

            // update database
            viewModelProductUpdate.isUpdate = repositoryProduct.updateProduct(viewModelProductUpdate.entityProduct)
            if (!viewModelProductUpdate.isUpdate) {
                helper.generateTOA(this, "failed update data product", false)
                return
            }

            // save image to gallery if correct bitmap not null
            if (correctedBitmap != null) {
                saveImageToLocalStorage(contentResolver, getString(R.string.app_name), correctedBitmap, viewModelProductUpdate.entityProduct.image)
            }

            helper.generateTOA(this, "data product updated", false)
            setResult()
        } catch (e: Exception) {
            helper.generateTOA(this, "failed!!\n\nerror: ${e.message}", false)
        }
    }

    private fun setResult() {
        val resultIntent = Intent().apply {
            // Set any data to be returned, if needed
            this.putExtra(constant.productID(), viewModelProductUpdate.productID)
            this.putExtra(constant.isUpdate(), viewModelProductUpdate.isUpdate)
        }
        setResult(if (viewModelProductUpdate.isUpdate) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
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
            val fileUri = FileProvider.getUriForFile(this@ProductUpdate, constant.getAuthority(), file)
            contractTakePicture.launch(fileUri)
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
        listUnit.add(viewModelProductUpdate.dataProduct.unit)
        if (units.isNotEmpty()) {
            for (unit in units) {
                if (unit.name == viewModelProductUpdate.dataProduct.unit) {
                    continue
                } else {
                    listUnit.add(unit.name)
                }
            }
        }
        listUnit.add(constant.createNew())
        val ad: ArrayAdapter<String> = ArrayAdapter<String>(
            this@ProductUpdate,
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
        listCategory.add(viewModelProductUpdate.dataProduct.category)
        if (categories.isNotEmpty()) {
            for (category in categories) {
                if (category.name == viewModelProductUpdate.dataProduct.category) {
                    continue
                } else {
                    listCategory.add(category.name)
                }
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