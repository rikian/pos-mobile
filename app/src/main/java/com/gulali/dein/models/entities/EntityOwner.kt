package com.gulali.dein.models.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gulali.dein.models.dto.DateTime

@Entity(
    tableName = "owner"
)
data class EntityOwner (
    @PrimaryKey(autoGenerate = false)
    var id: String,
    var shop: String,
    var owner: String,
    var address: String,
    var phone: String,
    var discountPercent: Double,
    var discountNominal: Int,
    var taxPercent: Double,
    var taxNominal: Int,
    var adm: Int,
    var bluetoothPaired: String,
    @Embedded
    var date: DateTime
)