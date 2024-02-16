package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoOwner
import com.gulali.dein.models.entities.EntityOwner

class RepositoryOwner(private val daoOwner: DaoOwner) {
    fun createOwner(data: EntityOwner): Long {
        return try {
            daoOwner.createOwner(data)
        } catch (e: Exception) {
            0
        }
    }

    fun updateOwner(data: EntityOwner) {
        return daoOwner.updateOwner(data)
    }

    fun getOwner(): List<EntityOwner>? {
        return try {
            daoOwner.getOwner()
        } catch (e: Exception) {
            null
        }
    }
}