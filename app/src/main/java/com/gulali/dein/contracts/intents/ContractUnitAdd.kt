package com.gulali.dein.contracts.intents

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.gulali.dein.models.constants.Constants
import com.gulali.dein.service.UnitAdd

class ContractUnitAdd(private val constants: Constants): ActivityResultContract<String, Boolean>(){
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, UnitAdd::class.java).apply {
            putExtra(constants.newActivity(), input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}