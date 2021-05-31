package products.fresh.foods.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Notification(
    @ColumnInfo(name="expiry_date_id") val expiryDateId: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="notification_id") var notificationId = 0L
}