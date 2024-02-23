package com.gulali.dein.models.dto

import com.gulali.dein.models.entities.EntityTransaction

data class DtoTransactionWithCount(
    var count: DtoCountAndSum,
    var list: MutableList<EntityTransaction>
)
