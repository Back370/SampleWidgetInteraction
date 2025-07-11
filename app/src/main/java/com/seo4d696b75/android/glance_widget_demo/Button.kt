package com.seo4d696b75.android.glance_widget_demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Button(
    modifier: Modifier = Modifier,
    icon: Painter,
    text: String,
    onClick: () -> Unit = {}
){
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Cyan.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )

        Text(
            text = text,
            fontSize = 30.sp,
            textAlign = TextAlign.Center
        )
    }
}