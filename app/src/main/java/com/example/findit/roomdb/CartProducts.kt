package com.example.findit.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "CartProducts")
data class CartProducts(

    @PrimaryKey
    val productId: String = "random",

    val productTitle: String ? = null,
    val productQuantity: String ,
    val productPrice: String ,
    var productCount: Int ? = null,
    var productStock: Int ? = null,
    var productImage: String ? = null,
    var productCategory: String ? = null,
    var adminUid: String? = null,
)

