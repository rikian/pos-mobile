package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.entities.EntityTransactionItem

@Dao
interface DaoTransaction {
    @Insert
    fun insertOne(data: EntityTransaction): Long

    @Query("SELECT * FROM `transactions` ORDER BY created DESC LIMIT :limit OFFSET :index * :limit")
    fun getTransaction(index: Int, limit: Int): MutableList<EntityTransaction>

    @Query("SELECT * FROM `transactions` WHERE id= :transactionID")
    fun getTransactionByID(transactionID: String): EntityTransaction

    @Transaction
    fun saveTransaction(
        dataTransaction: EntityTransaction,
        dataTransactionItem: List<EntityTransactionItem>,
        dataHistoryStock: List<EntityHistoryStock>,
        daoHistoryStock: DaoHistoryStock,
        daoTransactionItem: DaoTransactionItem,
        daoProduct: DaoProduct
    ): Long {
        var result: Long
        try {
            // insert transaction
            result = insertOne(dataTransaction)
            if (result.toInt() == 0) {
                throw RuntimeException("Failed to insert transaction")
            }
            // insert item transaction
            for (d in dataTransactionItem) {
                if (daoTransactionItem.insertTransactionItems(d).toInt() <= 0) {
                    throw RuntimeException("Failed to insert transaction item")
                }
            }
            // insert history stock
            for (d in dataHistoryStock) {
                if (daoHistoryStock.saveHistoryStock(d) <= 0) {
                    throw RuntimeException("Failed to insert history item")
                }
                // update stock
                daoProduct.updateStock(
                    id = d.pID,
                    stock = d.currentStock,
                    date = d.date.created
                )
            }
        } catch (e: Exception) {
            result = 0
        }

        return result
    }
}