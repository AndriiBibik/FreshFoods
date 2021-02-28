package products.fresh.foods.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductDatabaseDao {

    @Insert
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Query("SELECT * from products_table WHERE id = :id")
    suspend fun get(id: Long): Product?

    @Query("DELETE FROM products_table")
    suspend fun clear()

    /* select all products as it is */
    @Query("SELECT * FROM products_table")
    suspend fun getAllProducts() : LiveData<List<Product>>

    /* select all products desc by id(as well as by adding time) */
    @Query("SELECT * FROM products_table ORDER BY id DESC")
    suspend fun getAllProductsDesc() : LiveData<List<Product>>

    /* select all products sorted by expiry date desc */
    @Query("SELECT * FROM products_table ORDER BY expiry_date DESC")
    suspend fun getAllProductsDescByExpiryDate() : LiveData<List<Product>>

    /* select all products sorted by expiry date asc */
    @Query("SELECT * FROM products_table ORDER BY expiry_date ASC")
    suspend fun getAllProductsAscByExpiryDate() : LiveData<List<Product>>

}