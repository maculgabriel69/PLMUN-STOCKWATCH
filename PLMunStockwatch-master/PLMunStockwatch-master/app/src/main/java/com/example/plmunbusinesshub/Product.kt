package com.example.plmunbusinesshub

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val stock: Long = 0,
    val lowStockThreshold: Long = 5,
    @ServerTimestamp val updatedAt: Date? = null
) {
    val isOutOfStock get() = stock <= 0
    val isLowStock get() = stock in 1..lowStockThreshold
}

data class PriceChange(
    @DocumentId val id: String = "",
    val price: Double = 0.0,
    @ServerTimestamp val changedAt: Date? = null
)