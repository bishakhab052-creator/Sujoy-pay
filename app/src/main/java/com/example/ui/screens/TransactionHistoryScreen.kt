package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.ui.components.TransactionReceiptSheet
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.LocalSujoyColors
import com.example.util.Formatters

@Composable
fun TransactionHistoryScreen(
    transactions: List<TransactionEntity>,
    onSubmitDispute: (TransactionEntity, String, String, Boolean) -> Unit
) {
    val sujoy = LocalSujoyColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedTxn by remember { mutableStateOf<TransactionEntity?>(null) }

    val filters = listOf("ALL", "SUCCESS", "PENDING", "FAILED", "CREDIT", "DEBIT")

    val filtered = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { txn ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "SUCCESS", "PENDING", "FAILED" -> txn.status == selectedFilter
                "CREDIT", "DEBIT" -> txn.direction == selectedFilter
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                txn.counterpartyName.lowercase().contains(q) ||
                txn.counterpartyHandle.lowercase().contains(q) ||
                txn.utrNumber.lowercase().contains(q) ||
                txn.type.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    val totalCredits = remember(transactions) {
        transactions.filter { it.status == "SUCCESS" && it.direction == "CREDIT" }.sumOf { it.amountPaise }
    }
    val totalDebits = remember(transactions) {
        transactions.filter { it.status == "SUCCESS" && it.direction == "DEBIT" }.sumOf { it.amountPaise }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("Transaction Ledger", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = sujoy.successContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Credited", style = MaterialTheme.typography.labelSmall, color = sujoy.success)
                    Text(Formatters.formatInrCompact(totalCredits), style = MaterialTheme.typography.titleMedium, color = sujoy.success, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(Formatters.formatInrCompact(totalDebits), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search UTR, name, or UPI ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f ->
                FilterChip(
                    selected = selectedFilter == f,
                    onClick = { selectedFilter = f },
                    label = { Text(f) },
                    modifier = Modifier.testTag("history_filter_${f.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No matching transactions", style = MaterialTheme.typography.titleSmall)
                    Text("Try clearing your search or status filter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { txn ->
                    TransactionRowCard(
                        txn = txn,
                        onClick = { selectedTxn = txn }
                    )
                }
            }
        }
    }

    selectedTxn?.let { txn ->
        TransactionReceiptSheet(
            txn = txn,
            onDismiss = { selectedTxn = null },
            onSubmitDispute = { cat, desc, refund ->
                onSubmitDispute(txn, cat, desc, refund)
            }
        )
    }
}
