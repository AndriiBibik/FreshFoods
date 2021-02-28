package products.fresh.foods.database

import android.provider.BaseColumns

object ProductsContract {

    object ProductsEntry : BaseColumns {
        const val TABLE_NAME = "products"
        const val COLUMN_IMAGE = "image"
        const val COLUMN_TITLE = "title"
        const val COLUMN_EXPIRY_DATE = "expiry_date"
    }
}