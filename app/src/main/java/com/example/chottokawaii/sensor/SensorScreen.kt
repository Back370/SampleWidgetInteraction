package com.example.chottokawaii.sensor

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
import kotlin.math.abs
import kotlin.math.atan2


@Composable
fun SensorScreen(
    viewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // シェイク検知時にToastを表示
    LaunchedEffect(uiState.isShaking) {
        if (uiState.isShaking) {
            Toast.makeText(context, "ああああああああああああああ！！", Toast.LENGTH_SHORT).show()
        }
    }

    // センサーの監視
    DisposableEffect(Unit) {
        viewModel.startListening(context)
        onDispose {
            viewModel.stopListening()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 新しい水準器UI
        LineLevel(
            xAxisValue = uiState.xAxis,
            yAxisValue = uiState.yAxis,
            zAxisValue = uiState.zAxis,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        SensorValueRow("X座標", uiState.xAxis)
        Spacer(modifier = Modifier.height(16.dp))
        SensorValueRow("Y座標", uiState.yAxis)
        Spacer(modifier = Modifier.height(16.dp))
        SensorValueRow("Z座標", uiState.zAxis)
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun LineLevel(xAxisValue: Float, yAxisValue: Float, zAxisValue: Float, modifier: Modifier = Modifier) {

    // Math.toDegreesでラジアンから度数法に変換
    val xzAngle = Math.toDegrees(atan2(xAxisValue.toDouble(), zAxisValue.toDouble())).toFloat()
    val yzAngle = Math.toDegrees(atan2(yAxisValue.toDouble(), zAxisValue.toDouble())).toFloat()
    val xyAngle = Math.toDegrees(atan2(xAxisValue.toDouble(), yAxisValue.toDouble())).toFloat()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "${String.format("%.1f", xzAngle)}°", // 小数点以下1桁まで表示
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = if (abs(xzAngle) < 1.0f) Color.Green else Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- 直線を描画するキャンバス ---
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val center = this.center
            val lineWidth = this.size.width

            // 画面の水平線 (固定)
            drawLine(
                color = Color.LightGray,
                start = Offset(x = 0f, y = center.y),
                end = Offset(x = lineWidth, y = center.y),
                strokeWidth = 4f
            )

//            // 実際の水平線
//            rotate(degrees = angle, pivot = center) {
//                drawLine(
//                    color = Color(0,150,200),
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
        Text(label, modifier = Modifier.width(80.dp), fontSize = 18.sp)
        Text(
            text = String.format("%.2f", value),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}