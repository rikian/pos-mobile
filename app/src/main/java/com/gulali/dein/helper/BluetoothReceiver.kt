package com.gulali.dein.helper

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gulali.dein.contracts.bluetooth.BluetoothStateListener

class BluetoothReceiver(private val listener: BluetoothStateListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        when (val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
            BluetoothAdapter.STATE_ON -> {
                // Bluetooth is connected
                listener.onBluetoothConnected()
            }
            BluetoothAdapter.STATE_OFF -> {
                // Bluetooth is disconnected
                listener.onBluetoothDisconnected()
            }
            else -> {
                // There is something wrong with bluetooth state
                val stateMessage = getStateMessage(state)
                listener.onBluetoothStateError(stateMessage)
            }
        }
    }

    private fun getStateMessage(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_ON -> "Bluetooth is on"
            BluetoothAdapter.STATE_OFF -> "Bluetooth is off"
            else -> "Unknown Bluetooth state"
        }
    }
}