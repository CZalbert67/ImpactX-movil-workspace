package com.example.impactxwearable.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text

/**
 * Critical-impact acknowledgement screen. There is intentionally no countdown
 * or cancellation: SensorService already relayed the impact immediately and
 * continues the trip/telemetry while the phone confirms or queues the SOS.
 */
@Composable
fun WearAlertScreen(
    onCancel: () -> Unit,
    onTimeout: () -> Unit,
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkColor = infiniteTransition.animateColor(
        initialValue = Color(0xFFD32F2F),
        targetValue = Color(0xFF06111F),
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinkColor",
    ).value

    val vibrator = androidx.compose.runtime.remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    LaunchedEffect(Unit) {
        val pattern = longArrayOf(0, 400, 200, 400, 200)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
        // Kept for compatibility with the existing caller. The caller no
        // longer emits a second event because the impact is already relayed.
        onTimeout()
    }

    DisposableEffect(Unit) {
        onDispose { vibrator.cancel() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(blinkColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(12.dp),
        ) {
            Text(
                text = "🚨 ALERTA GENERADA 🚨",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Enviada al teléfono sin espera",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Con internet se notifican los monitores. Sin internet queda pendiente hasta reconectar.",
                fontSize = 9.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    vibrator.cancel()
                    onCancel()
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00BFA5)),
                modifier = Modifier.fillMaxWidth(0.9f).height(34.dp).clip(RoundedCornerShape(8.dp)),
            ) {
                Text(
                    text = "ENTENDIDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
