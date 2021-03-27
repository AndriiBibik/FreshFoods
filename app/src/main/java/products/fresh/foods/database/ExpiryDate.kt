package products.fresh.foods.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="expiry_date_table")
data class ExpiryDate(
    @ColumnInfo(name="product_id") val productId: Long,
    @ColumnInfo(name="expiry_date") val expiryDate: Int
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="expiry_date_id") var expiryDateId: Long = 0L
}