package com.gulali.dein.service

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gulali.dein.R
import com.gulali.dein.databinding.RegistrationBinding
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.viewmodels.ViewModelRegistration
import com.gulali.dein.repositories.RepositoryOwner
import org.koin.android.ext.android.inject

class Registration : AppCompatActivity() {
    private lateinit var binding: RegistrationBinding

    // di
    private val helper: Helper by inject()
    private val repositoryOwner: RepositoryOwner by inject()
    private val viewModelRegistration: ViewModelRegistration by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RegistrationBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        binding.btnRegistration.setOnClickListener {
            try {
                val ownerName = helper.lowerString(binding.regOwner.text.toString())
                if (ownerName == "") {
                    helper.generateTOA(this, "owner cannot be empty", true)
                    return@setOnClickListener
                }
                val shopName = helper.lowerString(binding.regShopName.text.toString())
                if (shopName == "") {
                    helper.generateTOA(this, "shop name cannot be empty", true)
                    return@setOnClickListener
                }
                val regAddress = helper.lowerString(binding.regAddress.text.toString())
                if (regAddress == "") {
                    helper.generateTOA(this, "address cannot be empty", true)
                    return@setOnClickListener
                }
                val regPhone = binding.regPhone.text.toString()
                if (regPhone == "") {
                    helper.generateTOA(this, "phone number cannot be empty", true)
                    return@setOnClickListener
                }

                val currentDate = helper.getCurrentDate()
                viewModelRegistration.owner.id = "$currentDate-${helper.generateTransactionID()}"
                viewModelRegistration.owner.owner = ownerName
                viewModelRegistration.owner.shop = shopName
                viewModelRegistration.owner.address = regAddress
                viewModelRegistration.owner.phone = regPhone
                viewModelRegistration.owner.date.created = currentDate
                viewModelRegistration.owner.date.updated = currentDate

                val resultRegistration = repositoryOwner.createOwner(viewModelRegistration.owner)

                if (resultRegistration.toInt() == 0) {
                    return@setOnClickListener
                } else {
                    launchIndex()
                }
            } catch (e: Exception) {
                helper.generateTOA(this@Registration, e.message.toString(), true)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dataOwner = repositoryOwner.getOwner()
        if (!dataOwner.isNullOrEmpty()) {
            return launchIndex()
        }
    }

    private fun launchIndex() {
        val intent = Intent(this@Registration, Home::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}