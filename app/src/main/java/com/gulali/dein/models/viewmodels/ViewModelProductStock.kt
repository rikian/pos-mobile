package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.entities.EntityHistoryStock

class ViewModelProductStock: ViewModel() {
    var isUpdate = false
    var productID = 0
    var dataProduct: DtoProduct = DtoProduct(
        id= 0,
        img= "default.jpeg",
        barcode= "",
        name= "",
        category= "",
        stock= 0,
        unit= "",
        price= 0,
        purchase= 0,
        info= "",
        created= 0,
        updated= 0
    )
    var entityHistoryStock = EntityHistoryStock(
        pID= 0,
        inStock= 0,
        outStock= 0,
        currentStock= 0,
        purchase= 0,
        transactionID= "",
        info= "",
        date= DateTime(
            created = 0,
            updated = 0
        )
    )
}