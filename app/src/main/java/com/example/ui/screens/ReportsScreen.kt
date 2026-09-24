package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FinancingSummary
import com.example.ui.AmortizationViewModel
import com.example.ui.MonthlyReportItem
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AnticipatedBlue
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PendingAmber

@Composable
fun ReportsScreen(
    viewModel: AmortizationViewModel,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.monthlyReports.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Relatórios Mensais",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Demonstrativo analítico de saldo devedor e juros mês a mês",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { shareFinancialReport(context, summary, reports) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar Relatório", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restaurar", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saldo Devedor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(summary.remainingNominalBalance), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Pago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(summary.totalPaidAmount), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = PaidGreen)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Juros Poupados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(summary.totalSavedInterest), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = GoldTertiaryLight)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Monthly Detailed List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { item ->
                MonthlyReportCard(item)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restaurar Base Oficial?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Deseja restaurar todas as 60 parcelas e o histórico exatamente para a planilha de amortização original?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToSpreadsheetData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MonthlyReportCard(item: MonthlyReportItem) {
    val statusBadgeColor = when {
        item.isAnticipated -> AnticipatedBlue
        item.isPaid -> PaidGreen
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(statusBadgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${item.parcelNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.monthYear,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBadgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            item.isAnticipated -> "Amortizada"
                            item.isPaid -> "Quitada"
                            else -> "Pendente"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusBadgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Saldo Inicial:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(item.initialBalance), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Amortização:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(item.amortizationAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Juros Mês:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(item.interestPaid), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.savedInterest > 0) {
                    Text(
                        text = "Economia gerada: +${formatCurrency(item.savedInterest)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldTertiaryLight,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Saldo Final: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(item.finalBalance),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun shareFinancialReport(
    context: Context,
    summary: FinancingSummary,
    reports: List<MonthlyReportItem>
) {
    val sb = StringBuilder()
    sb.appendLine("📊 RELATÓRIO DE AMORTIZAÇÃO DO FINANCIAMENTO")
    sb.appendLine("===========================================")
    sb.appendLine("Total Contratado: ${formatCurrency(summary.totalContractAmount)}")
    sb.appendLine("Total Já Pago: ${formatCurrency(summary.totalPaidAmount)}")
    sb.appendLine("Saldo Devedor Restante: ${formatCurrency(summary.remainingNominalBalance)}")
    sb.appendLine("Juros Totais Poupados: ${formatCurrency(summary.totalSavedInterest)}")
    sb.appendLine("Parcelas Quitadas: ${summary.paidInstallmentsCount} de ${summary.totalInstallments} (${summary.anticipatedInstallmentsCount} amortizadas antecipadas)")
    sb.appendLine("Meses Adiantados: ${summary.monthsAdvanced} meses")
    sb.appendLine("===========================================")
    sb.appendLine("Últimos meses registrados:")
    reports.filter { it.isPaid }.takeLast(5).forEach { item ->
        sb.appendLine("- Parcela #${item.parcelNumber} (${item.monthYear}): Pago ${formatCurrency(item.paymentAmount)} | Economia: ${formatCurrency(item.savedInterest)}")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Relatório AmortizaFlow")
    context.startActivity(shareIntent)
}
