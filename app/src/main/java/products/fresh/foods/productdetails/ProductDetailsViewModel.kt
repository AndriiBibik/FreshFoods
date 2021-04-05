package products.fresh.foods.productdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import products.fresh.foods.database.ProductDatabaseDao

class ProductDetailsViewModel(
    private val expiryDateId: Long,  private val databaseDao: ProductDatabaseDao, application: Application
) : AndroidViewModel(application) {

}