package com.gulali.dein.contracts.bluetooth

interface BluetoothStateListener {
    fun onBluetoothConnected()
    fun onBluetoothDisconnected()
    fun onBluetoothStateError(state: String)
}