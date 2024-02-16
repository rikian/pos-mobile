package com.gulali.dein.service

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gulali.dein.databinding.UnitAddBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.entities.EntityUnit
import com.gulali.dein.models.viewmodels.ViewModelUnit
import com.gulali.dein.repositories.RepositoryUnit
import org.koin.android.ext.android.inject

class UnitAdd : AppCompatActivity() {
    private lateinit var binding: UnitAddBinding
    private val repoUnit: RepositoryUnit by inject()
    private val helper: Helper by inject()
    private val viewModelUnit: ViewModelUnit by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitAddBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        binding.saveUnit.setOnClickListener {
            viewModelUnit.name = binding.unitName.text.toString().trim().lowercase()
            if (viewModelUnit.name == "") {
                helper.generateTOA(this, "Unit cannot be empty", true)
                return@setOnClickListener
            }
            if (viewModelUnit.name.length > 16) {
                helper.generateTOA(this, "Max unit name 16 character", true)
                return@setOnClickListener
            }
            val dateTimeLong = DateTime(
                created = helper.getCurrentDate(),
                updated = helper.getCurrentDate(),
            )
            val unitEntity = EntityUnit(
                name = viewModelUnit.name,
                date = dateTimeLong
            )
            val resultInsertUnit = repoUnit.insertUnit(unitEntity)
            if (resultInsertUnit.error != null) {
                helper.generateTOA(this@UnitAdd, resultInsertUnit.error.toString(), true)
                return@setOnClickListener
            }
            helper.generateTOA(this@UnitAdd, "data is saved", true)
            setResult(true)
        }

        binding.btnBackTbc.setOnClickListener {
            setResult(false)
        }
    }

    private fun setResult(status: Boolean) {
        val resultIntent = Intent().apply {
            // Set any data to be returned, if needed
        }
        setResult(if (status) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
        finish()
    }
}