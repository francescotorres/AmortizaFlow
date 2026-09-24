package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstallmentEntity
import com.example.ui.theme.AnticipatedBlue
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.PaidGreen

@Composable
fun OutstandingDebtAreaChart(
    installments: List<InstallmentEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Curva do Saldo Devedor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Evolução do saldo devedor até a quitação",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "60 Meses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line/Area Chart
            val primaryColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 20f
                    val chartHeight = h - paddingBottom

                    // Draw subtle horizontal grid lines
                    val steps = 4
                    for (i in 0..steps) {
                        val y = chartHeight * (i.toFloat() / steps)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (installments.isEmpty()) return@Canvas

                    // Calculate remaining balance progression
                    var cumulativeBalance = 59122.80
                    val points = mutableListOf<Offset>()
                    val totalCount = installments.size.coerceAtLeast(1)

                    installments.forEachIndexed { index, item ->
                        if (item.isPaid) {
                            cumulativeBalance -= (item.actualPaidAmount ?: item.nominalAmount)
                        } else {
                            cumulativeBalance -= item.theoreticalAmortization
                        }
                        val x = (index.toFloat() / (totalCount - 1).coerceAtLeast(1)) * w
                        val yNormalized = (cumulativeBalance.toFloat() / 59122.80f).coerceIn(0f, 1f)
                        val y = chartHeight * (1f - yNormalized)
                        points.add(Offset(x, y))
                    }

                    // Draw area gradient
                    val areaPath = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points.first().x, chartHeight)
                            lineTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, chartHeight)
                            close()
                        }
                    }

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                primaryColor.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw curve line
                    val linePath = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                    }

                    drawLinePath(linePath, primaryColor, 3.dp.toPx())

                    // Highlight current position (Parcela #5 / #6)
                    val currentIndex = 5.coerceAtMost(points.size - 1)
                    if (currentIndex < points.size) {
                        val currentPoint = points[currentIndex]
                        drawCircle(
                            color = primaryColor,
                            radius = 6.dp.toPx(),
                            center = currentPoint
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = currentPoint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mai/2026 (R$ 59.122)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Atual (R$ 52.980)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Abr/2031 (R$ 0,00)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLinePath(
    path: Path,
    color: Color,
    strokeWidth: Float
) {
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

@Composable
fun FinancingProgressDonut(
    paidCount: Int,
    anticipatedCount: Int,
    totalCount: Int,
    totalSavedInterest: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Canvas Donut
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                val pendingColor = MaterialTheme.colorScheme.surfaceVariant
                val paidRegularColor = PaidGreen
                val anticipatedColor = AnticipatedBlue

                Canvas(modifier = Modifier.size(100.dp)) {
                    val stroke = 12.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background track
                    drawArc(
                        color = pendingColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    val regularPaidCount = (paidCount - anticipatedCount).coerceAtLeast(0)
                    val sweepRegular = (regularPaidCount.toFloat() / totalCount.toFloat()) * 360f
                    val sweepAnticipated = (anticipatedCount.toFloat() / totalCount.toFloat()) * 360f

                    // Regular paid arc
                    drawArc(
                        color = paidRegularColor,
                        startAngle = -90f,
                        sweepAngle = sweepRegular,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Anticipated amortized arc
                    drawArc(
                        color = anticipatedColor,
                        startAngle = -90f + sweepRegular,
                        sweepAngle = sweepAnticipated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pct = if (totalCount > 0) ((paidCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quitado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Legend Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Status das 60 Parcelas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                LegendRow(
                    color = PaidGreen,
                    label = "Pagas Regulares",
                    value = "${paidCount - anticipatedCount} parcelas"
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = AnticipatedBlue,
                    label = "Amortizadas (Fim)",
                    value = "$anticipatedCount parcelas"
                )
                Spacer(modifier = Modifier.height(6.dp))
                LegendRow(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    label = "Pendentes",
                    value = "${totalCount - paidCount} parcelas"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldTertiaryLight.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Poupança: ${formatCurrency(totalSavedInterest)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldTertiaryLight
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
