package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.helper.Helper
import com.gulali.dein.models.dto.DtoTransactionFilter
import com.gulali.dein.models.entities.EntityOwner

class VIewModelHome: ViewModel() {
    var owner: List<EntityOwner> = mutableListOf()
    var isFromTransactionFilter = false
    var transactionTotalPage = 0
    var transactionFilter = DtoTransactionFilter(
        totalStart= 0,
        totalEnd= 0,
        dateStart= 0L,
        dateEnd= 0L,
        index= 0,
        limit= 10,
    )
}