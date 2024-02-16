package com.gulali.dein.models.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gulali.dein.models.dto.DateTime


@Entity(
    tableName = "units",
    indices = [Index(value = ["name"], unique = true)]
)
data class EntityUnit (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    @Embedded
    var date: DateTime
)