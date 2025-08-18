package com.seo4d696b75.android.glance_widget_demo.core.calendar

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes

/**
 * Google Calendar API設定の診断を行うユーティリティクラス
 */
class GoogleCalendarConfigChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "CalendarConfigChecker"
    }
    
    /**
     * 設定の診断を実行
     */
    fun diagnoseConfiguration(): DiagnosisResult {
        Log.d(TAG, "🔍 Starting Google Calendar configuration diagnosis...")
        
        val results = mutableListOf<DiagnosisItem>()
        
        // 1. パッケージ名の確認
        results.add(checkPackageName())
        
        // 2. Google Play Servicesの確認
        results.add(checkGooglePlayServices())
        
        // 3. サインイン状態の確認
        results.add(checkSignInStatus())
        
        // 4. スコープの確認
        results.add(checkCalendarScope())
        
        // 5. ネットワーク接続の確認
        results.add(checkNetworkConnectivity())
        
        // 6. 権限の確認
        results.add(checkPermissions())
        
        val allPassed = results.all { it.status == DiagnosisStatus.PASS }
        val criticalIssues = results.filter { it.status == DiagnosisStatus.CRITICAL }
        
        return DiagnosisResult(
            allPassed = allPassed,
            items = results,
            criticalIssues = criticalIssues,
            recommendations = generateRecommendations(results)
        )
    }
    
    private fun checkPackageName(): DiagnosisItem {
        val packageName = context.packageName
        val expectedPackage = "com.seo4d696b75.android.glance_widget_demo"
        
        return if (packageName == expectedPackage) {
            DiagnosisItem(
                name = "Package Name",
                status = DiagnosisStatus.PASS,
                message = "Package name matches expected: $packageName"
            )
        } else {
            DiagnosisItem(
                name = "Package Name",
                status = DiagnosisStatus.CRITICAL,
                message = "Package name mismatch. Expected: $expectedPackage, Actual: $packageName"
            )
        }
    }
    
    private fun checkGooglePlayServices(): DiagnosisItem {
        return try {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                DiagnosisItem(
                    name = "Google Play Services",
                    status = DiagnosisStatus.PASS,
                    message = "Google Play Services is available"
                )
            } else {
                DiagnosisItem(
                    name = "Google Play Services",
                    status = DiagnosisStatus.CRITICAL,
                    message = "Google Play Services not available. Error code: $resultCode"
                )
            }
        } catch (e: Exception) {
            DiagnosisItem(
                name = "Google Play Services",
                status = DiagnosisStatus.CRITICAL,
                message = "Error checking Google Play Services: ${e.message}"
            )
        }
    }
    
    private fun checkSignInStatus(): DiagnosisItem {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        
        return if (account != null) {
            DiagnosisItem(
                name = "Sign-In Status",
                status = DiagnosisStatus.PASS,
                message = "User signed in: ${account.email}"
            )
        } else {
            DiagnosisItem(
                name = "Sign-In Status",
                status = DiagnosisStatus.WARNING,
                message = "No user signed in"
            )
        }
    }
    
    private fun checkCalendarScope(): DiagnosisItem {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        
        if (account == null) {
            return DiagnosisItem(
                name = "Calendar Scope",
                status = DiagnosisStatus.WARNING,
                message = "Cannot check scope - no user signed in"
            )
        }
        
        val hasScope = GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR))
        
        return if (hasScope) {
            DiagnosisItem(
                name = "Calendar Scope",
                status = DiagnosisStatus.PASS,
                message = "Calendar scope granted"
            )
        } else {
            DiagnosisItem(
                name = "Calendar Scope",
                status = DiagnosisStatus.CRITICAL,
                message = "Calendar scope not granted"
            )
        }
    }
    
    private fun checkNetworkConnectivity(): DiagnosisItem {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            
            if (networkInfo?.isConnected == true) {
                DiagnosisItem(
                    name = "Network Connectivity",
                    status = DiagnosisStatus.PASS,
                    message = "Network is connected"
                )
            } else {
                DiagnosisItem(
                    name = "Network Connectivity",
                    status = DiagnosisStatus.CRITICAL,
                    message = "No network connection"
                )
            }
        } catch (e: Exception) {
            DiagnosisItem(
                name = "Network Connectivity",
                status = DiagnosisStatus.WARNING,
                message = "Could not check network connectivity: ${e.message}"
            )
        }
    }
    
    private fun checkPermissions(): DiagnosisItem {
        val requiredPermissions = listOf(
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.GET_ACCOUNTS
        )
        
        val missingPermissions = requiredPermissions.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        
        return if (missingPermissions.isEmpty()) {
            DiagnosisItem(
                name = "Required Permissions",
                status = DiagnosisStatus.PASS,
                message = "All required permissions granted"
            )
        } else {
            DiagnosisItem(
                name = "Required Permissions",
                status = DiagnosisStatus.CRITICAL,
                message = "Missing permissions: ${missingPermissions.joinToString()}"
            )
        }
    }
    
    private fun generateRecommendations(results: List<DiagnosisItem>): List<String> {
        val recommendations = mutableListOf<String>()
        
        results.forEach { item ->
            when (item.status) {
                DiagnosisStatus.CRITICAL -> {
                    when (item.name) {
                        "Package Name" -> recommendations.add("Google Cloud Consoleでパッケージ名を正しく設定してください")
                        "Google Play Services" -> recommendations.add("Google Play Servicesを更新してください")
                        "Calendar Scope" -> recommendations.add("Googleサインインでカレンダー権限を許可してください")
                        "Network Connectivity" -> recommendations.add("インターネット接続を確認してください")
                        "Required Permissions" -> recommendations.add("必要な権限をアプリ設定で許可してください")
                    }
                }
                DiagnosisStatus.WARNING -> {
                    when (item.name) {
                        "Sign-In Status" -> recommendations.add("Googleアカウントでサインインしてください")
                        "Calendar Scope" -> recommendations.add("カレンダー権限の追加を要求してください")
                    }
                }
                else -> {}
            }
        }
        
        // 一般的な推奨事項
        if (results.any { it.name == "Calendar Scope" && it.status == DiagnosisStatus.CRITICAL }) {
            recommendations.add("Google Cloud ConsoleでCalendar APIが有効化されているか確認してください")
            recommendations.add("OAuth同意画面でテストユーザーが追加されているか確認してください")
        }
        
        return recommendations
    }
}

/**
 * 診断結果を表すデータクラス
 */
data class DiagnosisResult(
    val allPassed: Boolean,
    val items: List<DiagnosisItem>,
    val criticalIssues: List<DiagnosisItem>,
    val recommendations: List<String>
)

/**
 * 個別の診断項目を表すデータクラス
 */
data class DiagnosisItem(
    val name: String,
    val status: DiagnosisStatus,
    val message: String
)

/**
 * 診断ステータスを表す列挙型
 */
enum class DiagnosisStatus {
    PASS,
    WARNING,
    CRITICAL
}
