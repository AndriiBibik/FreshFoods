package products.fresh.foods.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="products_table")
data class Product(
    val image: String?,
    val thumbnail: String?,
    val title: String
) {
    @PrimaryKey(autoGenerate=true)
    @ColumnInfo(name="product_id") var productId: Long = 0L
}