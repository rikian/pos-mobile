package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gulali.dein.models.entities.EntityUnit

@Dao
interface DaoUnit {
    @Query("SELECT * FROM units")
    fun getUnits(): List<EntityUnit>

    @Query("SELECT * FROM units WHERE id = :unitId")
    fun getUnit(unitId: Int): EntityUnit?

    @Query("SELECT * FROM units WHERE name = :unitId")
    fun getUnitByName(unitId: String): EntityUnit?

    @Insert
    fun insertUnit(data: EntityUnit): Long
}