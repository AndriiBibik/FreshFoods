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

    //product
    @Update
    suspend fun update(product: Product)

    //expiry date for product
    @Update
    suspend fun update(expiryDate: ExpiryDate)

    @Query("SELECT * FROM products_table WHERE product_id = :id")
    suspend fun getProductAndExpiryDate(id: Long): ProductAndExpiryDate?

    /* select all products as it is */
    @Query("SELECT * FROM products_table")
    fun getAllProductsAndExpiryDates() : List<ProductAndExpiryDate>

    /* select all products desc by id(as well as by adding time) */
    @Query("SELECT * FROM products_table ORDER BY product_id DESC")
    fun getAllProductsAndExpiryDatesDesc() : List<ProductAndExpiryDate>

//    /* select all products sorted by expiry date desc */
//    @Query("SELECT * FROM products_table ORDER BY expiry_date DESC")
//    fun getAllProductsAndExpiryDatesDescByExpiryDate() : List<ProductAndExpiryDate>
//
//    /* select all products sorted by expiry date asc */
//    @Query("SELECT * FROM products_table ORDER BY expiry_date ASC")
//    fun getAllProductsAndExpiryDatesAscByExpiryDate() : List<ProductAndExpiryDate>

}