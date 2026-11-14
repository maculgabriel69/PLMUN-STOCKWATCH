package com.example.plmunbusinesshub

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val subscription = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val products = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Product::class.java)?.copy(id = doc.id)
                        }
                        trySend(products)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun save(product: Product, oldPrice: Double? = null) {
        val docRef = if (product.id.isEmpty()) {
            db.collection("products").document()
        } else {
            db.collection("products").document(product.id)
        }

        val productData = hashMapOf(
            "name" to product.name,
            "category" to product.category,
            "price" to product.price,
            "stock" to product.stock,
            "lowStockThreshold" to product.lowStockThreshold,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(productData).await()

        if (oldPrice != null && oldPrice != product.price) {
            val priceData = hashMapOf(
                "price" to product.price,
                "changedAt" to FieldValue.serverTimestamp()
            )
            docRef.collection("priceHistory").add(priceData).await()
        }
    }

    suspend fun adjustStock(productId: String, delta: Long) {
        if (productId.isEmpty()) return
        val docRef = db.collection("products").document(productId)

        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStock = snapshot.getLong("stock") ?: 0L
                val newStock = (currentStock + delta).coerceAtLeast(0L)

                transaction.update(docRef, "stock", newStock)
                transaction.update(docRef, "updatedAt", FieldValue.serverTimestamp())
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun delete(productId: String) {
        if (productId.isNotEmpty()) {
            db.collection("products").document(productId).delete().await()
        }
    }

    suspend fun deleteGroup(variants: List<Product>) {
        for (p in variants) {
            if (p.id.isNotEmpty()) {
                db.collection("products").document(p.id).delete().await()
            }
        }
    }

    fun observePriceHistory(productId: String): Flow<List<PriceChange>> = callbackFlow {
        if (productId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("products")
            .document(productId)
            .collection("priceHistory")
            .orderBy("changedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val history = snapshot.toObjects(PriceChange::class.java)
                        trySend(history)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        awaitClose { subscription.remove() }
    }
}