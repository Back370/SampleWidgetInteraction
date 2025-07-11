package com.seo4d696b75.android.glance_widget_demo.sensor

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SensorScreen(
    viewModel: SensorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // シェイク検知時にToastを表示
    LaunchedEffect(uiState.isShaking) {
        if (uiState.isShaking) {
            Toast.makeText(context, "Shaking detected!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 水準器UI。ViewModelから渡されたXZ平面の角度を使う
        LineLevel(
            angle = uiState.xzAngle,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 各平面の角度を表示
        SensorValueRow("XZ Angle", uiState.xzAngle)
        Spacer(modifier = Modifier.height(16.dp))
        SensorValueRow("YZ Angle", uiState.yzAngle)
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun LineLevel(angle: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 角度を表示
        Text(
            text = "${String.format("%.1f", angle)}°",
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            // 傾きが1度未満なら緑色にする
            color = if (kotlin.math.abs(angle) < 1.0f) Color.Green else Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val center = this.center
            val lineWidth = this.size.width

            // 基準となる水平線 (動かない)
            drawLine(
                color = Color.LightGray,
                start = Offset(x = 0f, y = center.y),
                end = Offset(x = lineWidth, y = center.y),
                strokeWidth = 4f
            )

            // 実際の傾きを示す線 (ViewModelから渡された角度で回転)
//            rotate(degrees = angle, pivot = center) {
//                drawLine(
//                    color = Color(0, 150, 200),
//                    start = Offset(x = 0f, y = center.y),
//                    end = Offset(x = lineWidth, y = center.y),
//                    strokeWidth = 8f
//                )
//            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun SensorValueRow(label: String, value: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text(label, modifier = Modifier.width(120.dp), fontSize = 18.sp)
        Text(
            text = String.format("%.2f", value),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}