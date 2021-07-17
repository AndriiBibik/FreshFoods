package products.fresh.foods.database

import androidx.room.Embedded
import androidx.room.Relation

data class ProductAndExpiryDateWithNotifications(
    //parent
    @Embedded val expiryDateWithNotifications: ExpiryDateWithNotifications,
    @Relation(
        parentColumn = "product_id",
        entityColumn = "product_id"
    )
    //entity
    val product: Product
)