package com.seo4d696b75.android.glance_widget_demo.core.calendar

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Googleカレンダー連携機能を管理するクラス
 */
class GoogleCalendarManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleCalendarManager"
        private const val CALENDAR_ID = "primary" // プライマリカレンダーを使用
    }
    
    private var calendarService: Calendar? = null
    private var googleSignInClient: GoogleSignInClient? = null
    
    /**
     * Googleサインインクライアントを初期化
     */
    fun initializeGoogleSignIn() {
        try {
            Log.d(TAG, "🔐 Initializing Google Sign-In client...")
            
            // CALENDAR スコープを要求
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(CalendarScopes.CALENDAR))
                .build()
            
            Log.d(TAG, "🔐 GoogleSignInOptions built successfully")
            Log.d(TAG, "  - Requested scopes: ${gso.scopeArray?.joinToString()}")
            Log.d(TAG, "  - Package name: ${context.packageName}")
            
            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d(TAG, "🔐 Google Sign-In client initialized successfully")
            
            // 現在のサインイン状態をチェック
            val currentAccount = getCurrentAccount()
            if (currentAccount != null) {
                Log.d(TAG, "✅ User already signed in: ${currentAccount.email}")
                Log.d(TAG, "  - Granted scopes: ${currentAccount.grantedScopes.joinToString()}")
                Log.d(TAG, "  - Account ID: ${currentAccount.id}")
            } else {
                Log.d(TAG, "⚠️ No user currently signed in")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing Google Sign-In", e)
            Log.e(TAG, "  - Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  - Exception message: ${e.message}")
            if (e.cause != null) {
                Log.e(TAG, "  - Cause: ${e.cause?.message}")
            }
        }
    }
    
    /**
     * 現在サインインしているアカウントを取得
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * 必要なスコープが付与済みか
     */
    fun hasCalendarScope(account: GoogleSignInAccount?): Boolean {
        if (account == null) return false
        return GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR))
    }
    
    /**
     * カレンダーサービスを初期化（ユーザーのOAuth資格情報を使用）
     */
    private suspend fun initializeCalendarService(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔐 Initializing calendar service with user OAuth credential...")
            
            val account = getCurrentAccount()
            if (!hasCalendarScope(account)) {
                Log.e(TAG, "❌ Calendar scope not granted or user not signed in")
                Log.e(TAG, "  - Account: ${account?.email ?: "null"}")
                Log.e(TAG, "  - Has scope: ${hasCalendarScope(account)}")
                return@withContext false
            }
            
            Log.d(TAG, "  - Using account: ${account?.email}")
            Log.d(TAG, "  - Account ID: ${account?.id}")
            
            // ユーザーのGoogleアカウントでOAuth2資格情報を作成
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(CalendarScopes.CALENDAR)
            ).apply {
                // 新旧API差異によりどちらかが必要。両方セットは無害。
                selectedAccount = account?.account
                selectedAccountName = account?.email
            }
            
            Log.d(TAG, "  - OAuth2 credential created successfully")
            Log.d(TAG, "  - Selected account: ${credential.selectedAccountName}")
            
            calendarService = Calendar.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            )
                .setApplicationName("Glance Widget Demo")
                .build()
            
            Log.d(TAG, "✅ Calendar service initialized successfully with user account")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing calendar service", e)
            Log.e(TAG, "  - Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  - Exception message: ${e.message}")
            if (e.cause != null) {
                Log.e(TAG, "  - Cause: ${e.cause?.message}")
            }
            return@withContext false
        }
    }
    
    /**
     * 音声入力から予定を解析してカレンダーに追加
     */
    suspend fun addEventFromVoiceInput(voiceInput: String): CalendarResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎤 Processing voice input: '$voiceInput'")
            
            // 音声入力を解析して予定情報を抽出
            val eventInfo = parseVoiceInput(voiceInput)
            if (eventInfo == null) {
                Log.w(TAG, "⚠️ Could not parse voice input")
                return@withContext CalendarResult.Error("音声入力を解析できませんでした")
            }
            
            Log.d(TAG, "📅 Parsed event info: $eventInfo")
            
            // カレンダーサービスを初期化（サインインとスコープが前提）
            if (!initializeCalendarService()) {
                return@withContext CalendarResult.Error("Googleアカウントの認可が必要です。サインインしてください。")
            }
            
            // イベントを作成
            val event = createCalendarEvent(eventInfo)
            
            // カレンダーに追加
            val createdEvent = calendarService?.events()?.insert(CALENDAR_ID, event)?.execute()
            
            if (createdEvent != null) {
                Log.d(TAG, "✅ Event added to calendar: ${createdEvent.id}")
                return@withContext CalendarResult.Success(
                    eventInfo.title,
                    eventInfo.startTime,
                    createdEvent.htmlLink
                )
            } else {
                Log.e(TAG, "❌ Failed to add event to calendar")
                return@withContext CalendarResult.Error("カレンダーに予定を追加できませんでした")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding event from voice input", e)
            return@withContext CalendarResult.Error("予定の追加中にエラーが発生しました: ${e.message}")
        }
    }
    
    /**
     * 音声入力を解析して予定情報を抽出
     */
    private fun parseVoiceInput(voiceInput: String): EventInfo? {
        val normalizedInput = voiceInput.lowercase().trim()
        
        // 日時パターンを検出
        val dateTimePatterns = listOf(
            "今日" to LocalDateTime.now(),
            "明日" to LocalDateTime.now().plusDays(1),
            "明後日" to LocalDateTime.now().plusDays(2),
            "来週" to LocalDateTime.now().plusWeeks(1),
            "来月" to LocalDateTime.now().plusMonths(1)
        )
        
        var startTime: LocalDateTime? = null
        var title: String
        
        // 日時を検出
        for ((pattern, dateTime) in dateTimePatterns) {
            if (normalizedInput.contains(pattern)) {
                startTime = dateTime
                break
            }
        }
        
        // 時間パターンを検出（例：午後3時、15時、3時半、午後三時 など）
        val timePatterns = listOf(
            // 数字ベース
            Regex("午前(\\d{1,2})時") to { hour: Int -> if (hour == 12) 0 else hour },
            Regex("午後(\\d{1,2})時") to { hour: Int -> if (hour == 12) 12 else hour + 12 },
            Regex("(\\d{1,2})時") to { hour: Int -> hour }
        )
        // 漢数字ベース
        val timeKanjiPatterns = listOf(
            Regex("午前([一二三四五六七八九十]+)時") to { hour: Int -> if (hour == 12) 0 else hour },
            Regex("午後([一二三四五六七八九十]+)時") to { hour: Int -> if (hour == 12) 12 else hour + 12 },
            Regex("([一二三四五六七八九十]+)時") to { hour: Int -> hour }
        )
        
        // 時:分形式のパターンを別途処理
        val timeColonPattern = Regex("(\\d{1,2}):(\\d{2})")
        val timeColonMatch = timeColonPattern.find(normalizedInput)
        
        var hour = 9 // デフォルトは午前9時
        var minute = 0
        
        // 時:分形式の処理（例: 15:30）
        if (timeColonMatch != null) {
            val groups = timeColonMatch.groupValues
            if (groups.size >= 3) {
                hour = groups[1].toInt()
                minute = groups[2].toInt()
            }
        } else {
            // その他の時間パターンの処理（数字ベース）
            for ((pattern, hourConverter) in timePatterns) {
                val match = pattern.find(normalizedInput)
                if (match != null) {
                    val groups = match.groupValues
                    if (groups.size > 1) {
                        hour = hourConverter(groups[1].toInt())
                        break
                    }
                }
            }
            // まだ時間が数字で特定できていない場合は漢数字を試す
            if (!Regex("(午前|午後)?\\d{1,2}時").containsMatchIn(normalizedInput)) {
                for ((pattern, hourConverter) in timeKanjiPatterns) {
                    val match = pattern.find(normalizedInput)
                    if (match != null) {
                        val groups = match.groupValues
                        if (groups.size > 1) {
                            val kanji = groups[1]
                            hour = hourConverter(parseJapaneseNumeral(kanji))
                            break
                        }
                    }
                }
            }
        }

        // 「時半」が含まれていれば30分に設定（コロン指定がない場合）
        if (timeColonMatch == null && Regex("時半").containsMatchIn(normalizedInput)) {
            minute = 30
        }

        // 予定の長さ（デフォルト1時間）。「一時間」「1時間」「30分」などを解析
        var durationHours = 1
        var durationMinutes = 0
        // 数字: 時間/分
        Regex("(\\d{1,2})時間").find(normalizedInput)?.let {
            durationHours = it.groupValues[1].toInt()
        }
        Regex("(\\d{1,3})分").find(normalizedInput)?.let {
            durationMinutes = it.groupValues[1].toInt()
        }
        // 漢数字: 時間/分
        Regex("([一二三四五六七八九十]+)時間").find(normalizedInput)?.let {
            durationHours = parseJapaneseNumeral(it.groupValues[1])
        }
        Regex("([一二三四五六七八九十]+)分").find(normalizedInput)?.let {
            durationMinutes = parseJapaneseNumeral(it.groupValues[1])
        }
        
        // タイトルを抽出（日時以外の部分）
        title = extractTitle(normalizedInput)
        
        // 開始時刻を設定
        val finalStartTime = startTime?.withHour(hour)?.withMinute(minute) ?: LocalDateTime.now().withHour(hour).withMinute(minute)
        
        return EventInfo(
            title = title.ifEmpty { "予定" },
            startTime = finalStartTime,
            endTime = finalStartTime
                .plusHours(durationHours.toLong())
                .plusMinutes(durationMinutes.toLong()),
            description = voiceInput
        )
    }
    
    /**
     * タイトルを抽出
     */
    private fun extractTitle(input: String): String {
        // まず日時に関する具体的なパターンを削除（数字・漢数字どちらも）
        var title = input
        val removalPatterns = listOf(
            // 例: 午後3時, 午前10時, 15:30, 3時半, 三時半
            Regex("(今日|明日|明後日|明々後日|来週|来月)"),
            Regex("(午前|午後)?\\d{1,2}:\\d{2}"),
            Regex("(午前|午後)?\\d{1,2}時半"),
            Regex("(午前|午後)?\\d{1,2}時"),
            Regex("(午前|午後)?[一二三四五六七八九十]+時半"),
            Regex("(午前|午後)?[一二三四五六七八九十]+時"),
            Regex("[一二三四五六七八九十]+時間"),
            Regex("\\d{1,2}時間"),
            Regex("[一二三四五六七八九十]+分"),
            Regex("\\d{1,3}分"),
            // 助詞など
            Regex("[ 　]*(から|まで|に|で|を|が|は|の)[ 　]*")
        )
        for (pattern in removalPatterns) {
            title = pattern.replace(title, " ")
        }
        // トリムと連続空白の正規化
        title = title.replace("\\s+".toRegex(), " ").trim()
        return title.ifEmpty { "予定" }
    }
    
    /**
     * カレンダーイベントを作成
     */
    private fun createCalendarEvent(eventInfo: EventInfo): Event {
        // 端末のタイムゾーンで時刻を固定し、Google Calendar にも同じタイムゾーン情報を渡す
        val zone = java.time.ZoneId.systemDefault()
        val startZdt = eventInfo.startTime.atZone(zone)
        val endZdt = eventInfo.endTime.atZone(zone)

        val startDateTime = DateTime(startZdt.toInstant().toEpochMilli(), startZdt.offset.totalSeconds / 60)
        val endDateTime = DateTime(endZdt.toInstant().toEpochMilli(), endZdt.offset.totalSeconds / 60)

        return Event().apply {
            summary = eventInfo.title
            description = eventInfo.description
            start = EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(zone.id)
            end = EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(zone.id)
        }
    }

    /**
     * 漢数字（〜59程度まで）を整数に変換
     */
    private fun parseJapaneseNumeral(numeral: String): Int {
        val digitMap = mapOf(
            '零' to 0, '〇' to 0,
            '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
            '六' to 6, '七' to 7, '八' to 8, '九' to 9
        )
        // 「十」ベースの簡易変換: 例) 十=10, 十一=11, 二十=20, 二十三=23
        val tenChar = '十'
        if (numeral.contains(tenChar)) {
            val parts = numeral.split(tenChar)
            val tens = when (val left = parts.getOrNull(0)) {
                null, "" -> 1
                else -> left.mapNotNull { digitMap[it] }.firstOrNull() ?: 0
            }
            val ones = parts.getOrNull(1)?.mapNotNull { digitMap[it] }?.firstOrNull() ?: 0
            return tens * 10 + ones
        }
        // 一桁
        return numeral.mapNotNull { digitMap[it] }.firstOrNull() ?: 0
    }
    
    /**
     * サインインが必要かどうかをチェック
     */
    fun isSignInRequired(): Boolean {
        val account = getCurrentAccount()
        return !hasCalendarScope(account)
    }
    
    /**
     * サインインインテントを取得
     */
    fun getSignInIntent(): android.content.Intent? {
        return try {
            if (googleSignInClient == null) {
                Log.e(TAG, "❌ Google Sign-In client is null")
                return null
            }
            
            val signInIntent = googleSignInClient!!.signInIntent
            Log.d(TAG, "🔐 Sign-In intent created successfully")
            signInIntent
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating sign-in intent", e)
            null
        }
    }
    
    /**
     * サインアウト
     */
    fun signOut() {
        googleSignInClient?.signOut()?.addOnCompleteListener {
            Log.d(TAG, "🔓 User signed out")
        }
    }
}

/**
 * 予定情報を保持するデータクラス
 */
data class EventInfo(
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val description: String
)

/**
 * カレンダー操作の結果を表すシールドクラス
 */
sealed class CalendarResult {
    data class Success(
        val title: String,
        val startTime: LocalDateTime,
        val calendarLink: String
    ) : CalendarResult()
    
    data class Error(val message: String) : CalendarResult()
} 