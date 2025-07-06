package com.example.chottokawaii

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chottokawaii.character.CharacterScreen
import com.example.chottokawaii.home.HomeScreen
import com.example.chottokawaii.ui.theme.ChottoKawaiiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChottoKawaiiTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) {
                    innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "Home",
                        modifier = Modifier.padding(innerPadding)
                    ){
                        composable("Home") {
                            HomeScreen(
                                onCharacterClicked = { navController.navigate("Character") },
                                onSettingsClicked = { navController.navigate("Settings") }
                            )
                        }
                        composable("Settings") {
                            settingScreen(
                                onBackClick = { navController.navigate("Home") }
                            )
                        }
                        composable("Character"){
                            CharacterScreen(
                                onBackClick = { navController.navigate("Home") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChottoKawaiiTheme {
        Greeting("Android")
    }
}
