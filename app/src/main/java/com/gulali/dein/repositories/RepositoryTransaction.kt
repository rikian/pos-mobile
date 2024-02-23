package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoHistoryStock
import com.gulali.dein.contracts.dao.DaoProduct
import com.gulali.dein.contracts.dao.DaoTransaction
import com.gulali.dein.contracts.dao.DaoTransactionItem
import com.gulali.dein.models.dto.DtoTransactionFilter
import com.gulali.dein.models.dto.DtoTransactionWithCount
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.entities.EntityTransactionItem

class RepositoryTransaction(private val daoTransaction: DaoTransaction, private val daoHistoryStock: DaoHistoryStock, private val daoTransactionItem: DaoTransactionItem, private val daoProduct: DaoProduct) {
    fun saveTransaction(dataTransaction: EntityTransaction, dataTransactionItem: List<EntityTransactionItem>, dataHistoryStock: List<EntityHistoryStock>): Long {
        return try {
            daoTransaction.saveTransaction(
                dataTransaction= dataTransaction,
                dataTransactionItem= dataTransactionItem,
                dataHistoryStock= dataHistoryStock,
                daoHistoryStock=daoHistoryStock,
                daoTransactionItem= daoTransactionItem,
                daoProduct= daoProduct
            )
        } catch (e: Exception) {
            0
        }
    }

    fun getTransaction(index: Int, limit: Int): MutableList<EntityTransaction> {
        return daoTransaction.getTransaction(index, limit)
    }

    fun getTransactionByID(transactionID: String): EntityTransaction {
        return daoTransaction.getTransactionByID(transactionID)
    }

    fun getDataTransactionWithCountAndSum(index: Int, limit: Int): DtoTransactionWithCount {
        return daoTransaction.getDataTransactionWithCountAndSum(index, limit)
    }

    fun getDataTransactionFilterWithCountAndSum(p: DtoTransactionFilter): DtoTransactionWithCount {
        return daoTransaction.getDataTransactionFilterWithCountAndSum(p)
    }
}