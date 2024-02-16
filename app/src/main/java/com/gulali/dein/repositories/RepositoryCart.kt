package com.gulali.dein.repositories

import com.gulali.dein.contracts.dao.DaoCart
import com.gulali.dein.models.dto.contract.DtoTotalPriceAndItem
import com.gulali.dein.models.entities.EntityCart

class RepositoryCart(private val daoCart: DaoCart) {
    fun getProducts(transactionID: String): MutableList<EntityCart> {
        return daoCart.getProducts(transactionID)
    }

    fun updateProduct(data: EntityCart): Int {
        return try {
            daoCart.updateProduct(data)
        } catch (e: Exception) {
            0
        }
    }

    fun getProductById(transactionID: String, id: Int): EntityCart? {
        return try {
            daoCart.getProductById(transactionID, id)
        } catch (e: Exception) {
            null
        }
    }

    fun getTotalPriceAndItem(transactionID: String): DtoTotalPriceAndItem? {
        return try {
            daoCart.getTotalPriceAndItem(transactionID)
        } catch (e: Exception) {
            null
        }
    }

    fun pushItem(data: EntityCart): Long {
        return try {
            daoCart.pushItem(data)
        } catch (e: Exception) {
            0
        }
    }

    fun deleteProductInCart(data: EntityCart): Int {
        return try {
            daoCart.deleteProductInCart(data)
        } catch (e: Exception) {
            0
        }
    }

    fun truncateDaoCart(): Boolean {
        return try {
            daoCart.truncateDaoCart()
            true
        } catch (e: Exception) {
            false
        }
    }
}