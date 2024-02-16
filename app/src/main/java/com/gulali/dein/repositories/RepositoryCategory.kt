package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoCategory
import com.gulali.dein.models.dto.dbresult.DtoResultCategory
import com.gulali.dein.models.entities.EntityCategory

class RepositoryCategory(private val daoCategory: DaoCategory) {

    fun getCategories(): List<EntityCategory> {
        return daoCategory.getCategories()
    }

    fun insertCategory(data: EntityCategory): DtoResultCategory {
        return try {
            DtoResultCategory(daoCategory.insertCategory(data), null)
        } catch (e: Exception) {
            DtoResultCategory(0, e)
        }
    }
}