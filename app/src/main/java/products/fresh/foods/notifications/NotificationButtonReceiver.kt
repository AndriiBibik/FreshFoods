package products.fresh.foods.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class NotificationButtonReceiver: BroadcastReceiver() {

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
    }
}