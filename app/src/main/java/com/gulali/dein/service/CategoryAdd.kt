package com.gulali.dein.service

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gulali.dein.databinding.CategoryAddBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.entities.EntityCategory
import com.gulali.dein.models.viewmodels.ViewModelCategory
import com.gulali.dein.repositories.RepositoryCategory
import org.koin.android.ext.android.inject

class CategoryAdd : AppCompatActivity() {
    private lateinit var binding: CategoryAddBinding

    private val repositoryCategory: RepositoryCategory by inject()
    private val helper: Helper by inject()
    private val viewModelCategory: ViewModelCategory by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CategoryAddBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        binding.saveCategory.setOnClickListener{
            viewModelCategory.name = binding.categoryName.text.toString().trim().lowercase()
            if (viewModelCategory.name == "") {
                helper.generateTOA(this, "Category name cannot be empty", true)
                return@setOnClickListener
            }

            val dateTimeLong = DateTime(
                created = helper.getCurrentDate(),
                updated = helper.getCurrentDate(),
            )

            val categoryEntity = EntityCategory (
                name = viewModelCategory.name,
                date = dateTimeLong
            )

            val resultInsertCategory = repositoryCategory.insertCategory(categoryEntity)
            if (resultInsertCategory.error != null) {
                helper.generateTOA(this@CategoryAdd, resultInsertCategory.error.message.toString(), true)
                return@setOnClickListener
            }

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