package com.example.impactxwearable.ui

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.wear.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impactxwearable.data.SensorService

@Composable
fun WearHomeScreen(
    isServiceRunning: Boolean,
    heartRate: Int,
    gForce: Float,
    maxGForce: Float,
    isConnected: Boolean,
    onToggleService: () -> Unit,
    onSimulateImpact: () -> Unit
) {
    // Heart pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // App Title
            Text(
                text = "IMPACT.X",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00BFA5),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Heart Rate PPG Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Pulsaciones",
                    tint = if (heartRate > 0) Color(0xFFEF5350) else Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                        .let {
                            if (heartRate > 0) it.fillMaxHeight(heartScale) else it
                        }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (heartRate > 0) "$heartRate lpm" else "-- lpm",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // G-Force Display
            Text(
                text = String.format("%.2f G", gForce),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (gForce > 2.0f) Color(0xFFFFB74D) else Color.White
            )
            Text(
                text = String.format("Pico: %.2f G", maxGForce),
                fontSize = 10.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Phone Connection status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF81C784) else Color(0xFFEF5350))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isConnected) "Celular en línea" else "Sin conexión",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monitor Toggle Button
                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isServiceRunning) Color(0xFFEF5350) else Color(0xFF00BFA5)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.Warning else Icons.Default.PlayArrow,
                        contentDescription = if (isServiceRunning) "Detener" else "Iniciar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Simulate Impact Button
                Button(
                    onClick = onSimulateImpact,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF37474F)
                    ),
                    enabled = isServiceRunning,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
