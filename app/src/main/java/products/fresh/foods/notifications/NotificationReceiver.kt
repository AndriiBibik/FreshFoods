package products.fresh.foods.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import products.fresh.foods.GoodFoodApp
import products.fresh.foods.MainActivity
import products.fresh.foods.R
import products.fresh.foods.notifications.NotificationConstants.Companion.DIVIDER
import products.fresh.foods.notifications.NotificationConstants.Companion.NOTIFICATIONS_TO_DELETE_KEY
import products.fresh.foods.notifications.NotificationConstants.Companion.NOTIFICATION_SHARED_PREFERENCES
import products.fresh.foods.utils.ProductUtils

class NotificationReceiver : BroadcastReceiver() {

    // notification manager
    private lateinit var notificationManager: NotificationManagerCompat

    override fun onReceive(context: Context?, intent: Intent?) {
        var notificationId = -1L
        val notification = context?.let { context ->
            intent?.let { intent ->
                // initializing notification manager
                notificationManager = NotificationManagerCompat.from(context)

                val res = context.resources

                // get extras from intent
                val productTitle = intent.getStringExtra(NotificationConstants.TITLE_KEY)
                val expiryDate = intent.getIntExtra(NotificationConstants.EXPIRY_DATE_KEY, -1)
                val expiryDateId = intent.getLongExtra(NotificationConstants.EXPIRY_DATE_ID_KEY, -1L)
                val itemImagePath = intent.getStringExtra(NotificationConstants.IMAGE_PATH_KEY)
                val notificationsIds = intent.getLongArrayExtra(NotificationConstants.ALL_NOTIFICATIONS_KEY)

                notificationId = intent.getLongExtra(NotificationConstants.NOTIFICATION_ID_KEY, -1L)

                // on notification click intent
                val onClickIntent = Intent(context, MainActivity::class.java)
                val onClickPendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, 0)

                // button intent
                val buttonIntent = Intent(context, NotificationButtonReceiver::class.java)
                buttonIntent.putExtra(NotificationConstants.EXPIRY_DATE_ID_KEY, expiryDateId)
                buttonIntent.putExtra(NotificationConstants.ALL_NOTIFICATIONS_KEY, notificationsIds)
                val buttonPendingIntent = PendingIntent.getBroadcast(context, expiryDateId.toInt(), buttonIntent, PendingIntent.FLAG_ONE_SHOT)

                // button text
                val buttonText = context.resources.getString(R.string.notification_button_eaten)

                // build notification
                NotificationCompat.Builder(context, GoodFoodApp.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_cherries)
                    .setContentText(productTitle)
                    .setAutoCancel(true)
                    .setContentIntent(onClickPendingIntent)
                    .addAction(0, buttonText, buttonPendingIntent)
                    .apply {
                        //TODO int the future review if notification grouping work
                        //TODO (if needed) And implement it right!
                        //TODO but consider EATEN button. Maybe grouping is not necessary
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                            setGroup(NotificationConstants.GROUP)
                        }
                        if (expiryDate != -1) {
                            val timeLeft = ProductUtils.convertExpiryDateToTimeLeft(expiryDate)
                            val daysN = ProductUtils.millisecondsIntoDays(timeLeft)
                            val hoursN = ProductUtils.millisecondsIntoHours(timeLeft)
                            val minN = ProductUtils.millisecondsIntoMinutes(timeLeft)
                            val timeLeftText = String.format(
                                "%d ${res.getString(R.string.days_shortcut)} " +
                                        "%02d${res.getString(R.string.hours_shortcut)} " +
                                        "%02d${res.getString(R.string.minutes_shortcut)} " +
                                        "${res.getString(R.string.to_consume_text)}",
                                daysN,
                                hoursN,
                                minN
                            )
                            // TODO try to change contentTExt color to appropriate one
                            setContentTitle(timeLeftText)
                        }
                        if (itemImagePath != null) {
                            val density = res.displayMetrics.density
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
            val id =
                intent?.getLongExtra(NotificationConstants.EXPIRY_DATE_ID_KEY, -1L)
            if (id != null && id != -1L) {
                notificationManager.notify(id.toInt(), notification)
                // save notification id into shared prefs to delete it in the app
                // after it was shown by line above
                if (notificationId != -1L) {
                    val sp = context.getSharedPreferences(NOTIFICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE)
                    val idsToDelete = "${sp.getString(NOTIFICATIONS_TO_DELETE_KEY, "")}$notificationId$DIVIDER"
                    sp.edit().putString(NOTIFICATIONS_TO_DELETE_KEY, idsToDelete).apply()
                }
            }
        }
    }
}