package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.entities.EntityHistoryStock

class ViewModelStockHistory: ViewModel() {
    var productID = 0
    var stockHistory: MutableList<EntityHistoryStock> = mutableListOf()
}