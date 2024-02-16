package com.gulali.dein.contracts.intents

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.models.dto.contract.DtoContractTransactionAdd
import com.gulali.dein.service.TransactionAdd

class ContractTransactionAdd(private val constant: Constants): ActivityResultContract<String, DtoContractTransactionAdd?>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, TransactionAdd::class.java).apply {
            putExtra(constant.newActivity(), input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): DtoContractTransactionAdd? {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            val transactionID = intent.getStringExtra(constant.transactionID())
            if (transactionID == null) {
                null
            } else {
                val isUpdate = intent.getBooleanExtra(constant.isUpdate(), false)
                DtoContractTransactionAdd(transactionID, isUpdate)
            }
        } else {
            null
        }
    }

}