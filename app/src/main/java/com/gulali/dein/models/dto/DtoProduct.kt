package com.gulali.dein.models.dto

data class DtoProduct(
    val id: Int,
    var img: String,
    var barcode: String,
    var name: String,
    var category: String,
    var stock: Int,
    var unit: String,
    var price: Int,
    var purchase: Int,
    val info: String,
    var created: Long,
    var updated: Long
)