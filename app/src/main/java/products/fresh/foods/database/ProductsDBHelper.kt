package products.fresh.foods.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class ProductsDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "GoodBy.db"

        private const val SQL_CREATE_PRODUCTS_TABLE =
            "CREATE TABLE ${ProductsContract.ProductsEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${ProductsContract.ProductsEntry.COLUMN_IMAGE} TEXT, " +
                    "${ProductsContract.ProductsEntry.COLUMN_TITLE} TEXT NOT NULL," +
                    "${ProductsContract.ProductsEntry.COLUMN_EXPIRY_DATE} TEXT NOT NULL)"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}