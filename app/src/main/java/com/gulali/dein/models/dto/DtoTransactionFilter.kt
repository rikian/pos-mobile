package com.gulali.dein.models.dto

data class DtoTransactionFilter(
    var totalStart: Int,
    var totalEnd: Int,
    var dateStart: Long,
    var dateEnd: Long,
    var index: Int,
    var limit: Int
)
