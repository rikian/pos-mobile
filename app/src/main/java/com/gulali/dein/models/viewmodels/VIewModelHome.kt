package com.gulali.dein.models.viewmodels

import androidx.lifecycle.ViewModel
import com.gulali.dein.models.entities.EntityOwner

class VIewModelHome: ViewModel() {
    var owner: List<EntityOwner> = mutableListOf()
}