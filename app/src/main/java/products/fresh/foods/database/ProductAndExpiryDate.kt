package products.fresh.foods.database

import androidx.room.Embedded
import androidx.room.Relation

data class ProductAndExpiryDate(
    @Embedded val product: Product,
    @Relation(
        parentColumn = "product_id",
        entityColumn = "product_id"
    )
    val expiryDate: ExpiryDate
)