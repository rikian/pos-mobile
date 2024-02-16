package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gulali.dein.models.dto.contract.DtoTotalPriceAndItem
import com.gulali.dein.models.entities.EntityCart

@Dao
interface DaoCart {
    @Query("SELECT * FROM carts WHERE transactionID=:transactionID ORDER BY createdAt DESC")
    fun getProducts(transactionID: String): MutableList<EntityCart>

    @Query("SELECT * FROM carts WHERE transactionID=:transactionID AND productID=:id")
    fun getProductById(transactionID: String, id: Int): EntityCart?

    @Update
    fun updateProduct(data: EntityCart): Int

    @Insert
    fun pushItem(data: EntityCart): Long

    @Query("DELETE FROM carts")
    fun truncateDaoCart()

    @Query("SELECT COUNT(productID) as itemCount, SUM(totalAfterDiscount) as totalAfterDiscount FROM carts WHERE transactionID=:transactionID")
    fun getTotalPriceAndItem(transactionID: String): DtoTotalPriceAndItem

    @Delete
    fun deleteProductInCart(data: EntityCart): Int
}