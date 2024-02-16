package com.gulali.dein.helper

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class BarcodeScanner {
    fun initBarcodeScanner(ctx: Context): GmsBarcodeScanner {
        val scanOptions = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .allowManualInput()
            .enableAutoZoom()
            .build()
        return GmsBarcodeScanning.getClient(ctx, scanOptions)
    }
}