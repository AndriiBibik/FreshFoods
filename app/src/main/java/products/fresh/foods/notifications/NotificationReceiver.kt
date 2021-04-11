package products.fresh.foods.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import products.fresh.foods.GoodFoodApp
import products.fresh.foods.R

class NotificationReceiver: BroadcastReceiver() {

    // notification manager
    private lateinit var notificationManager: NotificationManagerCompat

    override fun onReceive(context: Context?, intent: Intent?) {
        val notification = context?.let {
            // initializing notification manager
            notificationManager = NotificationManagerCompat.from(context)
            // build notification
            NotificationCompat.Builder(context, GoodFoodApp.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_carrot_down)
                .setContentTitle("Title")
                .setContentText("content text")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
        }
        notification?.let { notificationManager.notify(2, it) }
    }

}