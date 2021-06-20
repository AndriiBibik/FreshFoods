package products.fresh.foods.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import products.fresh.foods.GoodFoodApp

class NotificationButtonReceiver: BroadcastReceiver() {

    companion object {
        const val DIVIDER = "|"
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        // get ids of pending intents to disable all of them
        val ids = intent?.getLongArrayExtra(NotificationConstants.ALL_NOTIFICATIONS_KEY)

        // notification id to cancel this notification
        val expiryDateId = intent?.getLongExtra(NotificationConstants.EXPIRY_DATE_ID_KEY, -1L)

        // alarm system manager
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // notification manager
        val notificationManager = NotificationManagerCompat.from(context)

        // cancel all notifications for current expiry date
        ids?.forEach { id ->

            if (id != -1L) {
                // recreate pending intents for notification to cancel in alarm manager
                val intent = Intent(context, NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_ONE_SHOT)
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }

        }

        // cancel notification on which button was clicked - for current expiry date
        if (expiryDateId != null && expiryDateId != -1L) {
            notificationManager.cancel(expiryDateId.toInt())
        }

        // indicate in shared prefs to delete this expiry date and appropriate notifications when
        // app starts
        if (expiryDateId != null && expiryDateId != -1L) {
            val sharedPrefs = context.getSharedPreferences(GoodFoodApp.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            val idsString = sharedPrefs.getString(NotificationConstants.EXPIRY_DATES_IDS_TO_DELETE, "")
            val newIdsString = "$idsString$expiryDateId$DIVIDER"
            editor.putString(NotificationConstants.EXPIRY_DATES_IDS_TO_DELETE, newIdsString).apply()
            //TODO works "4|5|... now delete expiry dates and notifications when init view model"
            Log.v("xxxv", "to delete ids: $newIdsString")
        }
    }
}