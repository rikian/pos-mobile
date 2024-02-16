package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoHistoryStock
import com.gulali.dein.models.entities.EntityHistoryStock

class RepositoryHistoryStock(private val daoHistoryStock: DaoHistoryStock) {
    fun getHistoryStockById(id: Int): List<EntityHistoryStock> {
        return daoHistoryStock.getHistoryStockById(id)
    }
}