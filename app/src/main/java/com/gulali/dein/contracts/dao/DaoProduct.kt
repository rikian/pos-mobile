package com.gulali.dein.contracts.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityProduct

@Dao
interface DaoProduct {
    private companion object {
        const val QUERY_GET_PRODUCT = """
            SELECT 
                a.id AS id,
                a.image AS img, 
                a.barcode AS barcode, 
                a.name AS name,
                a.stock AS stock,
                a.price AS price, 
                a.purchase AS purchase, 
                a.info AS info,
                a.created AS created, 
                a.updated AS updated, 
                b.name AS unit,
                c.name AS category 
            FROM 
                products AS a 
            INNER JOIN 
                units AS b ON a.unit = b.id 
            INNER JOIN 
                categories AS c ON a.category = c.id
        """
    }

    @Query("$QUERY_GET_PRODUCT ORDER BY updated DESC LIMIT 10")
    fun getProducts(): MutableList<DtoProduct>

    @Query("$QUERY_GET_PRODUCT WHERE a.name LIKE '%' || :name || '%' OR a.barcode LIKE '%' || :name || '%' ORDER BY updated DESC LIMIT 10")
    fun getProductByName(name: String): MutableList<DtoProduct>

    @Query("$QUERY_GET_PRODUCT WHERE a.id = :id")
    fun getProductByID(id: Int): DtoProduct?

    @Query("$QUERY_GET_PRODUCT WHERE a.barcode = :barcode")
    fun getProductByBarcode(barcode: String): DtoProduct?

    @Insert
    fun insertProduct(data: EntityProduct): Long

    @Update
    fun updateProduct(data: EntityProduct)

    @Query("UPDATE products SET stock=:stock, updated=:date WHERE id=:id")
    fun updateStock(id: Int, stock: Int, date: Long)

    @Transaction
    fun updateProductStock(dataHistoryStock: EntityHistoryStock, daoHistoryStock: DaoHistoryStock) {
        updateStock(dataHistoryStock.pID, dataHistoryStock.currentStock, dataHistoryStock.date.created)
        daoHistoryStock.saveHistoryStock(dataHistoryStock)
    }

    @Transaction
    fun saveProduct(dataProduct: EntityProduct, dataHistoryStock: EntityHistoryStock, daoHistoryStock: DaoHistoryStock): Long {
        val result = insertProduct(dataProduct)
        dataHistoryStock.pID = result.toInt()
        daoHistoryStock.saveHistoryStock(dataHistoryStock)
        return result
    }
}