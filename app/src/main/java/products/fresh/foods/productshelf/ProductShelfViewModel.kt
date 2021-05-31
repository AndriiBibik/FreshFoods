package products.fresh.foods.productshelf

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.*
import products.fresh.foods.GoodFoodApp
import products.fresh.foods.R
import products.fresh.foods.database.ExpiryDate
import products.fresh.foods.database.Notification
import products.fresh.foods.database.Product
import products.fresh.foods.database.ProductDatabaseDao
import products.fresh.foods.notifications.NotificationConstants
import products.fresh.foods.notifications.NotificationReceiver
import products.fresh.foods.utils.ProductUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.exp

class ProductShelfViewModel(
    private val databaseDao: ProductDatabaseDao, application: Application
) : AndroidViewModel(application) {

    companion object {
        const val APP_SHARED_PREFERENCES = "app_shared_preferences"
        const val SPINNER_ID_KEY = "spinner_id"
        const val IS_GRiD_KEY = "is_greed"
        const val SPINNER_ID_TIME_LEFT_ASC = 0
        const val SPINNER_ID_TIME_LEFT_DESC = 1
        const val SPINNER_ID_TIME_ADDED_DESC = 2
        const val SPINNER_ID_TIME_ADDED_ASC = 3
        private const val MAX_BITMAP_SIZE_PX = 512
        private const val SUFFIX_LARGER_IMAGE = ""
        private const val SUFFIX_THUMBNAIL_IMAGE = "_thumbnail"
        private const val IMAGE_QUALITY = 90
        const val SPAN_ONE = 1
    }

    // for shared preferences
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences(
        APP_SHARED_PREFERENCES, Context.MODE_PRIVATE
    )

    // current sort spinner position
    val sortSpinnerPos =
        MutableLiveData<Int>(sharedPreferences.getInt(SPINNER_ID_KEY, SPINNER_ID_TIME_ADDED_DESC))

    fun setSortSpinnerPos(pos: Int) {
        sortSpinnerPos.value = pos
        sharedPreferences.edit().putInt(SPINNER_ID_KEY, pos).apply()
    }

    // list of product for suggestions in enter product title field
    val productsList = databaseDao.getAllProductsByTitleAsc()

    // product if selected from suggested list
    val productSelected = MutableLiveData<Product>()

    // actual sorted list that we going to use. This list is based on "list" above and "sortType"
    val sortedList = Transformations.switchMap(sortSpinnerPos) { pos ->
        when (pos) {
            SPINNER_ID_TIME_LEFT_ASC -> databaseDao.getAllProductsAndExpiryDatesByTimeLeftAsc()//0
            SPINNER_ID_TIME_LEFT_DESC -> databaseDao.getAllProductsAndExpiryDatesByTimeLeftDesc()//1
            SPINNER_ID_TIME_ADDED_ASC -> databaseDao.getAllProductsAndExpiryDatesDesc()//3
            else -> databaseDao.getAllProductsAndExpiryDates() //2
        }
    }

    // ViewModel Job
    private val viewModelJob = Job()

    // coroutines scope
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Image Data
    private val _productImages = MutableLiveData<ImageBitmaps>()
    val productImages: LiveData<ImageBitmaps>
        get() = _productImages

    fun clearProductImages() {
        _productImages.value = null
    }

    // Product title/description
    private val _productTitle = MutableLiveData<String>()
    val productTitle: LiveData<String>
        get() = _productTitle

    // expiry date
    private val _expiryDate = MutableLiveData<String>()
    val expiryDate: LiveData<String>
        get() = _expiryDate

    fun resetExpiryDate() {
        _expiryDate.value = null
    }

    // current photo path
    private lateinit var currentPhotoPath: String

    // list in full screen or not
    private val _isListFullScreen = MutableLiveData<Boolean>(false)
    val isListFullScreen: LiveData<Boolean>
        get() = _isListFullScreen

    // to indicate if image is currently processing or not
    private val _isImageProcessing = MutableLiveData<Boolean>(false)
    val isImageProcessing: LiveData<Boolean>
        get() = _isImageProcessing

    // variable to hold info if list viewed as greed or list
    var isGrid: Boolean = sharedPreferences.getBoolean(IS_GRiD_KEY, false)
        set(isGrid) {
            field = isGrid
            sharedPreferences.edit().putBoolean(IS_GRiD_KEY, isGrid).apply()
        }

    // to indicate when title and expiry date is "grabbed" for further processing to put product into
    // the database
    private val _areTitleExpiryDateNoNeeded = MutableLiveData<Boolean>()
    val areTitleExpiryDateNoNeeded: LiveData<Boolean>
        get() = _areTitleExpiryDateNoNeeded

    // to notify if user wants to
    var toNotify = true

    // item touch helper callback to remove product from db when swipe
    val itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.RIGHT or
                    ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                    .addBackgroundColor(ContextCompat.getColor(application, R.color.itemBackground))
                    .addActionIcon(R.drawable.ic_cookie_delete)
                    // apply for list layout manager only
                    .apply {
                        if (!isGrid) {
                            this.addSwipeLeftLabel(application.resources.getString(R.string.delete_text_in_item))
                            this.setSwipeLeftLabelColor(
                                ContextCompat.getColor(
                                    application,
                                    R.color.white
                                )
                            )
                            this.setSwipeLeftLabelTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                application.resources.getDimension(R.dimen.product_list_item_delete_text_size)
                            )
                            this.addSwipeRightLabel(application.resources.getString(R.string.delete_text_in_item))
                            this.setSwipeRightLabelColor(
                                ContextCompat.getColor(
                                    application,
                                    R.color.white
                                )
                            )
                            this.setSwipeRightLabelTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                application.resources.getDimension(R.dimen.product_list_item_delete_text_size)
                            )
                        }
                    }
                    .create()
                    .decorate();
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val ed = sortedList.value?.get(pos)?.expiryDate?.let {
                    uiScope.launch {
                        delete(it)
                        //TODO delete this Toast and Clear AlarmManager notifications for this expiry date
                        Toast.makeText(application, it.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    // to delete Expiry Date from database
    private suspend fun delete(expiryDate: ExpiryDate) {
        withContext(Dispatchers.IO) {
            databaseDao.delete(expiryDate)
        }
    }

    // to calculate number of spans(columns)
    private fun calculateSpanCount(containerWidth: Int): Int {
        val columnWidth = getApplication<Application>().resources.let {
            (it.getDimension(R.dimen.products_grid_item_image_width)
                    + it.getDimension(R.dimen.product_grid_item_margin) * 2).toInt()
        }
        return containerWidth / columnWidth
    }

    // get number of spans
    fun getNumberOfSpans(containerWidth: Int): Int {
        return when (isGrid) {
            true -> calculateSpanCount(containerWidth)
            false -> SPAN_ONE
        }
    }

    ///////////// working with product image /////////////
    // to prepare images from path

    fun processImages() {

        uiScope.launch {
            // image started processing
            _isImageProcessing.value = true

            val imageBitmaps = prepareImageBitmaps(currentPhotoPath)
            imageBitmaps?.let {
                _productImages.value = it
            }
            // image processed..
            _isImageProcessing.value = false
        }
    }

    private suspend fun prepareImageBitmaps(photoPath: String): ImageBitmaps? {

        return withContext(Dispatchers.IO) {
            // thumbnail image size
            val thumbnailSize = getDimenDp(R.dimen.products_list_item_image_width)

            // larger bitmap size
            val maxBitmapSize = getMaxBitmapSize()

            // getting larger bitmap
            var maxSizeBitmap = getSavedBitmapScaled(photoPath, maxBitmapSize, maxBitmapSize)

            // getting thumbnail bitmap
            var listItemBitmap = getSavedBitmapScaled(photoPath, thumbnailSize!!, thumbnailSize)

            // rotate larger bitmap if there is a need to
            maxSizeBitmap = rotateBitmapIfNeeded(maxSizeBitmap, photoPath)

            // rotate thumbnail bitmap if there is a need to
            listItemBitmap = rotateBitmapIfNeeded(listItemBitmap, photoPath)

            // delete image file, all processing is done
            deleteImage(photoPath)

            listItemBitmap?.let {
                maxSizeBitmap?.let {
                    ImageBitmaps(listItemBitmap, maxSizeBitmap)
                }
            }
        }
    }

    // delete image having a path
    private fun deleteImage(photoPath: String): Boolean {
        val file = File(photoPath)
        return if (file.exists())
            file.delete()
        else
            false
    }

    // this function primarily is for getting layout imageViews size to prepare bitmap
    // 1dp = 1px (mdpi or 160 ppi)
    private fun getDimenDp(resourceId: Int) =
        getApplication<Application>().resources?.let { resources ->
            (resources.getDimension(resourceId) / resources.displayMetrics.density).toInt()
        }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap?, photoPath: String): Bitmap? {
        val exifInterface = ExifInterface(photoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        );
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270)
            else -> bitmap
        }
    }

    private fun rotateImage(img: Bitmap?, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg =
            img?.let { Bitmap.createBitmap(it, 0, 0, img.width, img.height, matrix, true) }
        img?.recycle()
        return rotatedImg
    }

    private fun getSmallestDeviceWidthDp(): Int {

        val widthDp =
            (getApplication<Application>().resources.displayMetrics.widthPixels / getApplication<Application>().resources.displayMetrics.density).toInt()

        val heightDp =
            (getApplication<Application>().resources.displayMetrics.heightPixels / getApplication<Application>().resources.displayMetrics.density).toInt()

        return when (widthDp <= heightDp) {
            true -> widthDp
            false -> heightDp
        }
    }

    // size of bitmap with highest resolution needed
    private fun getMaxBitmapSize(): Int {
        val size = getSmallestDeviceWidthDp()
        return when (size <= MAX_BITMAP_SIZE_PX) {
            true -> size
            false -> MAX_BITMAP_SIZE_PX
        }
    }

    private fun getSavedBitmapScaled(photoPath: String, width: Int, height: Int): Bitmap? {
        // Get the dimensions of the View

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(photoPath, this)

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(outWidth / width, outHeight / height))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        return BitmapFactory.decodeFile(photoPath, bmOptions)
            ?.let { bitmap ->
                val smallestSize = when (bitmap.width <= bitmap.height) {
                    true -> bitmap.width
                    false -> bitmap.height
                }
                // actual return
                ThumbnailUtils.extractThumbnail(bitmap, smallestSize, smallestSize)
            }
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val imageName: String = generateImageName()
        val storageDir: File? =
            getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            imageName, /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).also { file ->
            currentPhotoPath = file.absolutePath
        }
    }
    //
    //

    // check if we working with new product
    fun isUsingNewProduct(): Boolean {
        return productSelected.value == null || (productSelected.value != null &&
                (_productImages.value != null || !_productTitle.value.equals(productSelected.value?.title)))
    }

    // for "put product" button in fragment
    fun onPutProduct() {
        uiScope.launch {

            // new product
            if ( isUsingNewProduct() ) {

                // "grab" title and expiry date for further processing
                val title = _productTitle.value
                val expiryDate = _expiryDate.value?.let { expiryDate ->
                    ProductUtils.convertExpiryDateForDatabase(expiryDate)
                }
                // now above values not needed in the Ui and in ViewModel could be cleared, so..
                _areTitleExpiryDateNoNeeded.value = true
                _areTitleExpiryDateNoNeeded.value = false

                // same prefix to images files names
                val prefix = generateImageName()

                // save larger image
                val itemImagePath = _productImages.value?.let {
                    it.itemDetailsPageImage?.let { bitmap ->
                        saveBitmapIntoFile(bitmap, prefix, SUFFIX_LARGER_IMAGE, IMAGE_QUALITY)
                    }
                }
                // save thumbnail image
                val thumbnailImagePath = _productImages.value?.let {
                    it.listThumbnail?.let {
                        saveBitmapIntoFile(it, prefix, SUFFIX_THUMBNAIL_IMAGE, IMAGE_QUALITY)
                    }
                }
                // now images saved, so i can reset mutable live data value for product images
                _productImages.value = null

                val productId = title?.let { title ->
                    insertProduct(Product(itemImagePath, thumbnailImagePath, title))
                }

                productId?.let { productId ->
                    if (productId != -1L) {
                        expiryDate?.let { expiryDate ->
                            val id = insertExpiryDate(ExpiryDate(productId, expiryDate))
                            if (id != -1L) {
                                // schedule notifications here
                                if (toNotify) {
                                    schedulePendingNotifications(title, expiryDate, itemImagePath, id)
                                }
                            }
                        }
                    }
                }

                // use existed product
            } else {

                // "grab" expiry date for further processing
                val expiryDate = _expiryDate.value?.let { expiryDate ->
                    ProductUtils.convertExpiryDateForDatabase(expiryDate)
                }

                // now title and expiry date not needed in the Ui and in ViewModel could be cleared, so..
                _areTitleExpiryDateNoNeeded.value = true
                _areTitleExpiryDateNoNeeded.value = false

                val product = productSelected.value

                productSelected.value?.productId?.let { productId ->
                    if (productId != 0L) {
                        expiryDate?.let { expiryDate ->
                            val id = insertExpiryDate(ExpiryDate(productId, expiryDate))
                            if (id != -1L) {
                                // schedule notifications here
                                if (toNotify) {
                                    product?.let { product ->
                                        schedulePendingNotifications(product.title, expiryDate, product.image, id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            _productImages.value = null
        }
    }

    // TODO to override this method. this method written like that for test reasons
    private fun getNotificationsTestDaysLeft() = mutableListOf<Int>().apply {
        add(5)
        add(4)
        add(3)
        add(2)
        add(1)
    }

    // to schedule notification for the future, n days before product expires
    private suspend fun schedulePendingNotifications(title: String, expiryDate: Int, imagePath: String?, expiryDateId: Long) {
        val application = getApplication<Application>()
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // all days before exp. date to notify
        val daysBefore = getNotificationsTestDaysLeft()
        val notificationTimes = getNotificationTimes(9,21, expiryDate, daysBefore)

        // save notifications into database
        val ids = LongArray(notificationTimes.size) {
            insertNotification(Notification(expiryDateId))
        }

        // iterate to set all notifications
        notificationTimes.forEachIndexed {  idx, time ->

            // notification id from previously entered into table
            val notificationId = ids[idx]

            // check if inserted
            if (notificationId != -1L) {

                val intent = Intent(application, NotificationReceiver::class.java)
                // put extras
                intent.putExtra(NotificationConstants.TITLE_KEY, title)
                intent.putExtra(NotificationConstants.EXPIRY_DATE_KEY, expiryDate)
                intent.putExtra(NotificationConstants.EXPIRY_DATE_ID_KEY, expiryDateId)
                intent.putExtra(NotificationConstants.NOTIFICATION_ID_KEY, notificationId)
                intent.putExtra(NotificationConstants.IMAGE_PATH_KEY, imagePath)
                intent.putExtra(NotificationConstants.ALL_NOTIFICATIONS_KEY, ids)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                    val pendingIntent =
                        PendingIntent.getBroadcast(application, notificationId.toInt(), intent, PendingIntent.FLAG_ONE_SHOT)

                    // observation: if pending intent request code same - it rewrites
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                }
            }
        }

    }

    fun getNotificationTimes(
        workingHourStart: Int,
        workingHourEnd: Int,
        expiryDate: Int,
        daysBefore: List<Int>
    ): List<Long> {

        val timeLeft = ProductUtils.convertExpiryDateToTimeLeft(expiryDate)

        val fullDaysLeft = ProductUtils.convertTimeLeftToFullDaysLeft(timeLeft)

        val notificationTimes = mutableListOf<Long>()

        daysBefore.forEach { days ->
            if (days < fullDaysLeft) {

                // daysBefore + some n of hour into the future to show in the morning
                // TODO this logic can be reviewed in the future
                val notificationTime =
                    ProductUtils.convertExpiryDateIntoMillis(expiryDate) + 1 - days * 24 * 60 * 60 * 1000 + workingHourStart * 60 * 60 * 1000

                notificationTimes.add(notificationTime)
            }
        }
        return notificationTimes
    }

    // suspend fun to insert product into database
    private suspend fun insertProduct(product: Product): Long {
        return withContext(Dispatchers.IO) {
            databaseDao.insert(product)
        }
    }

    // suspend fun to insert expiry date into database
    private suspend fun insertExpiryDate(expiryDate: ExpiryDate): Long {
        return withContext(Dispatchers.IO) {
            databaseDao.insert(expiryDate)
        }
    }

    // suspend fun to insert notification into database
    private suspend fun insertNotification(notification: Notification): Long {
        return withContext(Dispatchers.IO) {
            databaseDao.insert(notification)
        }
    }

    // save bitmap into a file
    private suspend fun saveBitmapIntoFile(
        bitmap: Bitmap,
        prefix: String,
        suffix: String,
        quality: Int
    )
            : String {

        return withContext(Dispatchers.IO) {
            // image external directory
            val directory = getApplication<Application>()
                .getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                .toString()

            // image file
            val imageFile = File(directory, "$prefix$suffix.jpg")

            // file output stream
            val outputStream = FileOutputStream(imageFile)

            // actual saving of a file
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

            // closing stream
            outputStream.close()

            // return newly created file path
            imageFile.absolutePath
        }
    }

    fun enterExpiryDate(date: String) {
        _expiryDate.value = date
    }

    fun enterTitleDescription(titleDescription: String?) {
        _productTitle.value = titleDescription
    }

    fun setIsListFullScreen(isListFullScreen: Boolean) {
        _isListFullScreen.value = isListFullScreen
    }

    // getting/generating image name based on current time
    private fun generateImageName() = SimpleDateFormat("yyyyMMddHHmmssS").format(Date())

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}