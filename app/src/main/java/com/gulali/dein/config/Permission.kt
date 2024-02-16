package com.gulali.dein.config

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gulali.dein.models.constants.Constants

class Permission(
    private val ctx: Context,
    private val constant: Constants
) {
    private val granted = this.constant.granted()

    fun isCameraPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(ctx, constant.camera()) == granted
    }

    fun isReadExternalStoragePermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(ctx, constant.readExternalStorage()) == granted
    }

    fun isWriteExternalStoragePermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(ctx, constant.writeExternalStorage()) == granted
    }

    fun isBluetoothPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH) == granted
    }

    fun isBluetoothAdminPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN) == granted
    }

    fun isBluetoothAccessCoarseLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == granted
    }
}