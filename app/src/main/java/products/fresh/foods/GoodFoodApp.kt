package products.fresh.foods

import android.app.Application
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
                "Save this product!",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "This is notification channel"

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}