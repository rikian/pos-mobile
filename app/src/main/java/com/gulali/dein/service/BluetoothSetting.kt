package com.gulali.dein.service

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gulali.dein.R
import com.gulali.dein.adapter.AdapterStringItem
import com.gulali.dein.config.Permission
import com.gulali.dein.contracts.bluetooth.BluetoothStateListener
import com.gulali.dein.databinding.BluetoothSettingBinding
import com.gulali.dein.helper.BluetoothReceiver
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.viewmodels.ViewModelBluetoothUpdate
import com.gulali.dein.repositories.RepositoryOwner
import org.koin.android.ext.android.inject

class BluetoothSetting : AppCompatActivity(), BluetoothStateListener {
    private lateinit var binding: BluetoothSettingBinding

    // DI
    private val permission: Permission by inject()
    private val helper: Helper by inject()
    private val constant: Constants by inject()
    private val repositoryOwner: RepositoryOwner by inject()
    private val viewModelBluetoothUpdate: ViewModelBluetoothUpdate by inject()

    // bluetooth for print
    private lateinit var requestBluetoothPermission: ActivityResultLauncher<String>
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothReceiver: BluetoothReceiver? = null
    private lateinit var bluetoothFilter: IntentFilter
    private var alertDialogForShowTurnOnBluetooth: AlertDialog? = null
    private var isUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BluetoothSettingBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }
        // This must be executed every time the update button is pressed
        requestBluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getPairedDevice()
            } else {
                helper.generateTOA(this, "GPOS cannot show list paired because one of permission not allowed.\n\nYou can get instruction for more details in settings menu", true)
            }
        }
        bluetoothManager = this@BluetoothSetting.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothReceiver = BluetoothReceiver(this@BluetoothSetting)
        bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, bluetoothFilter)
        showDialogTurnOnBluetooth()

        binding.addPairedDevice.setOnClickListener { openBluetoothSetting() }

        binding.setUpdate.setOnClickListener {
            if (viewModelBluetoothUpdate.pairedChooser == "") {
                return@setOnClickListener helper.generateTOA(this@BluetoothSetting, "Please select one of paired devices in the list before update", true)
            }
            viewModelBluetoothUpdate.owner.bluetoothPaired = viewModelBluetoothUpdate.pairedChooser
            repositoryOwner.updateOwner(viewModelBluetoothUpdate.owner)
            val dataOwner = repositoryOwner.getOwner()
            if (dataOwner.isNullOrEmpty() || dataOwner[0].bluetoothPaired != viewModelBluetoothUpdate.pairedChooser) {
                helper.generateTOA(this@BluetoothSetting, "Failed to update bluetooth paired", true)
            } else {
                isUpdate = true
                helper.generateTOA(this@BluetoothSetting, "Updated", true)
                setResult()
            }
        }

        if (!bluetoothManager.adapter.isEnabled) {
            alertDialogForShowTurnOnBluetooth?.show()
            return
        }

        getPairedDevice()
    }

    override fun onStart() {
        super.onStart()
        if (!getOwner()) {
            helper.launchRegistration(this@BluetoothSetting)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver)
        }
    }

    private fun getPairedDevice() {
        val requestPermission = checkBluetoothPermission()
        if (checkBluetoothPermission() != "") return requestBluetoothPermission.launch(requestPermission)
        val pairedDeviceArr: MutableList<String> = mutableListOf()
        val pairedDevices = bluetoothManager.adapter.bondedDevices
        for (device in pairedDevices) {
            pairedDeviceArr.add(device.name)
        }
        showListPairedDevice(pairedDeviceArr)
    }

    private fun showListPairedDevice(items: List<String>) {
        val adapter = AdapterStringItem(this@BluetoothSetting, items)
        adapter.setOnItemClickListener(object : AdapterStringItem.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val bluetoothName = items[position]
                binding.printerName.text = bluetoothName
                viewModelBluetoothUpdate.pairedChooser = bluetoothName
                return
            }
        })
        binding.recyclerView.adapter = adapter
    }

    private fun getOwner(): Boolean {
        val dataOwner = repositoryOwner.getOwner()
        return if (dataOwner.isNullOrEmpty()) {
            false
        } else {
            viewModelBluetoothUpdate.owner = dataOwner[0]
            binding.printerName.text = viewModelBluetoothUpdate.owner.bluetoothPaired
            true
        }
    }

    private fun checkBluetoothPermission(): String {
        if (!permission.isBluetoothPermissionGranted()) return Manifest.permission.BLUETOOTH
        if (!permission.isBluetoothAdminPermissionGranted()) return Manifest.permission.BLUETOOTH_ADMIN
        if (!permission.isBluetoothAccessCoarseLocationPermissionGranted()) return Manifest.permission.ACCESS_COARSE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this@BluetoothSetting, Manifest.permission.BLUETOOTH_CONNECT) != constant.granted()) return Manifest.permission.BLUETOOTH_CONNECT
        }
        return ""
    }

    private fun showDialogTurnOnBluetooth() {
        val builder = AlertDialog.Builder(this@BluetoothSetting)
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
            openBluetoothSetting()
            alertDialogForShowTurnOnBluetooth?.dismiss() // Dismiss the dialog after starting the Bluetooth settings activity
        }
        btnCancel.setOnClickListener {
            alertDialogForShowTurnOnBluetooth?.dismiss()
            setResult()
        }
    }

    private fun openBluetoothSetting() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    }

    private fun setResult() {
        val resultIntent = Intent().apply {
            // Set any data to be returned, if needed
            this.putExtra(constant.isUpdate(), isUpdate)
        }
        setResult(if (isUpdate) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    override fun onBluetoothConnected() {
        alertDialogForShowTurnOnBluetooth?.dismiss()
        getPairedDevice()
    }

    override fun onBluetoothDisconnected() {
        alertDialogForShowTurnOnBluetooth?.show()
    }

    override fun onBluetoothStateError(state: String) {}
}