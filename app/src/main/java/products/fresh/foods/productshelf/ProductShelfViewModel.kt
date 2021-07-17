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
import android.support.v4.os.IResultReceiver
import android.util.Log
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.*
import products.fresh.foods.GoodFoodApp.Companion.APP_SHARED_PREFERENCES
import products.fresh.foods.R
import products.fresh.foods.database.ExpiryDate
import products.fresh.foods.database.Notification
import products.fresh.foods.database.Product
import products.fresh.foods.database.ProductDatabaseDao
import products.fresh.foods.notifications.NotificationButtonReceiver
import products.fresh.foods.notifications.NotificationConstants
import products.fresh.foods.notifications.NotificationConstants.Companion.DEFAULT_DELETE_DAYS
import products.fresh.foods.notifications.NotificationConstants.Companion.DELETE_DAYS_KEY
import products.fresh.foods.notifications.NotificationConstants.Companion.NEVER_DELETE_CHECKBOX_KEY
import products.fresh.foods.notifications.NotificationReceiver
import products.fresh.foods.utils.ProductUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class ProductShelfViewModel(
    private val databaseDao: ProductDatabaseDao, application: Application
) : AndroidViewModel(application) {

    companion object {
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

    // ViewModel Job
    private val viewModelJob = Job()

    // coroutines scope
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // listen to shared preferences changes when
    private val listener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when(key) {
                NotificationConstants.EXPIRY_DATES_IDS_TO_DELETE -> {
                    // array of ids
                    actionsToDelete()
                }
            }
        }

    // to trigger shared preferences change listener at start
    private fun triggerSPChaneListener() {
        val key = NotificationConstants.EXPIRY_DATES_IDS_TO_DELETE
        val idsString = sharedPreferences.getString(key, "")
        val editor = sharedPreferences.edit()
        editor.remove(key).apply()
        editor.putString(key, idsString).apply()
    }

    // camera photo path when picture is taken
    var cameraPhotoPath: String? = null

    // when start view mode
    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        triggerSPChaneListener()
        uiScope.launch {
            deleteExpiryDatesConsideringDeleteDays()
        }
    }

    // actions when shared preferences expiry dates ids to delete changes
    // one deletion and shared preferences update triggers next cycle of deletion
    // for example "1|2|3|" -> "1|2|" -> "1|" -> ""
    private fun actionsToDelete() {
        // get String value from prefs
        val idsString = sharedPreferences.getString(NotificationConstants.EXPIRY_DATES_IDS_TO_DELETE, "")
        if (!idsString.isNullOrBlank()) {
            val idsStringArray = idsString.split(NotificationButtonReceiver.DIVIDER)
            // last expiry date id
            val lastId = Integer.parseInt(idsStringArray[idsStringArray.size - 2])
            // delete expiry date
            uiScope.launch {
                withContext(Dispatchers.IO) {
                    //notifications pending intents deletes immediately after "Eaten! button press
                    databaseDao.deleteExpiryDateById(lastId.toLong())
                }
            }
            // to get string of ids without last id and without divider
            val startIndex = 0
            val endIndex = idsString.length - lastId.toString().length - 1
            // one more check to make it more solid
            if (endIndex >= startIndex) {
                val newIdsString = idsString.substring(0, endIndex)
                // update shared preferences
                sharedPreferences
                    .edit()
                    .putString(NotificationConstants.EXPIRY_DATES_IDS_TO_DELETE, newIdsString)
                    .apply()
            }
        }
    }

    // current sort spinner position
    val sortSpinnerPos =
        MutableLiveData<Int>(sharedPreferences.getInt(SPINNER_ID_KEY, SPINNER_ID_TIME_ADDED_DESC))

    fun setSortSpinnerPos(pos: Int) {
        sortSpinnerPos.value = pos
        sharedPreferences.edit().putInt(SPINNER_ID_KEY, pos).apply()
    }

    // list of notifications to delete them from shared preferences if needed
    val notificationsList = databaseDao.getNotifications()
    // notifications ids list based on list above
    val notificationsIdsList = Transformations.map(notificationsList) { notifications ->
        arrayListOf<Long>().apply {
            notifications.forEach { notification -> add(notification.notificationId) }
        }
    }
    // to process notifications ids (shared preferences) in a background thread
    fun processNotificationsIdsInSP(notificationsIds: List<Long>) {
        uiScope.launch {
            withContext(Dispatchers.Default) {

                val sp = getApplication<Application>().getSharedPreferences(NotificationConstants.NOTIFICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE)
                val idsFromSP = arrayListOf<Long>().apply {
                    val idsString = sp.getString(NotificationConstants.NOTIFICATIONS_TO_DELETE_KEY, "")
                    val matcher = Pattern.compile("\\d+").matcher(idsString)
                    while (matcher.find()) {
                        this.add(matcher.group().toLong())
                    }
                }
                val idsToRemove = arrayListOf<Long>().apply {
                    addAll(idsFromSP)
                    removeAll(notificationsIds)
                }
                val newSPIds = arrayListOf<Long>().apply {
                    addAll(idsFromSP)
                    removeAll(idsToRemove)
                }
                var newIdsString = ""
                newSPIds.forEach {
                    newIdsString += "$it${NotificationConstants.DIVIDER}"
                }
                //write
                sp.edit().putString(NotificationConstants.NOTIFICATIONS_TO_DELETE_KEY, newIdsString).apply()
            }
        }
    }

    // list of product for suggestions in enter product title field
    val productsList = databaseDao.getAllProductsByTitleAsc()

    // product if selected from suggested list
    val productSelected = MutableLiveData<Product>()

    // actual sorted list that we going to use. This list is based on "list" above and "sortType"
    val sortedList = Transformations.switchMap(sortSpinnerPos) { pos ->
        when (pos) {
            SPINNER_ID_TIME_LEFT_ASC -> databaseDao.getAllProductsAndExpiryDatesWithNotificationsByTimeLeftAsc()//0
            SPINNER_ID_TIME_LEFT_DESC -> databaseDao.getAllProductsAndExpiryDatesWithNotificationsByTimeLeftDesc()//1
            SPINNER_ID_TIME_ADDED_ASC -> databaseDao.getAllProductsAndExpiryDatesWithNotificationsDesc()//3
            else -> databaseDao.getAllProductsAndExpiryDatesWithNotifications() //2
        }
    }

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
                val ed = sortedList.value?.get(pos)?.expiryDateWithNotifications?.expiryDate?.let { expiryDate ->
                    uiScope.launch {
                        onDeleteExpiryDateAndNotifications(expiryDate.expiryDateId)
                    }
                }
            }
        }

    // to delete all expiry dates automatically when products expired
    // for more days then it is selected in options
    private suspend fun deleteExpiryDatesConsideringDeleteDays() {
        withContext(Dispatchers.IO) {

            val sp = getApplication<Application>()
                .getSharedPreferences(
                    NotificationConstants.NOTIFICATION_SHARED_PREFERENCES,
                    Context.MODE_PRIVATE)

            val deleteDays = sp.getInt(DELETE_DAYS_KEY, DEFAULT_DELETE_DAYS)

            // check if user wants to delete expiry dates
            if (!sp.getBoolean(NEVER_DELETE_CHECKBOX_KEY, false)) {

                val deleteDate = Calendar.getInstance().apply {
                    timeInMillis = Date().time - deleteDays*24*60*60*1000
                }
                val year = deleteDate.get(Calendar.YEAR)
                val month = deleteDate.get(Calendar.MONTH) + 1
                val day = deleteDate.get(Calendar.DATE)
                val deleteDateString = String.format("%d%02d%02d", year, month, day)
                val deleteDateInt = deleteDateString.toInt()

                // makes no sense to delete also pending intents for notifications
                // after expiration date
                databaseDao.deleteAllExpiryDatesConsideringDeleteDate(deleteDateInt)
            }
        }
    }

    // to delete Expiry Date from database
    private suspend fun deleteExpiryDate(expiryDate: ExpiryDate) {
        withContext(Dispatchers.IO) {
            databaseDao.delete(expiryDate)
        }
    }

    // delete expiry date and all appropriate notifications
    private suspend fun onDeleteExpiryDateAndNotifications(expiryDateId: Long) {
        // application context
        val application = getApplication<Application>()
        // find notifications for this expiry date
        val notifications = withContext(Dispatchers.IO) {
            databaseDao.getNotificationsByExpiryDate(expiryDateId)
        }
        // cancel all notifications in alarm manager
        val alarmManager =
            application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // iterate, cancel notifications
        notifications.forEach {  notification ->
            val id = notification.notificationId.toInt()
            // cancel
            val cancelIntent = Intent(application, NotificationReceiver::class.java)
            val pendingCancelIntent = PendingIntent.getBroadcast(application, id, cancelIntent, PendingIntent.FLAG_ONE_SHOT)
            alarmManager.cancel(pendingCancelIntent)
            pendingCancelIntent.cancel()
        }
        // delete expiry date after all notifications was canceled
        withContext(Dispatchers.IO) {
            databaseDao.deleteExpiryDateById(expiryDateId)
        }

        // done :)
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

    fun processImages(absolutePhotoPath: String) {

        uiScope.launch {
            // image started processing
            _isImageProcessing.value = true

            val imageBitmaps = prepareImageBitmaps(absolutePhotoPath)
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
        )
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

    // to schedule notification for the future, n days before product expires
    private suspend fun schedulePendingNotifications(title: String, expiryDate: Int, imagePath: String?, expiryDateId: Long) {
        val application = getApplication<Application>()
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val notificationTimes = getNotificationTimes(expiryDate)

        // save notifications into database
        val ids = LongArray(notificationTimes.size) {
            insertNotification(Notification(expiryDateId))
        }
        val testTime = Date().time

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

                // pending intent for alarm manager
                val pendingIntent =
                    PendingIntent.getBroadcast(application, notificationId.toInt(), intent, PendingIntent.FLAG_ONE_SHOT)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, (testTime + (idx+1)*5000), pendingIntent)
                } else { // for lower APIs: 16,17,18
                    alarmManager.set(AlarmManager.RTC_WAKEUP, (testTime + (idx+1)*5000), pendingIntent)
                }
            }
        }
    }

    private fun getNotificationTimes(expiryDate: Int): List<Long> {

        // application context
        val application = getApplication<Application>()

        // notification helper
        val helper = NotificationOptionsHelper(application)
        // notification hour
        val hour = helper.getNotificationHour()
        // to notify or not array
        val notifyArray = helper.getDaysBeforeArray()

        val fullDaysLeft = ProductUtils.convertTimeLeftToFullDaysLeft(
            ProductUtils.convertExpiryDateToTimeLeft(expiryDate)
        )
        val daysBefore = when {
            fullDaysLeft > NotificationConstants.MAX_DAYS_BEFORE -> NotificationConstants.MAX_DAYS_BEFORE
            else -> fullDaysLeft
        }

        // expiry date in milliseconds
        val expiryDateMillis = ProductUtils.convertExpiryDateIntoMillis(expiryDate)

        val notificationTimes = mutableListOf<Long>()

        // iterate
        for (day in 0 until daysBefore) {
            if (notifyArray[day]) {
                val notificationTime = expiryDateMillis + 1 - (day + 1)*24*60*60*1000 + hour*60*60*1000
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
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}