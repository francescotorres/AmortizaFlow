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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AmortizationType
import com.example.ui.AmortizationViewModel
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AnticipatedBlue
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.PaidGreen

@Composable
fun SimulatorScreen(
    viewModel: AmortizationViewModel,
    modifier: Modifier = Modifier
) {
    val simulatedAmount by viewModel.simulatedAmount.collectAsStateWithLifecycle()
    val simulationType by viewModel.simulationType.collectAsStateWithLifecycle()
    val result by viewModel.simulationResult.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val quickValues = listOf("500", "1000", "2000", "3000", "5000")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "Simulador de Amortização",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Descubra quanto poupará eliminando parcelas ou reduzindo a prestação",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Type Tabs
        TabRow(
            selectedTabIndex = if (simulationType == AmortizationType.REDUCE_TERM) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = simulationType == AmortizationType.REDUCE_TERM,
                onClick = { viewModel.setSimulationType(AmortizationType.REDUCE_TERM) },
                text = {
                    Text(
                        text = "Reduzir Prazo",
                        fontWeight = if (simulationType == AmortizationType.REDUCE_TERM) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = simulationType == AmortizationType.REDUCE_PAYMENT,
                onClick = { viewModel.setSimulationType(AmortizationType.REDUCE_PAYMENT) },
                text = {
                    Text(
                        text = "Reduzir Parcela",
                        fontWeight = if (simulationType == AmortizationType.REDUCE_PAYMENT) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quanto você tem para amortizar hoje?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = simulatedAmount,
                    onValueChange = { viewModel.setSimulationAmount(it) },
                    prefix = { Text("R$ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Valores rápidos:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(quickValues) { valStr ->
                        ElevatedFilterChip(
                            selected = simulatedAmount == valStr,
                            onClick = { viewModel.setSimulationAmount(valStr) },
                            label = { Text("R$ $valStr") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulation Results
        if (result != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resultado da Simulação",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (result!!.type == AmortizationType.REDUCE_TERM) {
                        // Term reduction highlights
                        ResultMetricItem(
                            icon = Icons.Default.FastForward,
                            iconColor = AnticipatedBlue,
                            title = "Parcelas Eliminadas Imediatamente",
                            value = "${result!!.monthsShortened} parcelas (${result!!.eliminatedInstallments.joinToString(", ") { "#$it" }})"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ResultMetricItem(
                            icon = Icons.Default.Savings,
                            iconColor = GoldTertiaryLight,
                            title = "Juros Futuros Poupados",
                            value = formatCurrency(result!!.totalSavedInterest),
                            highlight = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ResultMetricItem(
                            icon = Icons.Default.TrendingDown,
                            iconColor = PaidGreen,
                            title = "Nova Data Final Projetada",
                            value = result!!.newProjectedEndDate
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Explanatory alert
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.7f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "💡 Ao amortizar ${formatCurrency(result!!.totalPaidInSimulation)}, você deixa de pagar ${formatCurrency(result!!.totalSavedInterest)} de juros que seriam cobrados pelo banco!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (result!!.eliminatedInstallments.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.applySimulation() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aplicar Amortização no Financiamento", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Payment reduction highlights
                        ResultMetricItem(
                            icon = Icons.Default.TrendingDown,
                            iconColor = PaidGreen,
                            title = "Nova Parcela Mensal Estimada",
                            value = formatCurrency(result!!.newMonthlyInstallment ?: 985.38),
                            highlight = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ResultMetricItem(
                            icon = Icons.Default.Savings,
                            iconColor = GoldTertiaryLight,
                            title = "Economia Projetada em Juros",
                            value = formatCurrency(result!!.totalSavedInterest)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResultMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = if (highlight) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                },
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
