package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.entities.EntityProduct

class ViewModelProductUpdate: ViewModel() {
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
}