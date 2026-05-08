package com.hallisanthi.digital.data

import android.content.Context
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.AppNotification

object NotificationHelper {

    /** Fire-and-forget: insert a notification for the given user */
    suspend fun push(context: Context, userId: Long, type: String, title: String, body: String, refId: Long = 0) {
        if (userId <= 0) return
        val n = AppNotification(
            userId      = userId,
            type        = type,
            title       = title,
            body        = body,
            referenceId = refId,
            timestamp   = System.currentTimeMillis()
        )
        ProductDatabase.getDatabase(context).notificationDao().insert(n)
    }
}
