package com.gulali.dein.helper

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import com.gulali.dein.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ShareStructPayment(
    private val ctx: Context,
    private val helper: Helper
) {
    private val errMsg = "Failed generate struct payment for share"

    private fun generateErr() {
        return helper.generateTOA(ctx, errMsg, true)
    }

    fun share(view: View, idTransaction: String, message: String, shareResultLauncher: ActivityResultLauncher<Intent>) {
        val screenshotPayment = captureViewBitmap(view) ?: return generateErr()
        val paymentName = "${idTransaction}.jpeg"
        // save image to gallery
        val resultSaveImage: Boolean = saveImageToLocalStorage(screenshotPayment, paymentName)
        if (!resultSaveImage) return generateErr()
        // get image from gallery
        val imageUri: Uri? = getUriFromGallery(paymentName)

        // Replace message 'Your sharing content here' with the actual text content you want to share
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/jpeg" // Set the correct MIME type for your image
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        shareResultLauncher.launch(sendIntent)
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
            val appName = ctx.getString(R.string.app_name)
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
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                val outputStream = ctx.contentResolver.openOutputStream(uri) ?: return false
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
                outputStream.close()
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                ctx.contentResolver.update(uri, values, null, null)
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
                ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun getUriFromGallery(fileName: String): Uri? {
        var imageUri: Uri? = null
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = ctx.contentResolver.query(
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

    fun deleteImageFromGallery(fileName: String): Boolean {
        return try {
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            val contentResolver: ContentResolver = ctx.contentResolver
            // Get the image URI
            val imageUri: Uri? = getUriFromGallery(fileName)
            // Delete the image
            if (imageUri != null) {
                contentResolver.delete(imageUri, selection, selectionArgs)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            // Handle exception if needed
            false
        }
    }
}