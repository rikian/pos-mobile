package com.gulali.dein.contracts.intents

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.service.ProductAdd

class ContractProductAdd(private val constants: Constants): ActivityResultContract<String, Int>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, ProductAdd::class.java).apply {
            putExtra(constants.newActivity(), input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Int {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            intent.getIntExtra(constants.productID(), 0)
        } else {
            0
        }
    }
}