package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoTransactionItem
import com.gulali.dein.models.dto.DtoTransactionItem
import com.gulali.dein.models.entities.EntityTransactionItem

class RepositoryTransactionItem(private val daoTransactionItem: DaoTransactionItem) {

    fun getTransactionItemById(transactionID: String): MutableList<EntityTransactionItem> {
        return daoTransactionItem.getTransactionItemById(transactionID)
    }
}