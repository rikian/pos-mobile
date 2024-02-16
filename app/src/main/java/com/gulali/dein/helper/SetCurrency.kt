package com.gulali.dein.helper

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class SetCurrency(private var helper: Helper, private var editText: EditText): TextWatcher {
    private var cp = 0
    private var isFinish = false
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { cp = editText.selectionStart }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun afterTextChanged(s: Editable?) {
        try {
            if (isFinish) {
                isFinish = false
                helper.setSelectionEditText(editText, cp)
                return
            }
            val userInput = helper.rupiahToInt(s.toString())
            if (userInput == 0) {
                isFinish = true
                editText.setText("")
                return
            }
            isFinish = true
            helper.setEditTextWithRupiahFormat(editText)
        } catch (e: Exception) {
            println(e.message.toString())
        }
    }
}