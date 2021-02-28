package products.fresh.foods.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_table")
data class Product(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var productId: Long,

    @ColumnInfo(name = "image")
    val image: String?,

    @ColumnInfo(name = "title")
    var title: String,

    @ColumnInfo(name = "expiry_date")
    var expiryDate: Int
)