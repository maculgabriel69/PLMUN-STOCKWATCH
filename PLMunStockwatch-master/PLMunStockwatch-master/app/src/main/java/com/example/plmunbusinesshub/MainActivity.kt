package com.example.plmunbusinesshub

import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { HubApp() }
            }
        }
    }
}

private val peso: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
private val LowStockColor = Color(0xFFB26A00)
private val StandardSizes = listOf("XS", "S", "M", "L", "XL", "2XL")

private fun getFullSizeName(code: String): String {
    return when (code.uppercase()) {
        "XS" -> "EXTRA SMALL"
        "S" -> "SMALL"
        "M" -> "MEDIUM"
        "L" -> "LARGE"
        "XL" -> "EXTRA LARGE"
        "2XL", "XXL" -> "DOUBLE EXTRA LARGE"
        else -> code.uppercase()
    }
}

private fun parseProductInfo(fullName: String): Pair<String, String> {
    val trimmed = fullName.trim()
    val known = listOf("XS", "S", "M", "L", "XL", "2XL", "XXL", "3XL")
    val detectedSize = known.firstOrNull { size ->
        trimmed.equals(size, ignoreCase = true) || trimmed.endsWith(" $size", ignoreCase = true)
    } ?: ""

    val cleanName = if (detectedSize.isNotEmpty() && !trimmed.equals(detectedSize, ignoreCase = true)) {
        trimmed.substringBeforeLast(detectedSize).trim()
    } else if (detectedSize.isNotEmpty()) {
        "Product"
    } else {
        trimmed
    }

    val normalizedSize = if (detectedSize.equals("XXL", ignoreCase = true)) "2XL" else detectedSize

    return cleanName to normalizedSize
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubApp(vm: InventoryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showLogin by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var historyFor by remember { mutableStateOf<Product?>(null) }
    var deletingVariant by remember { mutableStateOf<Product?>(null) }
    var deletingCardGroup by remember { mutableStateOf<List<Product>?>(null) }

    var selectedCategory by remember { mutableStateOf("All") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }
    LaunchedEffect(vm.isAdmin) { if (vm.isAdmin) showLogin = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.plmun_logo),
                            contentDescription = "PLMun Logo",
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                        Column {
                            Text("PLMun StockWatch", fontWeight = FontWeight.Bold)
                            Text(
                                "● LIVE Plmun Business Center Hub · prices and stock update automatically",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                },
                actions = {
                    if (vm.isAdmin) {
                        IconButton(onClick = vm::signOut) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                        }
                    } else {
                        IconButton(onClick = { showLogin = true }) {
                            Icon(Icons.Default.Lock, contentDescription = "Admin sign in")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (vm.isAdmin) {
                FloatingActionButton(onClick = { editing = null; editorOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add product")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.plmun_bg),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA0A0A12))
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    label = { Text("Search merchandise or category") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.95f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                when (val s = state) {
                    is InventoryState.Loading -> CenteredBox { CircularProgressIndicator() }
                    is InventoryState.Error -> CenteredBox {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    }
                    is InventoryState.Ready -> {
                        val categories = listOf("All") + s.items.map { it.category.uppercase() }.distinct()

                        val filteredItems = s.items.filter { product ->
                            val matchesCategory = selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true)
                            val matchesLowStock = !showOnlyLowStock || (product.isLowStock || product.isOutOfStock)
                            matchesCategory && matchesLowStock
                        }

                        val groupedProducts = filteredItems.groupBy { product ->
                            val (cleanName, _) = parseProductInfo(product.name)
                            "${cleanName.lowercase()}_${product.category.lowercase()}"
                        }.values.toList()

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Black.copy(alpha = 0.6f),
                                        labelColor = Color.White,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            if (vm.isAdmin) {
                                item {
                                    FilterChip(
                                        selected = showOnlyLowStock,
                                        onClick = { showOnlyLowStock = !showOnlyLowStock },
                                        label = { Text("⚠️ Low Stock Only") },
                                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LowStockColor,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        if (groupedProducts.isEmpty()) {
                            CenteredBox { Text("No matching products found.", color = Color.White) }
                        } else {
                            Text(
                                "${groupedProducts.size} products listed · ${s.items.count { it.isLowStock || it.isOutOfStock }} low/out of stock items",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 88.dp)
                            ) {
                                items(groupedProducts, key = { group -> group.first().id }) { group ->
                                    ECommerceProductCard(
                                        variants = group,
                                        isAdmin = vm.isAdmin,
                                        onHistory = { product -> historyFor = product },
                                        onEdit = { product -> editing = product; editorOpen = true },
                                        onDeleteVariant = { product -> deletingVariant = product },
                                        onDeleteCard = { groupVariants -> deletingCardGroup = groupVariants },
                                        onAdjust = { id, delta -> vm.adjustStock(id, delta) },
                                        onAddNewVariant = { baseName, category, size ->
                                            editing = Product(id = "", name = baseName, category = category, price = group.first().price, stock = 0)
                                            editorOpen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogin) {
        LoginDialog(
            error = vm.authError,
            onDismiss = { showLogin = false; vm.clearAuthError() },
            onSignIn = vm::signIn
        )
    }
    if (editorOpen) {
        ProductDialog(
            initial = editing,
            onDismiss = { editorOpen = false },
            onSave = { product ->
                vm.save(product, oldPrice = editing?.price)
                editorOpen = false
            }
        )
    }
    historyFor?.let { p ->
        PriceHistoryDialog(vm, p) { historyFor = null }
    }
    deletingVariant?.let { p ->
        AlertDialog(
            onDismissRequest = { deletingVariant = null },
            title = { Text("Delete variant ${p.name}?") },
            text = { Text("This removes only this specific size variant.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(p.id); deletingVariant = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deletingVariant = null }) { Text("Cancel") } }
        )
    }
    deletingCardGroup?.let { group ->
        val sample = group.first()
        val (cleanTitle, _) = parseProductInfo(sample.name)
        AlertDialog(
            onDismissRequest = { deletingCardGroup = null },
            title = { Text("Delete ENTIRE product: $cleanTitle?") },
            text = { Text("This will permanently remove ALL sizes and variants of this card from Firestore.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteGroup(group)
                    deletingCardGroup = null
                }) { Text("DELETE CARD", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingCardGroup = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
fun ECommerceProductCard(
    variants: List<Product>,
    isAdmin: Boolean,
    onHistory: (Product) -> Unit,
    onEdit: (Product) -> Unit,
    onDeleteVariant: (Product) -> Unit,
    onDeleteCard: (List<Product>) -> Unit,
    onAdjust: (String, Long) -> Unit,
    onAddNewVariant: (String, String, String) -> Unit
) {
    val sampleProduct = variants.first()
    val (cleanTitle, _) = parseProductInfo(sampleProduct.name)
    val categoryUpper = sampleProduct.category.uppercase()
    val isApparel = categoryUpper.contains("UNIFORM") || categoryUpper.contains("SPORTS") || categoryUpper.contains("MAN")

    var selectedSizeCode by remember(variants) {
        if (isApparel) {
            val firstExistingSize = variants.map { parseProductInfo(it.name).second }.firstOrNull { it.isNotEmpty() }
            mutableStateOf(firstExistingSize ?: "M")
        } else {
            mutableStateOf("")
        }
    }

    val currentVariant = if (isApparel) {
        variants.firstOrNull { parseProductInfo(it.name).second.equals(selectedSizeCode, ignoreCase = true) }
    } else {
        variants.firstOrNull()
    }

    val (stockText, stockColor) = when {
        currentVariant == null -> "Availability: Out of stock (To be announced)" to MaterialTheme.colorScheme.error
        currentVariant.isOutOfStock -> "Availability: Out of stock" to MaterialTheme.colorScheme.error
        currentVariant.isLowStock -> "Availability: Low stock (${currentVariant.stock})" to LowStockColor
        else -> "Availability: In stock (${currentVariant.stock})" to Color(0xFF2E7D32)
    }

    val activePrice = currentVariant?.price ?: sampleProduct.price

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = categoryUpper,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = cleanTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = peso.format(activePrice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            if (isApparel) {
                Text(
                    text = "Size: $selectedSizeCode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(StandardSizes) { size ->
                        val isSelected = size.equals(selectedSizeCode, ignoreCase = true)
                        val existsInDatabase = variants.any { parseProductInfo(it.name).second.equals(size, ignoreCase = true) }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = 48.dp, height = 36.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.Black else if (existsInDatabase) Color.LightGray else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .background(
                                    color = if (isSelected) Color.Black else Color.White,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { selectedSizeCode = size }
                        ) {
                            Text(
                                text = size,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else if (existsInDatabase) Color.Black else Color.Gray,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            if (currentVariant == null || currentVariant.isOutOfStock) {
                Button(
                    onClick = {
                        if (isAdmin && currentVariant == null && isApparel) {
                            onAddNewVariant(cleanTitle, sampleProduct.category, selectedSizeCode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFF424242),
                        containerColor = if (currentVariant == null) Color(0xFF1565C0) else Color.DarkGray
                    )
                ) {
                    val fullSizeLabel = getFullSizeName(selectedSizeCode)
                    Text(
                        if (currentVariant == null && isAdmin && isApparel) "ADD STOCK FOR $fullSizeLabel" else "SOLD OUT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("AVAILABLE IN BUSINESS CENTER", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "• $stockText",
                color = stockColor,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isApparel) selectedSizeCode else "Admin Controls",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        if (currentVariant != null) {
                            IconButton(onClick = { onHistory(currentVariant) }) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = Color.DarkGray)
                            }
                            IconButton(onClick = { onAdjust(currentVariant.id, -1) }, enabled = currentVariant.stock > 0) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.DarkGray)
                            }
                            IconButton(onClick = { onAdjust(currentVariant.id, 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.DarkGray)
                            }
                            IconButton(onClick = { onEdit(currentVariant) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Blue)
                            }
                            IconButton(onClick = { onDeleteVariant(currentVariant) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Variant", tint = Color.DarkGray)
                            }
                        }

                        IconButton(onClick = { onDeleteCard(variants) }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Entire Card", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginDialog(error: String?, onDismiss: () -> Unit, onSignIn: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin sign in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSignIn(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank()
            ) { Text("Sign in") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(initial: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    val initialRawName = initial?.name ?: ""
    val initialDetectedSize = StandardSizes.firstOrNull { size ->
        initialRawName.trim().endsWith(" $size", ignoreCase = true) || initialRawName.trim().equals(size, ignoreCase = true)
    } ?: ""

    val initialCleanedName = if (initialDetectedSize.isNotEmpty()) {
        initialRawName.trim().substringBeforeLast(initialDetectedSize).trim()
    } else {
        initialRawName
    }

    var name by remember(initial) { mutableStateOf(initialCleanedName) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "") }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "") }
    var threshold by remember(initial) { mutableStateOf((initial?.lowStockThreshold ?: 5).toString()) }

    var selectedSize by remember(initial) { mutableStateOf(initialDetectedSize.ifEmpty { "M" }) }

    val isApparel = category.contains("UNIFORM", ignoreCase = true) ||
            category.contains("SPORTS", ignoreCase = true) ||
            category.contains("MAN", ignoreCase = true) ||
            name.contains("UNIFORM", ignoreCase = true) ||
            category.isBlank()

    val priceValue = price.toDoubleOrNull()
    val stockValue = stock.toLongOrNull()
    val thresholdValue = threshold.toLongOrNull()
    val valid = name.isNotBlank() &&
            priceValue != null && priceValue >= 0 &&
            stockValue != null && stockValue >= 0 &&
            thresholdValue != null && thresholdValue >= 0

    val number = KeyboardOptions(keyboardType = KeyboardType.Number)
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null || initial.id.isBlank()) "Add product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. UNIFORM, SUPPLIES)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isApparel) {
                    Text(
                        text = "Select Size Variant:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StandardSizes) { size ->
                            FilterChip(
                                selected = selectedSize.equals(size, ignoreCase = true),
                                onClick = {
                                    selectedSize = size
                                },
                                label = { Text(size) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.LightGray.copy(alpha = 0.3f),
                                    labelColor = Color.DarkGray,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (PHP)") },
                    singleLine = true,
                    keyboardOptions = decimal,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock") },
                    singleLine = true,
                    keyboardOptions = number,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Low-stock alert at") },
                    singleLine = true,
                    keyboardOptions = number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    // Dito natin sinisiguradong pure name lang ang ise-save (walang idinidikit na size sa dulo)
                    val finalName = name.trim()

                    onSave(
                        Product(
                            id = initial?.id ?: "",
                            name = finalName,
                            category = category.ifBlank { "UNIFORM" },
                            price = priceValue ?: 0.0,
                            stock = stockValue ?: 0,
                            lowStockThreshold = thresholdValue ?: 5
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PriceHistoryDialog(vm: InventoryViewModel, product: Product, onDismiss: () -> Unit) {
    val history by remember(product.id) { vm.observePriceHistory(product.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${product.name}: price history") },
        text = {
            if (history.isEmpty()) {
                Text("No price changes recorded yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(history, key = { it.id }) { h ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(peso.format(h.price), fontWeight = FontWeight.Medium)
                            Text(
                                h.changedAt?.let { DateUtils.getRelativeTimeSpanString(it.time).toString() } ?: "just now",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}