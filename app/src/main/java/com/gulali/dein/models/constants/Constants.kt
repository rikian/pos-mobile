package com.gulali.dein.models.constants

import android.Manifest
import android.content.pm.PackageManager

class Constants {
    fun granted(): Int {
        return PackageManager.PERMISSION_GRANTED
    }

    fun transactionID(): String {
        return "TRANSACTION_ID"
    }

    fun productID(): String {
        return "PRODUCT_ID"
    }

    fun isUpdate(): String {
        return "IS_UPDATE"
    }

    fun camera(): String {
        return Manifest.permission.CAMERA
    }

    fun writeExternalStorage(): String {
        return Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    fun readExternalStorage(): String {
        return Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun getAuthority(): String {
        return "com.gulali.dein.fileProvider"
    }

    fun newActivity(): String {
        return "NEW_ACTIVITY"
    }

    fun createNew(): String {
        return "Create New"
    }
}