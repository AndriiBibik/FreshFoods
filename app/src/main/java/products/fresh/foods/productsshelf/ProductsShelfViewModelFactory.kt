package products.fresh.foods.productsshelf

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import products.fresh.foods.database.ProductDatabaseDao

class ProductsShelfViewModelFactory(
    private val dataSource: ProductDatabaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductsShelfViewModel::class.java)) {
            return ProductsShelfViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}