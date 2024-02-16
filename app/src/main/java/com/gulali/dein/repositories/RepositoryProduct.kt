package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoHistoryStock
import com.gulali.dein.contracts.dao.DaoProduct
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityProduct

class RepositoryProduct(private val daoProduct: DaoProduct, private val daoHistoryStock: DaoHistoryStock) {
    fun insertProduct(data: EntityProduct, dataHistoryStock: EntityHistoryStock): Long {
        return try {
            val idProduct = daoProduct.saveProduct(data, dataHistoryStock, daoHistoryStock)
            if (this.getProductByID(idProduct.toInt()) == null) {
                0
            } else {
                idProduct
            }
        } catch (e: Exception) {
            0
        }
    }

    fun getProductByID(id: Int): DtoProduct? {
        return daoProduct.getProductByID(id)
    }

    fun getProductByName(name: String): MutableList<DtoProduct> {
        return daoProduct.getProductByName(name)
    }

    fun getProducts(): MutableList<DtoProduct> {
        return daoProduct.getProducts()
    }

    fun updateProduct(data: EntityProduct): Boolean {
        return try {
            daoProduct.updateProduct(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateProductStock(dataHistoryStock: EntityHistoryStock): Boolean {
        return try {
            daoProduct.updateProductStock(dataHistoryStock, daoHistoryStock)
            true
        } catch (e: Exception) {
            false
        }
    }
}