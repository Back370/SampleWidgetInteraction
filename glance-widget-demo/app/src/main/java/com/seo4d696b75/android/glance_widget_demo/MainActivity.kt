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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.appcheck.BuildConfig
import com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService
import com.seo4d696b75.android.glance_widget_demo.ui.theme.ChottoKawaiiTheme
import com.seo4d696b75.android.glance_widget_demo.home.HomeScreen
import com.seo4d696b75.android.glance_widget_demo.character.CharacterScreen



class MainActivity : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        // App Check初期化（セキュリティ強化）
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            android.util.Log.d("MainActivity", "✅ Firebase App Check initialized")
            android.util.Log.d("gemini", BuildConfig.apiKey)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "⚠️ Firebase App Check initialization failed (optional)", e)
        }

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
                                onWidgetSettingClicked = { navController.navigate("WidgetSettings") }
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
                        composable("WidgetSettings"){
                            WidgetSettingsScreen(
                                onBackClick = { navController.navigate("Home") }
                            )
                        }
                    }
                }
            }
        }
        // 権限チェックと初期化は並行して実行
        checkAndRequestStoragePermission()

        android.util.Log.d("MainActivity", "MainActivity setup completed")
    }
    
    // Firebase匿名認証を行うサスペンド関数
    private suspend fun signInAnonymouslyIfNeeded() {
        try {
            android.util.Log.d("MainActivity", "🔐 Attempting Firebase anonymous authentication...")
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                android.util.Log.d("MainActivity", "✅ Anonymous authentication successful")
            } else {
                android.util.Log.d("MainActivity", "ℹ️ User already authenticated")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Firebase authentication failed", e)
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
            android.util.Log.d("MainActivity", "All needed storage permissions granted")
            onStoragePermissionGranted()
        } else {
            android.util.Log.w("MainActivity", "Storage permissions denied")
            // 必要なら説明ダイアログなど出す
        }
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
            android.util.Log.d("MainActivity", "All storage permissions already granted")
            onStoragePermissionGranted()
        } else {
            android.util.Log.d(
                "MainActivity",
                "Requesting storage permissions: $permissionsToRequest"
            )
            storagePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // 権限OK時の初期化処理
    private fun onStoragePermissionGranted() {
        android.util.Log.d("MainActivity", "Storage permissions granted - initializing folders")

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
            android.util.Log.d("MainActivity", "=== Widget Folder Creation ===")
            android.util.Log.d("MainActivity", "Base external files dir: ${baseDir?.absolutePath}")

            if (baseDir != null) {
                val widgetImagesDir = File(baseDir, "WidgetImages")
                android.util.Log.d(
                    "MainActivity",
                    "Target WidgetImages dir: ${widgetImagesDir.absolutePath}"
                )

                if (!widgetImagesDir.exists()) {
                    val created = widgetImagesDir.mkdirs()
                    android.util.Log.d("MainActivity", "WidgetImages folder created: $created")
                    android.util.Log.d(
                        "MainActivity",
                        "Folder path: ${widgetImagesDir.absolutePath}"
                    )
                } else {
                    android.util.Log.d("MainActivity", "WidgetImages folder already exists")
                }

                // フォルダの存在と権限を確認
                android.util.Log.d("MainActivity", "Folder exists: ${widgetImagesDir.exists()}")
                android.util.Log.d(
                    "MainActivity",
                    "Folder is directory: ${widgetImagesDir.isDirectory}"
                )
                android.util.Log.d("MainActivity", "Folder readable: ${widgetImagesDir.canRead()}")
                android.util.Log.d("MainActivity", "Folder writable: ${widgetImagesDir.canWrite()}")

            } else {
                android.util.Log.e("MainActivity", "External files directory is null")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error creating WidgetImages folder", e)
        }
    }





    private fun startImageDownloads() {
        android.util.Log.d("MainActivity", "=== Starting Image Downloads ===")
        
        try {
            // 現在のディレクトリ状況を確認
            val baseDir = getExternalFilesDir(null)
            android.util.Log.d("MainActivity", "Base directory: ${baseDir?.absolutePath}")
            
            // Mao/State/Adle と Mao/State/Flow ディレクトリの状況を確認
            val maoDir = File(baseDir, "Mao")
            val stateDir = File(maoDir, "State")
            val adleDir = File(stateDir, "Adle")
            val flowDir = File(stateDir, "Flow")
            
            android.util.Log.d("MainActivity", "📁 Directory status:")
            android.util.Log.d("MainActivity", "  - Mao: exists=${maoDir.exists()}")
            android.util.Log.d("MainActivity", "  - State: exists=${stateDir.exists()}")
            android.util.Log.d("MainActivity", "  - Adle: exists=${adleDir.exists()}, files=${adleDir.listFiles()?.size ?: 0}")
            android.util.Log.d("MainActivity", "  - Flow: exists=${flowDir.exists()}, files=${flowDir.listFiles()?.size ?: 0}")
            
            // Maoキャラクターの画像をダウンロード
            android.util.Log.d("MainActivity", "🚀 Starting download for Mao character")
            
            // 全てのアニメーションタイプをダウンロード
            ImageDownloadService.downloadCharacterImages(this, "Mao")
            
            android.util.Log.d("MainActivity", "✅ Image download service started for Mao character")
            
            // 5秒後にダウンロード状況を再確認
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(5000)
                checkDownloadStatus()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error starting image downloads", e)
        }
    }
    
    private fun checkDownloadStatus() {
        android.util.Log.d("MainActivity", "=== Download Status Check ===")
        
        val baseDir = getExternalFilesDir(null)
        val adleDir = File(baseDir, "Mao/State/Adle")
        val flowDir = File(baseDir, "Mao/State/Flow")
        
        android.util.Log.d("MainActivity", "📊 Download results:")
        android.util.Log.d("MainActivity", "  - Adle images: ${adleDir.listFiles()?.count { it.isFile && it.extension.lowercase() == "png" } ?: 0}")
        android.util.Log.d("MainActivity", "  - Flow images: ${flowDir.listFiles()?.count { it.isFile && it.extension.lowercase() == "png" } ?: 0}")
        
        if (adleDir.exists()) {
            adleDir.listFiles()?.take(3)?.forEach { file ->
                android.util.Log.d("MainActivity", "  - Adle file: ${file.name} (${file.length()} bytes)")
            }
        }
        
        if (flowDir.exists()) {
            flowDir.listFiles()?.take(3)?.forEach { file ->
                android.util.Log.d("MainActivity", "  - Flow file: ${file.name} (${file.length()} bytes)")
            }
        }
    }
    
    private fun debugExternalStorage() {
        try {
            android.util.Log.d("MainActivity", "=== External Storage Debug ===")

            val baseDir = getExternalFilesDir(null)
            if (baseDir != null) {
                val widgetImagesDir = File(baseDir, "WidgetImages")

                // ディレクトリ内のファイルをすべて確認
                if (widgetImagesDir.exists() && widgetImagesDir.isDirectory) {
                    val allFiles = widgetImagesDir.listFiles()
                    android.util.Log.d(
                        "MainActivity",
                        "Total files in WidgetImages: ${allFiles?.size ?: 0}"
                    )

                    allFiles?.forEach { file ->
                        android.util.Log.d("MainActivity", "File found: ${file.name}")
                        android.util.Log.d("MainActivity", "  - Extension: ${file.extension}")
                        android.util.Log.d("MainActivity", "  - Is file: ${file.isFile}")
                        android.util.Log.d("MainActivity", "  - Size: ${file.length()} bytes")
                        android.util.Log.d("MainActivity", "  - Full path: ${file.absolutePath}")

                        // 画像ファイルかチェック
                        val isImageFile =
                            file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                        android.util.Log.d("MainActivity", "  - Is image file: $isImageFile")
                    }

                    // 画像ファイルのみを抽出
                    val imageFiles = allFiles?.filter { file ->
                        file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                    }?.sortedBy { it.name }

                    android.util.Log.d(
                        "MainActivity",
                        "Total image files: ${imageFiles?.size ?: 0}"
                    )
                    imageFiles?.forEachIndexed { index, file ->
                        android.util.Log.d("MainActivity", "Image $index: ${file.name}")
                    }
                } else {
                    android.util.Log.w(
                        "MainActivity",
                        "WidgetImages directory does not exist or is not a directory"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error debugging external storage", e)
        }
    }
}

