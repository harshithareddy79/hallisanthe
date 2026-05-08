package com.hallisanthi.digital.data

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

object VoiceSearchHelper {
    const val REQUEST_CODE = 9001

    /** Launch Android's built-in speech recogniser */
    fun startListening(activity: Activity) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a product name or category…")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    /** Extract the best result from onActivityResult data */
    fun extractResult(data: Intent?): String? {
        return data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
    }
}
