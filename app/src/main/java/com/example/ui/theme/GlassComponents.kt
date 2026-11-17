package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HolographicBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) {
        listOf(MidnightBg, Color(0xFF131938), Color(0xFF0E172E))
    } else {
        listOf(IceWhite, Color(0xFFEDF2FF), Color(0xFFE0F7FA).copy(alpha = 0.65f))
    }
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset(900f, 1600f)
            )
        )
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentGradient: List<Color> = listOf(RoyalPurple.copy(alpha = 0.35f), ElectricBlue.copy(alpha = 0.25f), LuminousCyan.copy(alpha = 0.2f)),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == MidnightBg
    val surfaceColor = if (isDark) MidnightGlass else IceGlassTint
    val borderBrush = Brush.linearGradient(accentGradient)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun HoloGradientButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = Brush.horizontalGradient(
        colors = listOf(RoyalPurple, ElectricBlue, LuminousCyan)
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) brush else Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AnimatedSystemCoreOrb(
    isHealthy: Boolean,
    lowEndMode: Boolean
) {
    val pulseScale = if (lowEndMode) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "core_orb")
        val value by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "core_scale"
        )
        value
    }

    val coreColors = if (isHealthy) {
        listOf(LuminousCyan, ElectricBlue, RoyalPurple)
    } else {
        listOf(RosePink, CrimsonError, MetallicGold)
    }

    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(coreColors),
                shape = CircleShape
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension / 2f) * pulseScale.coerceAtMost(1f)
            drawCircle(
                brush = Brush.radialGradient(coreColors),
                radius = radius
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = radius * 0.42f,
                center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TelemetryLineChart(
    values: List<Float>,
    labels: List<String>
) {
    val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (values.isEmpty()) return@Canvas
            val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
            val path = Path()
            val fillPath = Path()
            val points = values.mapIndexed { idx, v ->
                val x = idx * stepX
                val normalized = (v / maxVal).coerceIn(0f, 1f)
                val y = size.height - (normalized * (size.height * 0.82f)) - (size.height * 0.08f)
                Offset(x, y)
            }
            points.forEachIndexed { idx, pt ->
                if (idx == 0) {
                    path.moveTo(pt.x, pt.y)
                    fillPath.moveTo(pt.x, size.height)
                    fillPath.lineTo(pt.x, pt.y)
                } else {
                    path.lineTo(pt.x, pt.y)
                    fillPath.lineTo(pt.x, pt.y)
                }
            }
            fillPath.lineTo(points.last().x, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ElectricBlue.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(RoyalPurple, ElectricBlue, LuminousCyan)),
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            points.forEach { pt ->
                drawCircle(color = LuminousCyan, radius = 8f, center = pt)
                drawCircle(color = Color.White, radius = 4f, center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { lbl ->
                Text(
                    text = lbl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
