package com.gulali.dein.models.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.dto.DtoTransactionItem

@Entity(
    tableName = "transaction_item",
    foreignKeys = [
        ForeignKey(onDelete= ForeignKey.CASCADE, entity = EntityTransaction::class, parentColumns = ["id"], childColumns = ["transactionID"]),
        ForeignKey(entity = EntityProduct::class, parentColumns = ["id"], childColumns = ["productID"]),
    ]
)
data class EntityTransactionItem (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionID: String,
    @Embedded
    var product: DtoTransactionItem,
    @Embedded
    var date: DateTime
)