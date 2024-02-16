package com.gulali.dein.models.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gulali.dein.models.dto.DateTime

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(onDelete= ForeignKey.CASCADE, entity=EntityUnit::class, parentColumns=["id"], childColumns=["unit"]),
        ForeignKey(onDelete= ForeignKey.CASCADE, entity=EntityCategory::class, parentColumns=["id"], childColumns=["category"]),
    ]
)
data class EntityProduct (
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var image: String,
    var name: String,
    var category: Int,
    var barcode: String,
    var stock: Int,
    var unit: Int,
    var purchase: Int,
    var price: Int,
    var info: String,
    @Embedded
    var date: DateTime
)