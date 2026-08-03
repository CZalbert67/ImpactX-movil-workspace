package com.example.impactxwearable.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.wear.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WearAlertScreen(
    onCancel: () -> Unit,
    onTimeout: () -> Unit
) {
    val context = LocalContext.current
    var secondsLeft by remember { mutableStateOf(15) }
    var sosSent by remember { mutableStateOf(false) }
    
    // Warning background blinking animation
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFD32F2F), // Red
        targetValue = Color(0xFF06111F),  // Dark background
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkColor"
    )

    // Vibrator Setup
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Start vibrating and countdown loop
    LaunchedEffect(Unit) {
        val pattern = longArrayOf(0, 400, 200, 400, 200)
        
        while (secondsLeft > 0) {
            // Trigger vibration pattern
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }

            delay(1000)
            secondsLeft--
        }
        
        // Timer finished -> Trigger emergency SOS
        onTimeout()
        sosSent = true
    }

    // Stop vibration when screen is closed
    DisposableEffect(Unit) {
        onDispose {
            vibrator.cancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(blinkColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (sosSent) {
                Text(
                    text = "🚨 SOS ENVIADO 🚨",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Se ha alertado a tus contactos.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Entendido Button to exit alarm screen
                Button(
                    onClick = {
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00BFA5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "ENTENDIDO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "🚨 ALERTA 🚨",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "¿ESTÁS BIEN?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Mandando SOS en:",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )

                // Large Countdown Timer
                Text(
                    text = "$secondsLeft",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Cancel Button ("ESTOY BIEN")
                Button(
                    onClick = {
                        vibrator.cancel()
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00BFA5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "ESTOY BIEN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
