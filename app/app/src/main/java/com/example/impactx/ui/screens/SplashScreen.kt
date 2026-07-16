package com.example.impactx.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    // Navigate after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        onTimeout()
    }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBlue, Color(0xFF040D17))
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Animated Radar Icon representation
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulse waves
                Box(
                    modifier = Modifier
                        .size(100.dp * pulseScale)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = pulseAlpha))
                )
                
                // Solid Inner circle
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(TealPrimary, Color(0xFF006666))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Logo
            Text(
                text = "IMPACT.X",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Burbuja de Seguridad Inteligente",
                fontSize = 14.sp,
                color = GrayMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
