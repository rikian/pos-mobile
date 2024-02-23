package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.gulali.dein.models.dto.DtoCountAndSum
import com.gulali.dein.models.dto.DtoTransactionFilter
import com.gulali.dein.models.dto.DtoTransactionWithCount
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.entities.EntityTransactionItem

@Dao
interface DaoTransaction {
    @Insert
    fun insertOne(data: EntityTransaction): Long

    @Query("SELECT COUNT(grandTotal) AS count, SUM(grandTotal) AS sum FROM `transactions`")
    fun getDataCountAndSum(): DtoCountAndSum

    @Query("SELECT * FROM `transactions` ORDER BY created DESC LIMIT :limit OFFSET :index * :limit")
    fun getTransaction(index: Int, limit: Int): MutableList<EntityTransaction>

    @Query("SELECT * FROM `transactions` WHERE id= :transactionID")
    fun getTransactionByID(transactionID: String): EntityTransaction

    @Query("SELECT COUNT(*) as count, SUM(grandTotal) as sum FROM `transactions` WHERE grandTotal >= :totalStart AND grandTotal <= :totalEnd AND created >= :dateStart AND created <= :dateEnd")
    fun getDataFilterCountAndSum(
        totalStart: Int,
        totalEnd: Int,
        dateStart: Long,
        dateEnd: Long
    ): DtoCountAndSum

    @Query("SELECT * FROM `transactions` WHERE grandTotal >= :totalStart AND grandTotal <= :totalEnd AND created >= :dateStart AND created <= :dateEnd ORDER BY grandTotal ASC LIMIT :limit OFFSET :index * :limit")
    fun getDataFilter(
        totalStart: Int,
        totalEnd: Int,
        dateStart: Long,
        dateEnd: Long,
        index: Int,
        limit: Int
    ): MutableList<EntityTransaction>

    @Transaction
    fun getDataTransactionWithCountAndSum(index: Int, limit: Int): DtoTransactionWithCount {
        val count = getDataCountAndSum()
        val transactions = getTransaction(index, limit)
        return DtoTransactionWithCount(count, transactions)
    }

    @Transaction
    fun getDataTransactionFilterWithCountAndSum(p: DtoTransactionFilter): DtoTransactionWithCount {
        val count = getDataFilterCountAndSum(
            p.totalStart,
            p.totalEnd,
            p.dateStart,
            p.dateEnd
        )
        val listFilter = getDataFilter(
            p.totalStart,
            p.totalEnd,
            p.dateStart,
            p.dateEnd,
            p.index,
            p.limit,
        )
        return DtoTransactionWithCount(count, listFilter)
    }

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