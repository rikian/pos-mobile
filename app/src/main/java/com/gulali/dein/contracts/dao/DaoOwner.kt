package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gulali.dein.models.entities.EntityOwner

@Dao
interface DaoOwner {
    @Insert
    fun createOwner(data: EntityOwner): Long

    @Update
    fun updateOwner(data: EntityOwner)

    @Query("SELECT * FROM owner")
    fun getOwner(): List<EntityOwner>

    @Query("Update owner SET bluetoothPaired=:name WHERE id='001'")
    fun updateBluetooth(name: String)

    @Query("SELECT bluetoothPaired FROM owner WHERE id='001'")
    fun getOwnerBluetooth(): String

    @Query("DELETE FROM owner")
    fun truncateOwnerTable()
}