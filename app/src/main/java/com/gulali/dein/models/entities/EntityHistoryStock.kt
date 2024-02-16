package com.gulali.dein.models.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gulali.dein.models.dto.DateTime

@Entity(
    tableName = "history_stock",
    foreignKeys = [
        ForeignKey(onDelete= ForeignKey.CASCADE, entity=EntityProduct::class, parentColumns=["id"], childColumns=["pID"]),
    ]
)
data class EntityHistoryStock(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var pID: Int,
    var inStock: Int,
    var outStock: Int,
    var currentStock: Int,
    var purchase: Int,
    var transactionID: String,
    var info: String,
    @Embedded
    var date: DateTime
)