package products.fresh.foods.productdetails

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import products.fresh.foods.R
import products.fresh.foods.database.ProductDatabaseDao
import products.fresh.foods.utils.ProductUtils

class ProductDetailsViewModel(
    private val expiryDateId: Long,  private val databaseDao: ProductDatabaseDao, application: Application
) : AndroidViewModel(application) {

    private val productAndExpiryDate = databaseDao.getProductAndExpiryDate(expiryDateId)

    // ProductDetailsViewModel job
    private val viewModelJob = Job()

    // ViewModel scope
    private val uiScope = CoroutineScope(Main + viewModelJob)

    override fun onCleared() {
        viewModelJob.cancel()
        super.onCleared()
    }

    val image = MediatorLiveData<Bitmap>().apply {
        addSource(productAndExpiryDate) { p ->
            uiScope.launch {
                // image path null check
                p.product.image?.let { image ->
                    val bitmap = ProductUtils.readBitmapInIO(image)
                    bitmap?.let { value = it }
                }
            }
        }
    }

    val title = MediatorLiveData<String>().apply {
        addSource(productAndExpiryDate) { p ->
            p.product.title?.let {
                value = it
            }
        }
    }

    val expiryDate = MediatorLiveData<String>().apply {
        addSource(productAndExpiryDate) { p ->
            value = "${application.resources.getString(R.string.expiry_date_text)}: " +
                    "${ProductUtils.convertExpiryDateForUi(p.expiryDate.expiryDate)}"
        }
    }

    val daysLeft = MediatorLiveData<String>().apply {
        addSource(productAndExpiryDate) { p ->
            value = ProductUtils.buildTimeLeftText(p.expiryDate.expiryDate)
        }
    }

    val daysLeftColor = MediatorLiveData<Int>().apply {
        addSource(productAndExpiryDate) { p ->
            value = ProductUtils.getTextLeftColor(p.expiryDate.expiryDate)
        }
    }
}