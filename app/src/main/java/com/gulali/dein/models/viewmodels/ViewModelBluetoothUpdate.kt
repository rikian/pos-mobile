package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.dto.DateTime
import com.gulali.dein.models.entities.EntityOwner

class ViewModelBluetoothUpdate: ViewModel() {
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

    var pairedChooser = ""
}