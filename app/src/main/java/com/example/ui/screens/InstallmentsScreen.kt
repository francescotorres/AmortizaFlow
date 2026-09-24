package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InstallmentEntity
import com.example.ui.AmortizationViewModel
import com.example.ui.InstallmentFilter
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AnticipatedBlue
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingAmber

@Composable
fun InstallmentsScreen(
    viewModel: AmortizationViewModel,
    modifier: Modifier = Modifier
) {
    val filteredList by viewModel.filteredInstallments.collectAsStateWithLifecycle()
    val allList by viewModel.allInstallments.collectAsStateWithLifecycle()
    val currentFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    var editingInstallment by remember { mutableStateOf<InstallmentEntity?>(null) }

    val paidCount = allList.count { it.isPaid }
    val pendingCount = allList.count { !it.isPaid }
    val anticipatedCount = allList.count { it.isAnticipated }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Screen Title
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Cronograma de Parcelas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "60 parcelas • R$ 985,38 fixas nominais",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentFilter == InstallmentFilter.ALL,
                    onClick = { viewModel.setFilter(InstallmentFilter.ALL) },
                    label = { Text("Todas (${allList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == InstallmentFilter.PAID,
                    onClick = { viewModel.setFilter(InstallmentFilter.PAID) },
                    label = { Text("Pagas ($paidCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PaidGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PaidGreen
                    )
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == InstallmentFilter.PENDING,
                    onClick = { viewModel.setFilter(InstallmentFilter.PENDING) },
                    label = { Text("Pendentes ($pendingCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PendingAmber.copy(alpha = 0.2f),
                        selectedLabelColor = PendingAmber
                    )
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == InstallmentFilter.ANTICIPATED,
                    onClick = { viewModel.setFilter(InstallmentFilter.ANTICIPATED) },
                    label = { Text("Amortizadas ($anticipatedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AnticipatedBlue.copy(alpha = 0.2f),
                        selectedLabelColor = AnticipatedBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Installments List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = filteredList,
                key = { it.parcelNumber }
            ) { installment ->
                InstallmentItemCard(
                    installment = installment,
                    onTogglePaid = { viewModel.togglePaid(installment.parcelNumber) },
                    onEdit = { editingInstallment = installment }
                )
            }
        }
    }

    // Edit Installment Modal
    editingInstallment?.let { inst ->
        EditInstallmentDialog(
            installment = inst,
            onDismiss = { editingInstallment = null },
            onConfirm = { actualPaid, date, notes ->
                viewModel.updateInstallmentDetails(
                    parcelNumber = inst.parcelNumber,
                    actualPaid = actualPaid,
                    paymentDate = date,
                    notes = notes
                )
                editingInstallment = null
            }
        )
    }
}

@Composable
fun InstallmentItemCard(
    installment: InstallmentEntity,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        installment.isAnticipated -> AnticipatedBlue
        installment.isPaid -> PaidGreen
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val statusLabel = when {
        installment.isAnticipated -> "Amortizada (Fim)"
        installment.isPaid -> "Paga"
        else -> "Pendente"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (installment.isPaid) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (installment.isPaid) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Number & Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle with parcel number
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${installment.parcelNumber}",
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = installment.dueDate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Amortização: ${formatCurrency(installment.theoreticalAmortization)} | Juros: ${formatCurrency(installment.interestAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (installment.isPaid && installment.actualPaidAmount != null) {
                        Text(
                            text = "Valor Pago: ${formatCurrency(installment.actualPaidAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (installment.savedInterest > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = GoldTertiaryLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Economia: +${formatCurrency(installment.savedInterest)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldTertiaryLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Right Actions: Toggle Checkbox & Edit button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (installment.isPaid) PaidGreen else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onTogglePaid) {
                        Icon(
                            imageVector = if (installment.isPaid) Icons.Default.Check else Icons.Default.HourglassEmpty,
                            contentDescription = "Alternar status",
                            tint = if (installment.isPaid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditInstallmentDialog(
    installment: InstallmentEntity,
    onDismiss: () -> Unit,
    onConfirm: (actualPaid: Double, paymentDate: String, notes: String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(
            installment.actualPaidAmount?.toString() ?: installment.nominalAmount.toString()
        )
    }
    var dateText by remember { mutableStateOf(installment.paymentDate ?: installment.dueDate) }
    var notesText by remember { mutableStateOf(installment.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Parcela #${installment.parcelNumber}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Vencimento oficial: ${installment.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor Efetivamente Pago (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Data do Pagamento (DD/MM/AAAA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Observação / Banco") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.replace(',', '.').toDoubleOrNull() ?: installment.nominalAmount
                    onConfirm(amount, dateText, notesText)
                }
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
