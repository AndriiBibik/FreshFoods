package products.fresh.foods

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI


class MainActivity : AppCompatActivity() {

    private val LOG_TAG = MainActivity::class.qualifiedName

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // setting up Up button
        NavigationUI.setupActionBarWithNavController(
            this,
            this.findNavController(R.id.main_fragment_holder)
        )
//
//        setTitle(R.string.main_activity_title)
//
//        date_picker_actions.setOnClickListener {
//            if ( enter_product_text_field.hasFocus() ) enter_product_text_field.clearFocus()
//            showDatePicker()
//        }
//
//        //click to make a picture
//        take_a_photo_button.setOnClickListener {
//            //hide keyboard, not needed anymore
//            it.hideKeyboard()
//            //clear focus from enter product field if there is some
//            if ( enter_product_text_field.hasFocus() ) enter_product_text_field.clearFocus()
//
//            //if system os is Marshmallow or above, permission request is needed
//            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
//                if ( checkSelfPermission(Manifest.permission.CAMERA)
//                    == PackageManager.PERMISSION_DENIED ||
//                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    == PackageManager.PERMISSION_DENIED ) {
//                    //permission was not enabled
//                    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    //show popup to request permissions
//                    requestPermissions(permissions, PERMISSION_CODE)
//
//                } else {
//                    //permission already granted
//                    openCamera()
//                }
//            } else {
//                //system os is < Marshmallow
//                openCamera()
//            }
//        }
//
//        enter_product_edit_text.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if ( s != null && s.trim().isNotEmpty() ) {
//                    // setting up custom end icon - text is changed
//                    enter_product_text_field.endIconMode = TextInputLayout.END_ICON_CUSTOM
//                    enter_product_text_field.setEndIconDrawable(R.drawable.ic_done_circle)
//                } else {
//                    enter_product_text_field.endIconMode = TextInputLayout.END_ICON_NONE
//                }
//            }
//        })
//
//        //put into database button
//        put_into_database_button.setOnClickListener { v ->
//            //TODO sanity check
//            //title
//            val title = enter_product_edit_text.text.toString()
//            //date
//            val date = date_picker_actions.text.toString()
//
//            //TODO replace with provider
//            val db = ProductsDbHelper(this).writableDatabase
//            val cv = ContentValues().apply {
//                put(ProductsContract.ProductsEntry.COLUMN_IMAGE, image_uri.toString())
//                put(ProductsContract.ProductsEntry.COLUMN_TITLE, title)
//                put(ProductsContract.ProductsEntry.COLUMN_EXPIRY_DATE, date)
//            }
//            val rowId = db.insert(ProductsContract.ProductsEntry.TABLE_NAME, null, cv)
//        }
//
//        //inflating list of products with records
//        val cursor = ProductsDbHelper(this).readableDatabase
//            .rawQuery("SELECT * FROM " + ProductsContract.ProductsEntry.TABLE_NAME, null)
//        products_list.setAdapter(ProductsCursorAdapter(this, cursor))
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.main_fragment_holder)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
//

//
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        //called when image was captured by camera intent
////        if( resultCode == Activity.RESULT_OK ) {
////            //setting image captured to imageview
////            //TODO this is for test reason should be rewrited
////            val bitmap = ThumbnailUtils.extractThumbnail(
////                MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri),
////                100,
////                56,
////                ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
////            product_image.setImageBitmap(bitmap)
////        }
////        super.onActivityResult(requestCode, resultCode, data)
////    }
//
//    fun View.hideKeyboard() {
//        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(windowToken, 0)
//    }
}
