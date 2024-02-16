package com.gulali.dein.models.dto

data class DtoTransaction (
    var totalItem: Int,
    var subTotalProduct: Int,
    var discountNominal: Int,
    var discountPercent: Double,
    var taxNominal: Int,
    var taxPercent: Double,
    var adm: Int,
    var cash: Int,
    var returned: Int,
    var grandTotal: Int = 0,
)