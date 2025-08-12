package com.seo4d696b75.android.glance_widget_demo.widget

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.seo4d696b75.android.glance_widget_demo.core.calendar.GoogleCalendarManager
import com.seo4d696b75.android.glance_widget_demo.core.calendar.CalendarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

class VoiceInputActivity : Activity() {
    companion object {
        const val EXTRA_VOICE_RESULT = "voice_result"
        const val EXTRA_CALENDAR_RESULT = "calendar_result"
        private const val PERMISSION_REQUEST_CODE = 123
        private const val SPEECH_REQUEST_CODE = 456
        private const val GOOGLE_SIGN_IN_REQUEST_CODE = 789
    }

    private lateinit var calendarManager: GoogleCalendarManager
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("VoiceInputActivity", " Voice input activity started")

        calendarManager = GoogleCalendarManager(this)
        calendarManager.initializeGoogleSignIn()

        logSystemLanguageSettings()

        if (checkAudioPermission()) {
            startVoiceRecognition()
        } else {
            requestAudioPermission()
        }
    }

    private fun logSystemLanguageSettings() {
        val locale = resources.configuration.locales[0]
        val systemLanguage = locale.language
        val systemCountry = locale.country
        android.util.Log.d("VoiceInputActivity", " System Language: $systemLanguage")
        android.util.Log.d("VoiceInputActivity", " System Country: $systemCountry")
        android.util.Log.d("VoiceInputActivity", " Full Locale: $locale")
        if (systemLanguage == "ja") {
            android.util.Log.d("VoiceInputActivity", " System is set to Japanese")
        } else {
            android.util.Log.w("VoiceInputActivity", " System is NOT set to Japanese (current: $systemLanguage)")
        }
    }

    private fun checkAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "音声入力にはマイク権限が必要です", Toast.LENGTH_LONG).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition()
            } else {
                Toast.makeText(this, "音声入力にはマイク権限が必要です", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Googleカレンダーに追加したい予定を話してください（例：明日午後3時に会議、今日午前10時に打ち合わせなど）")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra("android.speech.extra.USE_LEGACY_LANGUAGE_MODEL", true)
            }
            android.util.Log.d("VoiceInputActivity", " Starting Japanese voice recognition with ja-JP locale")
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            android.util.Log.e("VoiceInputActivity", " Error starting voice recognition", e)
            Toast.makeText(this, "音声認識を開始できませんでした", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun processVoiceInputForCalendar(spokenText: String) {
        android.util.Log.d("VoiceInputActivity", "📅 Processing voice input for calendar: '$spokenText'")

        val account = calendarManager.getCurrentAccount()
        val hasScope = calendarManager.hasCalendarScope(account)
        if (account == null) {
            val signInIntent = calendarManager.getSignInIntent()
            if (signInIntent == null) {
                Toast.makeText(this, "Googleサインインを初期化できませんでした", Toast.LENGTH_SHORT).show()
                finish(); return
            }
            intent.putExtra("saved_voice_input", spokenText)
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
            return
        }
        if (!hasScope) {
            android.util.Log.d("VoiceInputActivity", " Requesting additional Calendar scope permission")
            intent.putExtra("saved_voice_input", spokenText)
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_SIGN_IN_REQUEST_CODE,
                account,
                Scope("https://www.googleapis.com/auth/calendar")
            )
            return
        }
        addEventToCalendar(spokenText)
    }

    private fun addEventToCalendar(spokenText: String) {
        coroutineScope.launch {
            try {
                android.util.Log.d("VoiceInputActivity", "Adding event to Google Calendar: '$spokenText'")
                Toast.makeText(this@VoiceInputActivity, "カレンダーに予定を追加中...", Toast.LENGTH_SHORT).show()
                val result = calendarManager.addEventFromVoiceInput(spokenText)
                when (result) {
                    is CalendarResult.Success -> {
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_VOICE_RESULT, spokenText)
                            putExtra(EXTRA_CALENDAR_RESULT, "success")
                            putExtra("event_title", result.title)
                            putExtra("event_time", result.startTime.toString())
                            putExtra("calendar_link", result.calendarLink)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        val broadcastIntent = Intent("com.seo4d696b75.android.glance_widget_demo.VOICE_INPUT_RESULT").apply {
                            putExtra(Intent.EXTRA_TEXT, spokenText)
                            putExtra("calendar_result", "success")
                            putExtra("event_title", result.title)
                        }
                        sendBroadcast(broadcastIntent)
                        Toast.makeText(this@VoiceInputActivity, "予定を追加しました: ${result.title}", Toast.LENGTH_LONG).show()
                    }
                    is CalendarResult.Error -> {
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_VOICE_RESULT, spokenText)
                            putExtra(EXTRA_CALENDAR_RESULT, "error")
                            putExtra("error_message", result.message)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        Toast.makeText(this@VoiceInputActivity, "予定の追加に失敗しました: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VoiceInputActivity", "Error processing calendar event", e)
                Toast.makeText(this@VoiceInputActivity, "カレンダー処理中にエラーが発生しました", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SPEECH_REQUEST_CODE -> {
                android.util.Log.d("VoiceInputActivity", " Speech recognition result received")
                android.util.Log.d("VoiceInputActivity", "  - Result code: $resultCode")
                android.util.Log.d("VoiceInputActivity", "  - Data: $data")
                if (resultCode == Activity.RESULT_OK) {
                    val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = results?.get(0)
                    android.util.Log.d("VoiceInputActivity", " Raw results: $results")
                    android.util.Log.d("VoiceInputActivity", " Selected text: '$spokenText'")
                    if (!spokenText.isNullOrEmpty()) {
                        android.util.Log.d("VoiceInputActivity", " 日本語音声入力受信: '$spokenText'")
                        if (results != null && results.size > 1) {
                            android.util.Log.d("VoiceInputActivity", " 候補結果:")
                            results.forEachIndexed { index, result ->
                                android.util.Log.d("VoiceInputActivity", "  ${index + 1}. $result")
                            }
                        }
                        processVoiceInputForCalendar(spokenText)
                    } else {
                        android.util.Log.w("VoiceInputActivity", "Spoken text is null or empty")
                        Toast.makeText(this, "音声を認識できませんでした", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                } else {
                    android.util.Log.w("VoiceInputActivity", "Speech recognition failed with result code: $resultCode")
                    Toast.makeText(this, "音声入力がキャンセルされました", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
            GOOGLE_SIGN_IN_REQUEST_CODE -> {
                android.util.Log.d("VoiceInputActivity", " Google Sign-In result received")
                android.util.Log.d("VoiceInputActivity", "  - Result code: $resultCode")
                android.util.Log.d("VoiceInputActivity", "  - Data: $data")
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    android.util.Log.d("VoiceInputActivity", " Signed in as: ${account.email}")
                    val savedVoiceInput = intent.getStringExtra("saved_voice_input")
                    if (!savedVoiceInput.isNullOrEmpty()) {
                        addEventToCalendar(savedVoiceInput)
                    } else {
                        Toast.makeText(this@VoiceInputActivity, "音声入力データが見つかりません", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: ApiException) {
                    val status = e.statusCode
                    val message = when (status) {
                        CommonStatusCodes.CANCELED -> "サインインがキャンセルされました"
                        CommonStatusCodes.DEVELOPER_ERROR -> "設定エラー（パッケージ名やSHA-1の不一致など）"
                        CommonStatusCodes.NETWORK_ERROR -> "ネットワークエラー"
                        CommonStatusCodes.SIGN_IN_REQUIRED -> "サインインが必要です"
                        else -> "サインインに失敗しました (code=$status)"
                    }
                    android.util.Log.w("VoiceInputActivity", " Google Sign-In failed: code=$status, ${e.localizedMessage}")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        android.util.Log.d("VoiceInputActivity", "🎤 Voice input cancelled by back press")
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
} 