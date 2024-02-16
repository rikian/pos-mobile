package com.gulali.dein.models.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gulali.dein.models.dto.DtoTransactionItem

@Entity(
    tableName = "carts",
    foreignKeys = [
        ForeignKey(onDelete= ForeignKey.CASCADE, entity = EntityProduct::class, parentColumns = ["id"], childColumns = ["productID"]),
    ],
    indices = [Index(value = ["productID"], unique = true)]
)
data class EntityCart(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var transactionID: String,
    var img: String = "default.jpeg",
    var stock: Int,
    var unit: String,
    @Embedded
    var product: DtoTransactionItem,
    var createdAt: Long
)