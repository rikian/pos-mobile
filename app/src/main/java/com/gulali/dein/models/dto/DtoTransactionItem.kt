package com.gulali.dein.models.dto

data class DtoTransactionItem(
    var productID: Int,
    var name: String,
    var quantity: Int = 1,
    var price: Int,
    var discountPercent: Double = 0.0,
    var discountNominal: Int,
    var totalBeforeDiscount: Int = 0,
    var totalAfterDiscount: Int = 0,
)
