package products.fresh.foods.database

import androidx.room.Embedded
import androidx.room.Relation

data class ExpiryDateWithNotifications(
    @Embedded val expiryDate: ExpiryDate,
    @Relation(
        parentColumn = "expiry_date_id",
        entityColumn = "expiry_date_id"
    )
    val notifications: List<Notification>
) {}