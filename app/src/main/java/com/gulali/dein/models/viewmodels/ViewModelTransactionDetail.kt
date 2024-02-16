package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.dto.DtoPercentNominal
import com.gulali.dein.models.dto.DtoProduct
import com.gulali.dein.models.dto.DtoTransaction
import com.gulali.dein.models.dto.DtoTransactionItem
import com.gulali.dein.models.entities.EntityOwner
import com.gulali.dein.models.entities.EntityTransaction
import com.gulali.dein.models.entities.EntityTransactionItem

class ViewModelTransactionDetail: ViewModel() {
    var idTransaction = ""
    var owner: EntityOwner = EntityOwner(
        id= "",
        shop= "",
        owner= "",
        address= "",
        phone= "",
        discountPercent= 0.0,
        discountNominal= 0,
        taxPercent= 0.0,
        taxNominal= 0,
        adm= 0,
        bluetoothPaired= "",
        date= DateTime(
            created = 0,
            updated = 0
        ),
    )
    var transaction = EntityTransaction(
        id= "",
        edited= false,
        dataTransaction= DtoTransaction(
            totalItem= 0,
            subTotalProduct= 0,
            discountNominal= 0,
            discountPercent= 0.0,
            taxNominal= 0,
            taxPercent= 0.0,
            adm= 0,
            cash= 0,
            returned = 0,
            grandTotal= 0,
        ),
        date= DateTime(
            created = 0,
            updated = 0
        ),
    )
    var transactionItem: MutableList<EntityTransactionItem> = mutableListOf()
}