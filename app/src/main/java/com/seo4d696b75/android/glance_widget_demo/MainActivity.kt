package com.seo4d696b75.android.glance_widget_demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.seo4d696b75.android.glance_widget_demo.ui.MainScreen
import com.seo4d696b75.android.glance_widget_demo.theme.AppTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import android.content.Intent
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seo4d696b75.android.glance_widget_demo.character.CharacterScreen
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson

import com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService
import com.seo4d696b75.android.glance_widget_demo.ui.theme.ChottoKawaiiTheme
import com.seo4d696b75.android.glance_widget_demo.home.HomeScreen
import com.seo4d696b75.android.glance_widget_demo.sensor.SensorScreen
import com.seo4d696b75.android.glance_widget_demo.character.CharacterScreen
import com.seo4d696b75.android.glance_widget_demo.response.GeminiModel
import com.seo4d696b75.android.glance_widget_demo.time.UploadWorker
import java.util.concurrent.TimeUnit

import com.seo4d696b75.android.glance_widget_demo.time.GeminiData
import com.seo4d696b75.android.glance_widget_demo.time.setDailyAlarm


class MainActivity : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        val geminiModel: GeminiModel by viewModels()

        // App Check初期化（セキュリティ強化）
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            android.util.Log.d("MainActivity", "✅ Firebase App Check initialized")
            android.util.Log.d("gemini", BuildConfig.apiKey)
        } catch (e: Exception) {
            Log.w("MainActivity", "⚠️ Firebase App Check initialization failed (optional)", e)
        }



        val myData = GeminiData(geminiModel)
        val gson = Gson()
        val GeminiJsonData = gson.toJson(myData)

            // ここでWorkRequestを作成し、WorkManagerに依頼する
        val myUploadWork = PeriodicWorkRequestBuilder<UploadWorker>(
            1, TimeUnit.HOURS, // repeatInterval (the period cycle)
            15, TimeUnit.MINUTES) // flexInterval
            .setInputData(
                workDataOf(
                    "gemini_json_data" to GeminiJsonData
                )
            )
            .build()
        WorkManager.getInstance(this).enqueue(myUploadWork) // これで依頼完了！

        setDailyAlarm(this) //アラーム設定
        Log.d("MyAlarmReceiver", "アラーム設定")
        Toast.makeText(this, "バックグラウンドタスクをスケジュールしました", Toast.LENGTH_SHORT).show()


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
                                onSettingsClicked = { navController.navigate("Settings") },
                                onWidgetSettingClicked = { navController.navigate("WidgetSettings") },
                                onSensorClicked = { navController.navigate("Sensor") },
                                geminiModel = geminiModel
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

//                        composable("WidgetSettings"){
//                            WidgetSettingsScreen(
//                                onBackClick = { navController.navigate("Home") }
//                            )
//                        }
//                        composable("Sensor"){
//                            SensorScreen(
//                                onBackClick = { navController.navigate("Home") },
//                                viewModel = viewModel(),
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
                    }
                }
            }
        }
        // 権限チェックと初期化は並行して実行
        checkAndRequestStoragePermission()

    }
    
    // Firebase匿名認証を行うサスペンド関数
    private suspend fun signInAnonymouslyIfNeeded() {
        try {
            Log.d("MainActivity", "Attempting Firebase anonymous authentication...")
            
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                Log.d("MainActivity", "Anonymous authentication successful")
            } else {
                Log.d("MainActivity", "User already authenticated")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", " Firebase authentication failed", e)
        }
    }
    
    // 権限名のリスト（Android 13+はREAD_MEDIA_IMAGESを使う）
    private val storagePermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    // 権限リクエストランチャ
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            Log.d("MainActivity", "All needed storage permissions granted")
            onStoragePermissionGranted()
        } else {
            Log.w("MainActivity", "Storage permissions denied")
            // 必要なら説明ダイアログなど出す
        }
    }

    @Composable
    fun setUpImage(){
        val context = LocalContext.current
        ImageDownloadService.downloadCharacterImages(context, "Haru")
        ImageDownloadService.downloadCharacterImages(context, "Mao")
    }

    @Composable
    private fun AppNavigation() {
        AppTheme {
            val navController = rememberNavController()
            
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "main",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("main") {
                        MainScreen(
                            onWidgetSettingsClick = { 
                                navController.navigate("widget_settings")
                            }
                        )
                    }
                    composable("widget_settings") {
                        WidgetSettingsScreen(
                            onBackClick = { 
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    // ストレージ権限チェック・リクエスト関数
    private fun checkAndRequestStoragePermission() {
        // Android 10+ の分離ストレージ対応…本アプリの own sandbox 配下のみなら権限不要だが、念のため
        val permissionsToRequest = storagePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isEmpty()) {
            Log.d("MainActivity", "All storage permissions already granted")
            onStoragePermissionGranted()
        } else {
            Log.d(
                "MainActivity",
                "Requesting storage permissions: $permissionsToRequest"
            )
            storagePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // 権限OK時の初期化処理
    private fun onStoragePermissionGranted() {
        Log.d("MainActivity", "Storage permissions granted - initializing folders")

        // ウィジェット用の画像フォルダを作成
        createWidgetImageFolder()

        // 詳細なデバッグ情報を出力
        debugExternalStorage()
        
        // 画像ダウンロードを開始
        startImageDownloads()
    }

    private fun createWidgetImageFolder() {
        try {
            val baseDir = getExternalFilesDir(null)
            Log.d("MainActivity", "=== Widget Folder Creation ===")
            Log.d("MainActivity", "Base external files dir: ${baseDir?.absolutePath}")

            if (baseDir != null) {
                val widgetImagesDir = File(baseDir, "WidgetImages")
                Log.d(
                    "MainActivity",
                    "Target WidgetImages dir: ${widgetImagesDir.absolutePath}"
                )

                if (!widgetImagesDir.exists()) {
                    val created = widgetImagesDir.mkdirs()
                    Log.d("MainActivity", "WidgetImages folder created: $created")
                    Log.d(
                        "MainActivity",
                        "Folder path: ${widgetImagesDir.absolutePath}"
                    )
                } else {
                    Log.d("MainActivity", "WidgetImages folder already exists")
                }

            } else {
                Log.e("MainActivity", "External files directory is null")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error creating WidgetImages folder", e)
        }
    }





    private fun startImageDownloads() {
        Log.d("MainActivity", "=== Starting Image Downloads ===")
        
        try {
            // 現在のディレクトリ状況を確認
            val baseDir = getExternalFilesDir(null)
            Log.d("MainActivity", "Base directory: ${baseDir?.absolutePath}")
            
            // Mao/State/Adle と Mao/State/Flow ディレクトリの状況を確認
            val maoDir = File(baseDir, "Mao")
            val stateDir = File(maoDir, "State")
            val adleDir = File(stateDir, "Adle")
            val flowDir = File(stateDir, "Flow")
            
            Log.d("MainActivity", "📁 Directory status:")
            Log.d("MainActivity", "  - Mao: exists=${maoDir.exists()}")
            Log.d("MainActivity", "  - State: exists=${stateDir.exists()}")
            Log.d("MainActivity", "  - Adle: exists=${adleDir.exists()}, files=${adleDir.listFiles()?.size ?: 0}")
            Log.d("MainActivity", "  - Flow: exists=${flowDir.exists()}, files=${flowDir.listFiles()?.size ?: 0}")
            
            // Maoキャラクターの画像をダウンロード
            Log.d("MainActivity", " Starting download for Mao character")
            
            // 全てのアニメーションタイプをダウンロード
            ImageDownloadService.downloadCharacterImages(this, "Mao")
            ImageDownloadService.downloadCharacterImages(this, "Haru")


            Log.d("MainActivity", " Image download service started for Mao character")
            
            // 5秒後にダウンロード状況を再確認
            CoroutineScope(Dispatchers.Main).launch {
                delay(5000)
                checkDownloadStatus()
            }
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting image downloads", e)
        }
    }
    
    private fun checkDownloadStatus() {
        Log.d("MainActivity", "=== Download Status Check ===")
        
        val baseDir = getExternalFilesDir(null)
        val adleDir = File(baseDir, "Mao/State/Adle")
        val flowDir = File(baseDir, "Mao/State/Flow")
        
        Log.d("MainActivity", "Download results:")
        Log.d("MainActivity", "  - Adle images: ${adleDir.listFiles()?.count { it.isFile && it.extension.lowercase() == "png" } ?: 0}")
        Log.d("MainActivity", "  - Flow images: ${flowDir.listFiles()?.count { it.isFile && it.extension.lowercase() == "png" } ?: 0}")
        
        if (adleDir.exists()) {
            adleDir.listFiles()?.take(3)?.forEach { file ->
                Log.d("MainActivity", "  - Adle file: ${file.name} (${file.length()} bytes)")
            }
        }
        
        if (flowDir.exists()) {
            flowDir.listFiles()?.take(3)?.forEach { file ->
                Log.d("MainActivity", "  - Flow file: ${file.name} (${file.length()} bytes)")
            }
        }
    }
    
    private fun debugExternalStorage() {
        try {
            Log.d("MainActivity", "=== External Storage Debug ===")

            val baseDir = getExternalFilesDir(null)
            if (baseDir != null) {
                val widgetImagesDir = File(baseDir, "WidgetImages")

                // ディレクトリ内のファイルをすべて確認
                if (widgetImagesDir.exists() && widgetImagesDir.isDirectory) {
                    val allFiles = widgetImagesDir.listFiles()
                    Log.d(
                        "MainActivity",
                        "Total files in WidgetImages: ${allFiles?.size ?: 0}"
                    )

                    allFiles?.forEach { file ->
                        Log.d("MainActivity", "File found: ${file.name}")
                        Log.d("MainActivity", "  - Extension: ${file.extension}")
                        Log.d("MainActivity", "  - Is file: ${file.isFile}")
                        Log.d("MainActivity", "  - Size: ${file.length()} bytes")
                        Log.d("MainActivity", "  - Full path: ${file.absolutePath}")

                        // 画像ファイルかチェック
                        val isImageFile =
                            file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                        Log.d("MainActivity", "  - Is image file: $isImageFile")
                    }

                    // 画像ファイルのみを抽出
                    val imageFiles = allFiles?.filter { file ->
                        file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                    }?.sortedBy { it.name }

                    Log.d(
                        "MainActivity",
                        "Total image files: ${imageFiles?.size ?: 0}"
                    )
                    imageFiles?.forEachIndexed { index, file ->
                        Log.d("MainActivity", "Image $index: ${file.name}")
                    }
                } else {
                    Log.w(
                        "MainActivity",
                        "WidgetImages directory does not exist or is not a directory"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error debugging external storage", e)
        }
    }
}

