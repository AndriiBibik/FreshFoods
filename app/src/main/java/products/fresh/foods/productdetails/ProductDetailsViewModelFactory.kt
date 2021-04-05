package products.fresh.foods.productdetails

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import products.fresh.foods.database.ProductDatabaseDao

class ProductDetailsViewModelFactory(
    private val expiryDateId: Long,
    private val dataSource: ProductDatabaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductDetailsViewModel::class.java)) {
            return ProductDetailsViewModel(expiryDateId, dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}