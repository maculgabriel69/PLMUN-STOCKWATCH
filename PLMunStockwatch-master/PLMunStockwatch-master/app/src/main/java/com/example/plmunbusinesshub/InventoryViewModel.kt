package com.example.plmunbusinesshub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface InventoryState {
    data object Loading : InventoryState
    data class Ready(val items: List<Product>) : InventoryState
    data class Error(val message: String) : InventoryState
}

class InventoryViewModel(
    private val repo: ProductRepository = ProductRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    val query = MutableStateFlow("")

    val state: StateFlow<InventoryState> =
        combine(
            repo.observeProducts()
                .map<List<Product>, Result<List<Product>>> { Result.success(it) }
                .catch { emit(Result.failure(it)) },
            query
        ) { result, q ->
            result.fold(
                onSuccess = { list ->
                    val needle = q.trim()
                    InventoryState.Ready(
                        if (needle.isEmpty()) list
                        else list.filter {
                            it.name.contains(needle, ignoreCase = true) ||
                                    it.category.contains(needle, ignoreCase = true)
                        }
                    )
                },
                onFailure = { InventoryState.Error(it.message ?: "Could not load products") }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryState.Loading)

    var isAdmin by mutableStateOf(true)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    private val allowedAdmins = listOf(
        "juegojairon23@gmail.com",
        "juegojairon@plmun.edu.ph",
        "juegojairon_bsit@plmun.edu.ph",
        "admin4@plmun.edu.ph",
        "admin5@plmun.edu.ph"
    )

    fun signIn(email: String, password: String) {
        val cleanEmail = email.trim().lowercase()

        if (cleanEmail.isBlank() || password.isBlank()) {
            authError = "Please enter email and password."
            return
        }

        if (!allowedAdmins.contains(cleanEmail)) {
            authError = "Ang email na ito ay walang karapatang mag-admin."
            return
        }

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(cleanEmail, password).await()
                isAdmin = true
                authError = null
            } catch (e: Exception) {
                authError = e.message ?: "Sign-in failed"
            }
        }
    }

    fun clearAuthError() { authError = null }

    fun signOut() {
        auth.signOut()
        isAdmin = false
    }

    fun save(product: Product, oldPrice: Double?) {
        viewModelScope.launch {
            try {
                repo.save(product, oldPrice)
            } catch (e: Exception) {
                message = e.message ?: "Failed to save product"
            }
        }
    }

    fun adjustStock(id: String, delta: Long) {
        viewModelScope.launch {
            try {
                repo.adjustStock(id, delta)
            } catch (e: Exception) {
                message = e.message ?: "Failed to adjust stock"
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repo.delete(id)
                message = "Variant deleted"
            } catch (e: Exception) {
                message = e.message ?: "Failed to delete product"
            }
        }
    }

    fun deleteGroup(variants: List<Product>) {
        viewModelScope.launch {
            try {
                repo.deleteGroup(variants)
                message = "Product card deleted successfully"
            } catch (e: Exception) {
                message = e.message ?: "Failed to delete product group"
            }
        }
    }

    fun observePriceHistory(id: String) = repo.observePriceHistory(id)
}