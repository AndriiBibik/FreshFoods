package products.fresh.foods.productsshelf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import products.fresh.foods.database.ProductDatabaseDao

class ProductsShelfViewModel(
    val databaseDao: ProductDatabaseDao, application: Application): AndroidViewModel(application) {

}