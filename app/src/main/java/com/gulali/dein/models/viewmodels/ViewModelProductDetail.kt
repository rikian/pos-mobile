package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.dto.DtoProduct

class ViewModelProductDetail: ViewModel() {
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
}