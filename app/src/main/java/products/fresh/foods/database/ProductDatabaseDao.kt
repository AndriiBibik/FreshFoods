package products.fresh.foods.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductDatabaseDao {

    //product
    @Insert
    suspend fun insert(product: Product): Long

    //expiry date for product
    @Insert
    suspend fun insert(expiryDate: ExpiryDate): Long

    //notification
    @Insert
    suspend fun insert(notification: Notification): Long

    //product
    @Update
    suspend fun update(product: Product)

    //expiry date for product
    @Update
    suspend fun update(expiryDate: ExpiryDate)

    //delete expiry date
    @Delete
    suspend fun delete(expiryDate: ExpiryDate)

    //delete notification
    @Delete
    suspend fun delete(notification: Notification)

    //delete expiry date by id
    @Query("DELETE FROM expiry_date_table WHERE expiry_date_id = :id")
    suspend fun deleteExpiryDateById(id: Long)

    //delete all notifications for expiry date id
    @Query("DELETE FROM notification_table WHERE expiry_date_id = :id")
    suspend fun deleteNotificationsForExpiryDate(id: Long)

    //delete all expiry dates when expired for days more than ... in selected in options
    //in this DAO case when expiry date < then calculated delete date
    @Query("DELETE FROM expiry_date_table WHERE expiry_date < :deleteDate")
    suspend fun deleteAllExpiryDatesConsideringDeleteDate(deleteDate: Int)

    // getting all notifications
    @Query("SELECT * FROM notification_table")
    fun getNotifications(): LiveData<List<Notification>>

    //
    @Query("SELECT * FROM notification_table WHERE expiry_date_id = :id")
    suspend fun getNotificationsByExpiryDate(id: Long): List<Notification>

    @Query("SELECT * FROM expiry_date_table WHERE expiry_date_id = :id")
    fun getProductAndExpiryDate(id: Long): LiveData<ProductAndExpiryDate>

    // query to get info from "products","expiry_date","notifications" tables
    @Transaction
    @Query("SELECT * FROM expiry_date_table WHERE expiry_date_id = :id")
    fun getProductAndExpiryDateWithNotifications(id: Long): LiveData<ProductAndExpiryDateWithNotifications>

    //for product title suggestions
    @Query("SELECT * FROM products_table ORDER BY title ASC")
    fun getAllProductsByTitleAsc(): LiveData<List<Product>>

    //position 0 - time left asc
    @Query("SELECT * FROM expiry_date_table ORDER BY expiry_date ASC")
    fun getAllProductsAndExpiryDatesWithNotificationsByTimeLeftAsc(): LiveData<List<ProductAndExpiryDateWithNotifications>>

    //position 1 - time left desc
    @Query("SELECT * FROM expiry_date_table ORDER BY expiry_date DESC")
    fun getAllProductsAndExpiryDatesWithNotificationsByTimeLeftDesc(): LiveData<List<ProductAndExpiryDateWithNotifications>>

    //position 2 - time added(id) desc
    @Query("SELECT * FROM expiry_date_table ORDER BY expiry_date_id DESC")
    fun getAllProductsAndExpiryDatesWithNotificationsDesc() : LiveData<List<ProductAndExpiryDateWithNotifications>>

    //position 3 - time added(id) asc
    @Query("SELECT * FROM expiry_date_table")
    fun getAllProductsAndExpiryDatesWithNotifications(): LiveData<List<ProductAndExpiryDateWithNotifications>>

    //get all notifications for expiration date the ROOM way
    @Transaction
    @Query("SELECT * FROM expiry_date_table WHERE expiry_date_id = :expiryDateId")
    suspend fun getExpiryDateWithNotifications(expiryDateId: Long): List<ExpiryDateWithNotifications>

}