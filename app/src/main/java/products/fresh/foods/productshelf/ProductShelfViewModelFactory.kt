package products.fresh.foods.productshelf

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import products.fresh.foods.database.ProductDatabaseDao

class ProductShelfViewModelFactory(
    private val dataSource: ProductDatabaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductShelfViewModel::class.java)) {
            return ProductShelfViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}