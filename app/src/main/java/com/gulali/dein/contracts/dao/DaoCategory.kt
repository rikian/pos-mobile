package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gulali.dein.models.entities.EntityCategory

@Dao
interface DaoCategory {
    @Query("SELECT * FROM categories")
    fun getCategories(): List<EntityCategory>

    @Query("SELECT * FROM categories WHERE id = :catId")
    fun getCategory(catId: Int): EntityCategory?

    @Insert
    fun insertCategory(data: EntityCategory): Long
}