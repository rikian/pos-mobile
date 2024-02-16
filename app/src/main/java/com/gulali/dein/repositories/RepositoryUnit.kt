package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoUnit
import com.gulali.dein.models.dto.dbresult.DtoResultInsertUnit
import com.gulali.dein.models.entities.EntityUnit

class RepositoryUnit(private val daoUnit: DaoUnit) {
    private var duplicateConstraint = "UNIQUE constraint failed: units.name (code 2067 SQLITE_CONSTRAINT_UNIQUE[2067])"

    fun getUnits(): List<EntityUnit> {
        return daoUnit.getUnits()
    }

    fun insertUnit(data: EntityUnit): DtoResultInsertUnit {
        return try {
            DtoResultInsertUnit(daoUnit.insertUnit(data), null)
        } catch (e: Exception) {
            DtoResultInsertUnit(0, e)
        }
    }
}