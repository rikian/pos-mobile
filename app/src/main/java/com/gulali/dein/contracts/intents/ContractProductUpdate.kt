package com.gulali.dein.contracts.intents

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.contract.DtoContractProductDetail
import com.gulali.dein.service.ProductDetail
import com.gulali.dein.service.ProductUpdate

class ContractProductUpdate(private val constant: Constants): ActivityResultContract<Int, DtoContractProductDetail?>() {
    override fun createIntent(context: Context, input: Int): Intent {
        return Intent(context, ProductUpdate::class.java).apply {
            putExtra(constant.productID(), input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): DtoContractProductDetail? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            val productID = intent.getIntExtra(constant.productID(), 0)
            val isUpdate = intent.getBooleanExtra(constant.isUpdate(), false)
            DtoContractProductDetail(productID, isUpdate)
        } else {
            null
        }
    }
}