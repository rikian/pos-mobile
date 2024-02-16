package com.gulali.dein.models.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.dto.DtoTransaction

@Entity(
    tableName = "transactions",
)
data class EntityTransaction (
    @PrimaryKey(autoGenerate = false)
    var id: String,
    var edited: Boolean = false,
    @Embedded
    var dataTransaction: DtoTransaction,
    @Embedded
    var date: DateTime
)