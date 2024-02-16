package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gulali.dein.models.entities.EntityHistoryStock

@Dao
interface DaoHistoryStock {
    @Query("SELECT * FROM history_stock WHERE pID=:id ORDER BY created DESC LIMIT 10")
    fun getHistoryStockById(id: Int): List<EntityHistoryStock>

    @Insert
    fun saveHistoryStock(data: EntityHistoryStock): Long

    @Insert
    fun saveAllHistoryStock(data: List<EntityHistoryStock>)
}