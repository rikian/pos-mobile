package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.entities.EntityHistoryStock
import com.gulali.dein.models.entities.EntityProduct

class ViewModelProductAdd: ViewModel() {
    var idProduct: Int = 0
    var entityProduct: EntityProduct = EntityProduct(
        image= "default.jpeg",
        name= "",
        category= 0,
        barcode= "",
        stock= 0,
        unit= 0,
        purchase= 0,
        price= 0,
        info= "",
        date= DateTime(
            created = 0,
            updated = 0
        )
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