package products.fresh.foods.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName="notification_table",
    foreignKeys = [ForeignKey(
        entity = ExpiryDate::class,
        parentColumns = arrayOf("expiry_date_id"),
        childColumns = arrayOf("expiry_date_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Notification(
    @ColumnInfo(name="expiry_date_id") val expiryDateId: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="notification_id") var notificationId = 0L
}