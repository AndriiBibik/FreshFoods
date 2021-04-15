package products.fresh.foods.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import products.fresh.foods.GoodFoodApp
import products.fresh.foods.R
import products.fresh.foods.utils.ProductUtils

class NotificationReceiver: BroadcastReceiver() {

    // notification manager
    private lateinit var notificationManager: NotificationManagerCompat

    override fun onReceive(context: Context?, intent: Intent?) {
        val notification = context?.let { context ->
            intent?.let { intent ->
                // initializing notification manager
                notificationManager = NotificationManagerCompat.from(context)

                val productTitle = intent.getStringExtra(NotificationConstants.TITLE_EXTRAS_ID)
                val expiryDate = intent.getIntExtra(NotificationConstants.EXPIRY_DATE_EXTRAS_ID, -1)
                val itemImagePath = intent.getStringExtra(NotificationConstants.IMAGE_PATH_EXTRAS_ID)
                // build notification
                NotificationCompat.Builder(context, GoodFoodApp.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_carrot_down)
                    .setContentText(productTitle)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .apply {
                        if (expiryDate != -1) {
                            val timeLeft = ProductUtils.convertExpiryDateToTimeLeft(expiryDate)
                            val daysN = ProductUtils.millisecondsIntoDays(timeLeft)
                            val hoursN = ProductUtils.millisecondsIntoHours(timeLeft)
                            val minN = ProductUtils.millisecondsIntoMinutes(timeLeft)
                            // TODO to use later string resources instead of hardcoded text
                            val timeLeftText = String.format("%d d %02dh:%02dmin to eat", daysN, hoursN, minN)
                            // TODO try to change contentTExt color to appropriate one
                            setContentTitle(timeLeftText)
                        }
                        if (itemImagePath != null) {
                            val density = context.resources.displayMetrics.density
                            val size = when {
                                density <= 0.75f -> 48
                                density == 1.0f -> 64
                                density == 1.5f -> 96
                                density == 2.0f -> 128
                                else -> 192
                            }
                            val bitmap = BitmapFactory.decodeFile(itemImagePath)
                            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, size, size, false)
                            setLargeIcon(resizedBitmap)
                        }
                    }
                    .build()
            }
        }
        notification?.let { notification ->
            intent?.let { intent ->
                val id = intent.getLongExtra(NotificationConstants.EXPIRY_DATE_ID_EXTRAS_ID, -1)
                if (id != -1L)
                    notificationManager.notify(id.toInt(), notification)
            }

        }
    }

}