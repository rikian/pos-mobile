package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.entities.EntityProduct

class ViewModelProduct: ViewModel() {
    var data: EntityProduct = EntityProduct(
        image= "",
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
