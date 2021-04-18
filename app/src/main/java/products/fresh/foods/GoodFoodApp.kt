package products.fresh.foods

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class GoodFoodApp : Application() {
    companion object {
        lateinit var instance: GoodFoodApp private set
        const val CHANNEL_ID = "channelId"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                resources.getString(R.string.notifications_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = resources.getString(R.string.notifications_channel_description)

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}