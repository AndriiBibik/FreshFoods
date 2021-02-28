package products.fresh.foods

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.enter_product_layout.*
import products.fresh.foods.database.ProductsContract
import products.fresh.foods.database.ProductsCursorAdapter
import products.fresh.foods.database.ProductsDbHelper
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val LOG_TAG = MainActivity::class.qualifiedName

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setTitle(R.string.main_activity_title)

        date_picker_actions.setOnClickListener {
            if ( enter_product_text_field.hasFocus() ) enter_product_text_field.clearFocus()
            showDatePicker()
        }

        //click to make a picture
        take_a_photo_button.setOnClickListener {
            //hide keyboard, not needed anymore
            it.hideKeyboard()
            //clear focus from enter product field if there is some
            if ( enter_product_text_field.hasFocus() ) enter_product_text_field.clearFocus()

            //if system os is Marshmallow or above, permission request is needed
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                if ( checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED ) {
                    //permission was not enabled
                    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //show poput to request permissions
                    requestPermissions(permissions, PERMISSION_CODE)

                } else {
                    //permission already granted
                    openCamera()
                }
            } else {
                //system os is < Marshmallow
                openCamera()
            }
        }

        enter_product_edit_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if ( s != null && s.trim().isNotEmpty() ) {
                    // setting up custom end icon - text is changed
                    enter_product_text_field.endIconMode = TextInputLayout.END_ICON_CUSTOM
                    enter_product_text_field.setEndIconDrawable(R.drawable.ic_done_circle)
                } else {
                    enter_product_text_field.endIconMode = TextInputLayout.END_ICON_NONE
                }
            }
        })

        //put into database button
        put_into_database_button.setOnClickListener { v ->
            //TODO sanity check
            //title
            val title = enter_product_edit_text.text.toString()
            //date
            val date = date_picker_actions.text.toString()

            //TODO replace with provider
            val db = ProductsDbHelper(this).writableDatabase
            val cv = ContentValues().apply {
                put(ProductsContract.ProductsEntry.COLUMN_IMAGE, image_uri.toString())
                put(ProductsContract.ProductsEntry.COLUMN_TITLE, title)
                put(ProductsContract.ProductsEntry.COLUMN_EXPIRY_DATE, date)
            }
            val rowId = db.insert(ProductsContract.ProductsEntry.TABLE_NAME, null, cv)
        }

        //inflating list of products with records
        val cursor = ProductsDbHelper(this).readableDatabase
            .rawQuery("SELECT * FROM " + ProductsContract.ProductsEntry.TABLE_NAME, null)
        products_list.setAdapter(ProductsCursorAdapter(this, cursor))
    }

    fun showDatePicker() {

        // build date picker dialog
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText(R.string.expiry_date_text)

        // calendar constraints to prevent from picking earlier dates
        val constraintsBuilder = CalendarConstraints.Builder()
        constraintsBuilder.setValidator(DateValidatorPointForward.now())
        builder.setCalendarConstraints(constraintsBuilder.build())

        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener {
            val date = SimpleDateFormat("dd-MM-yyyy").format(Date(it))
            date_picker_actions.setText(date)
            // setting up custom end icon - date is picked
            expiry_date_text_field.endIconMode = TextInputLayout.END_ICON_CUSTOM
            expiry_date_text_field.setEndIconDrawable(R.drawable.ic_done_circle)
        }
        datePicker.show(supportFragmentManager, LOG_TAG)
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "new image")
        values.put(MediaStore.Images.Media.DESCRIPTION, "captured image")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when(requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //called when image was captured by camera intent
        if( resultCode == Activity.RESULT_OK ) {
            //setting image captured to imageview
            //TODO this is for test reason should be rewrited
            val bitmap = ThumbnailUtils.extractThumbnail(
                MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri),
                100,
                56,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
            product_image.setImageBitmap(bitmap)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}
