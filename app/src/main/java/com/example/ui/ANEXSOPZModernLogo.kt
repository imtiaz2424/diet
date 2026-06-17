package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ANEXSOPZModernLogo(
    modifier: Modifier = Modifier,
    showText: Boolean = false,
    isBengali: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFE0F2F1))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(54.dp)) {
                // Background subtle ring representing circadian rhythms & tracker cycles
                drawCircle(
                    color = Color(0xFF81C784).copy(alpha = 0.35f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Animated metabolic pulse ring
                drawArc(
                    color = Color(0xFF009688),
                    startAngle = rotation,
                    sweepAngle = 140f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                drawArc(
                    color = Color(0xFFFFB300),
                    startAngle = rotation + 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Center leaf symbol of health & wellness path
                val leafPath = android.graphics.Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.18f)
                    cubicTo(
                        size.width * 0.15f, size.height * 0.35f,
                        size.width * 0.22f, size.height * 0.85f,
                        size.width * 0.5f, size.height * 0.82f
                    )
                    cubicTo(
                        size.width * 0.78f, size.height * 0.85f,
                        size.width * 0.85f, size.height * 0.35f,
                        size.width * 0.5f, size.height * 0.18f
                    )
                    close()
                }

                drawPath(
                    path = leafPath.asComposePath(),
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2E7D32), Color(0xFF004D40))
                    )
                )

                // Leaf middle vein for high detail look
                drawLine(
                    color = Color(0xFFA5D6A7).copy(alpha = 0.5f),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.22f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.78f),
                    strokeWidth = 2.dp.toPx()
                )

                // Leaf side veins branching off beautifully
                val veinColor = Color(0xFFA5D6A7).copy(alpha = 0.6f)
                val veinProgressions = listOf(0.35f, 0.45f, 0.55f, 0.65f)
                for (p in veinProgressions) {
                    val y = size.height * p
                    // Left branch
                    drawLine(
                        color = veinColor,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width * (0.5f - (0.75f - p) * 0.45f), y - size.height * 0.08f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    // Right branch
                    drawLine(
                        color = veinColor,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width * (0.5f + (0.75f - p) * 0.45f), y - size.height * 0.08f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Small shiny gold spark representing healthy cells & longevity energy
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = size.width * 0.08f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.38f)
                )
            }
        }
        
        if (showText) {
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isBengali) "ANEXSOPZ হেলথ" else "ANEXSOPZ",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFF1E5E2F),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (isBengali) "ভালো খান, চমৎকার থাকুন!" else "Eat Well, Feel Great!",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(0xFF00796B),
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
