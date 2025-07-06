package com.example.chottokawaii.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chottokawaii.Button
import com.example.chottokawaii.R

//@Preview(showBackground = true)
@Composable
fun HomeScreen(
    onCharacterClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {}
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "ち ょ っ と",
            fontSize = 55.sp
        )
        Text(
            text = "か わ い い",
            fontSize = 55.sp
        )

        Spacer(modifier = Modifier.height(50.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ){

            Button(
                modifier = Modifier.weight(1f),
                icon = painterResource(id = R.drawable.baseline_face_24),
                text = "キャラ切替",
                onClick = onCharacterClicked
            )

            Button(
                modifier = Modifier.weight(1f),
                icon = painterResource(id = R.drawable.baseline_settings_24),
                text = "設定",
                onClick = onSettingsClicked
            )
        }

    }
}

