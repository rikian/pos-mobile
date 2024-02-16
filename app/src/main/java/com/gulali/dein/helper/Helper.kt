package com.gulali.dein.helper

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.gulali.dein.R
import com.gulali.dein.models.dto.DateTimeFormat
import com.gulali.dein.models.dto.DtoTransaction
import com.gulali.dein.service.Registration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class Helper {
    fun generateTransactionID(): String {
        return "${UUID.randomUUID()}".split("-")[0]
    }

    fun generateTOA(ctx: Context, msg: String, isShort: Boolean) {
        return if (isShort) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun dateToUnixTimestamp(date: Date): Long {
        return date.time / 1000 // Convert milliseconds to seconds
    }

    fun getCurrentDate(): Long {
        val calendar = Calendar.getInstance()
        val date = calendar.time
        return this.dateToUnixTimestamp(date)
    }

    fun formatSpecificDate(date: Date): DateTimeFormat{
        val d = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(date)
        val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)

        return DateTimeFormat(
            date = d,
            time = t,
        )
    }

    fun unixTimestampToDate(timestamp: Long): Date {
        return Date(timestamp * 1000) // Convert seconds to milliseconds
    }

    fun generateUniqueImgName(): String {
        return "dein-${UUID.randomUUID()}"
    }

    fun initBeebSound(ctx: Context): MediaPlayer? {
        return MediaPlayer.create(ctx, R.raw.beep_scanner)
    }

    fun initBarcodeScanner(ctx: Context): GmsBarcodeScanner {
        val scanOptions = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .allowManualInput()
            .enableAutoZoom()
            .build()
        return GmsBarcodeScanning.getClient(ctx, scanOptions)
    }

    fun initialSockListener(qtyMin: Button, qtyPlus: Button, input: EditText) {
        qtyMin.setOnClickListener {
            val value = input.text.toString().toIntOrNull()
            if (value != null && value > 0) {
                val newVal = (value - 1).toString()
                input.setText(newVal)
                input.setSelection(newVal.length)
            }
        }

        qtyPlus.setOnClickListener {
            val value = input.text.toString().toIntOrNull()
            if (value != null) {
                val newVal = (value + 1).toString()
                input.setText(newVal)
                input.setSelection(newVal.length)
            }
        }
    }

    fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        var displayName: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            cursor.moveToFirst()
            displayName = cursor.getString(nameIndex)
        }

        return displayName
    }

    fun getUriFromGallery(contentResolver: ContentResolver, fileName: String): Uri? {
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

    // for set rupiah in edit text
    fun intToRupiah(value: Int): String {
        val v = value.toString()
        val vp = v.chunked(1)

        if (vp.size <= 3) {
            return v
        }

        val rvp = mutableListOf<String>()
        var n = 0
        for (i in vp.size downTo 1) {
            if (n == 3) {
                n = 0
                rvp.add(".")
            }
            rvp.add(vp[i - 1])
            n++
        }

        var r = ""
        for (i in rvp.size downTo 1) {
            r += rvp[i - 1]
        }

        return r
    }

    fun rupiahToInt(rupiah: String): Int {
        if (rupiah == "") return 0

        // Remove non-numeric characters and the dot separator
        val cleanedString = rupiah.replace("\\D".toRegex(), "")

        if (cleanedString.isNotBlank()) {
            return cleanedString.toInt()
        }

        // Return 0 if the cleaned string is empty or contains only non-numeric characters
        return 0
    }

    fun setSelectionEditText(edt: EditText, sS: Int) {
        val sE = this.rupiahToInt(edt.text.toString())
        val selection = if (sS < sE.toString().length) sS else this.intToRupiah(sE).length
        edt.setSelection(selection)
    }

    fun setEditTextWithRupiahFormat(e: EditText) {
        val nominal = this.rupiahToInt(e.text.toString())
        e.setText(this.intToRupiah(nominal))
    }

    fun strToInt(v: String): Int {
        return try {
            v.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getDiscountNominal(discount: Double, price: Int, qty: Int): Int{
        val totalPrice = price * qty
        val discountPercentage = discount / 100
        val discountNominal = discountPercentage * totalPrice
        return discountNominal.toInt()
    }

    fun getTotalPriceAfterDiscount(discount: Double, price: Int, qty: Int): Int {
        val totalPrice = price * qty
        val discountNominal = getDiscountNominal(discount, price, qty)
        return totalPrice - discountNominal
    }

    fun setPriceAfterDiscount(
        targetBeforeDiscount: TextView,
        targetAfterDiscount: TextView,
        discount: Double,
        price: Int,
        qty: Int,
        needRp: Boolean,
        ctx: Context
    ) {
        try {
            val totalPrice = price * qty
            val totalPriceStr: String = if (needRp) {
                "Rp ${this.intToRupiah(totalPrice)}"
            } else {
                this.intToRupiah(totalPrice)
            }

            targetBeforeDiscount.text = totalPriceStr
            targetBeforeDiscount.paintFlags = targetBeforeDiscount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            targetBeforeDiscount.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            targetBeforeDiscount.setTypeface(null, Typeface.NORMAL)
            targetBeforeDiscount.setTypeface(null, Typeface.BOLD)

            if (discount == 0.0) {
                targetAfterDiscount.visibility = View.GONE
            } else {
                val totalPriceAfterDiscount = getTotalPriceAfterDiscount(discount, price, qty)
                val totalPriceAfterDiscountStr = if (needRp) {
                    "Rp ${this.intToRupiah(totalPriceAfterDiscount.toInt())}"
                } else {
                    this.intToRupiah(totalPriceAfterDiscount.toInt())
                }
                targetBeforeDiscount.paintFlags = targetBeforeDiscount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                targetBeforeDiscount.setTextColor(ContextCompat.getColor(ctx, R.color.text_gray))
                targetBeforeDiscount.setTypeface(null, Typeface.ITALIC)

                targetAfterDiscount.visibility = View.VISIBLE
                targetAfterDiscount.text = totalPriceAfterDiscountStr
            }
        } catch (e: Exception) {
            this.generateTOA(ctx, "Something wrong!!\nPlease make sure you give the correct input", true)
        }
    }

    fun strToDouble(v: String): Double {
        return try {
            v.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    fun lowerString(data: String): String {
        return data.trim().lowercase()
    }

    fun launchRegistration(activity: Activity) {
        val intent = Intent(activity, Registration::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}