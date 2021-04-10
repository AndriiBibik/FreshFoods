package products.fresh.foods.productdetails

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import products.fresh.foods.database.ProductDatabaseDao

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
            //TODO Review this for assynchroness
            uiScope.launch {
                // image path null check
                p.product.image?.let { image ->
                    val bitmap = readBitmap(image)
                    bitmap?.let { value = it }
                }
            }
        }
    }

    val title = MediatorLiveData<String>().apply {
        addSource(productAndExpiryDate) { t ->
            t.product.title?.let {
                value = it
            }
        }
    }

    val expiryDate = MediatorLiveData<String>().apply {
        addSource(productAndExpiryDate) { e ->
            e.expiryDate.expiryDate.toString()?.let {
                value = it
            }
        }
    }

    // Read Bitmap from file by path
    private suspend fun readBitmap(path: String): Bitmap {
        return withContext(IO) {
            BitmapFactory.decodeFile(path)
        }
    }
}