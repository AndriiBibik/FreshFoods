package products.fresh.foods.database

import androidx.room.Embedded
import androidx.room.Relation

data class ProductAndExpiryDate(
    //parent
    @Embedded val expiryDate: ExpiryDate,
    @Relation(
        parentColumn = "product_id",
        entityColumn = "product_id"
    )
    //entity
    val product: Product
)