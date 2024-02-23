package com.gulali.dein.helper

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView

class SetPriceTotalProduct(
    private val helper: Helper,
    private val input: EditText,
    private var target: TextView
): TextWatcher {
    private var isFinish = false
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
        val value = input.toString().toIntOrNull()
        if (isFinish) {
            isFinish = false
            input.setSelection(input.text.length)
            return
        }
    }
}