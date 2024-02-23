package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.gulali.dein.models.entities.EntityTransactionItem

@Dao
interface DaoTransactionItem {
    @Query("SELECT * FROM transaction_item WHERE transactionID= :transactionID")
    fun getTransactionItemById(transactionID: String): MutableList<EntityTransactionItem>

    @Query("SELECT SUM(quantity) FROM transaction_item WHERE productID=:productID")
    fun getSoldOut(productID: Int): Int

    @Insert
    fun insertTransactionItems(data: EntityTransactionItem): Long

    @Transaction
    fun saveTransactionItems(data: List<EntityTransactionItem>): Int {
        var result = 0
        try {
            for (d in data) {
                if (insertTransactionItems(d) != -1L) {
                    result++
                } else {
                    throw RuntimeException("Failed to insert item: $d")
                }
            }
        } catch (e: Exception) {
            result = 0
        }

        return result
    }
}