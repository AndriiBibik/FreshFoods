package products.fresh.foods.productshelf

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.layout_enter_product.view.*
import kotlinx.android.synthetic.main.layout_product_list.view.*
import kotlinx.coroutines.delay
import products.fresh.foods.R
import products.fresh.foods.database.ProductDatabase
import products.fresh.foods.databinding.FragmentProductsShelfBinding
import products.fresh.foods.productshelf.ProductShelfViewModel.Companion.SPAN_ONE
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProductShelfFragment : Fragment() {

    companion object {
        private const val PERMISSION_CODE = 101
        private const val TAKE_PICTURE_CODE = 201
    }

    private lateinit var binding: FragmentProductsShelfBinding

    private lateinit var productShelfViewModel: ProductShelfViewModel

    private lateinit var productAdapter: ProductAndExpiryDateAdapter

    private lateinit var gridLayoutManager: GridLayoutManager

    private val enterProductTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val titleText = binding.enterProductLayout.enter_product_title_edit_text
                .text.toString().trim()
            val expiryDateText = binding.enterProductLayout.date_picker_edit_field
                .text.toString().trim()

            // enable disable button if needed
            binding.enterProductLayout.put_into_database_button.let { b ->
                b.isEnabled = (titleText.isNotEmpty() && expiryDateText.isNotEmpty())
            }

            // update title in ViewModel and
            // remove/show "correct" ic
            if (titleText.isNotEmpty()) {
                productShelfViewModel?.let {
                    it.enterTitleDescription(titleText)
                }
                showTitleCorrectIc()
            } else {
                productShelfViewModel?.let {
                    it.enterTitleDescription(null)
                }
                removeTitleCorrectIc()
            }


            if (expiryDateText.isNotEmpty())
                showExpiryDateCorrectIc()

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Get a reference to the binding object and inflate the fragment views.
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_products_shelf, container, false
        )

        // initializing ViewModel
        //
        val application = requireNotNull(this.activity).application

        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao

        val viewModelFactory = ProductShelfViewModelFactory(dataSource, application)

        productShelfViewModel =
            ViewModelProvider(this, viewModelFactory)
                .get(ProductShelfViewModel::class.java)
        //

        val time = Date().time

        // init adapter with GridLayoutManager when layout width is available
        binding.productListLayout.recycler_view_container.doOnLayout {

            // init gridLayoutManager
            gridLayoutManager = GridLayoutManager(
                context,
                productShelfViewModel.getNumberOfSpans(it.width)
            )
            // init adapter
            productAdapter = ProductAndExpiryDateAdapter(gridLayoutManager)


            // this observer will be triggered: 1. list goes from null to {some database data};
            // 2. if ViewModel loads {database data} first (before doOnLayout triggered)
            // That is GOOD. TESTED...
            // GridLayoutManager and Adapter are initialized, now observer on the list can be applied
            productShelfViewModel.list.observe(viewLifecycleOwner, Observer {

                // list is changed, so submit it to the adapter
                productAdapter.submitList(it)

                // apply initialized GridLayoutManager and Adapter to RecyclerView
                binding.productListLayout.products_recycler_view.apply {
                    layoutManager = gridLayoutManager
                    adapter = productAdapter
                }
            })
        }

        // listen to edit text's changes
        // for "title/description" and for "expiry date"
        binding.enterProductLayout.enter_product_title_edit_text
            .addTextChangedListener(enterProductTextWatcher)
        binding.enterProductLayout.date_picker_edit_field
            .addTextChangedListener(enterProductTextWatcher)

        // listener for "put product" button
        binding.enterProductLayout.put_into_database_button.setOnClickListener {
            productShelfViewModel.onPutProduct()
        }

        //switch between grid/list mode
        binding.productListLayout.grid_list_switch_button.let { greedOrList ->

            //TODO get grid list ic from ViewModel
            // decide to display list in grid or list mode
            when (productShelfViewModel.isGrid) {
                // grid
                true -> {
                    greedOrList.setImageResource(R.drawable.ic_list_on)
                }
                // list
                false -> {
                    greedOrList.setImageResource(R.drawable.ic_grid_on)
                }
            }

            // listen clicks upon this switch imageView
            greedOrList.setOnClickListener { view ->
                when (productShelfViewModel.isGrid) {
                    // grid
                    true -> {
                        // switch to list
                        (view as ImageView).setImageResource(R.drawable.ic_grid_on)
                        // change variable in viewmodel
                        productShelfViewModel.isGrid = false
                        // change span for GridLayoutManager to switch for list
                        gridLayoutManager.spanCount = SPAN_ONE
                    }
                    false -> {
                        (view as ImageView).setImageResource(R.drawable.ic_list_on)
                        // change variable in viewmodel
                        productShelfViewModel.isGrid = true
                        // change span for GridLayoutManager to switch for grid
                        gridLayoutManager.spanCount = productShelfViewModel.getNumberOfSpans(
                            binding.productListLayout.recycler_view_container.width
                        )
                    }
                }
            }
        }

        // Spinner
        context?.let { context ->
            SortSpinnerAdapter(context, SortSpinnerItem.ITEMS).also { adapter ->

                val spinner = binding.productListLayout.sort_by_spinner
                // Apply the adapter to the spinner
                spinner.adapter = adapter

                spinner.setSelection(productShelfViewModel.sortSpinnerPos.value!!, false)

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        pos: Int,
                        id: Long
                    ) {
                        productShelfViewModel.setSortSpinnerPos(pos)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }
            }
        }

        // take picture click listener
        binding.enterProductLayout.take_a_photo_button.setOnClickListener {
            takePicture()
        }

        // observe changes to isImgageProcessing to show or disable progressBar
        productShelfViewModel.isImageProcessing.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> binding.enterProductLayout.image_progress_bar.visibility = View.VISIBLE
                false -> binding.enterProductLayout.image_progress_bar.visibility = View.GONE
            }
        })

        // observe bitmap changes
        productShelfViewModel.productImages.observe(viewLifecycleOwner, Observer {
            binding.enterProductLayout.product_image.setImageBitmap(it.listThumbnail)
        })

        // navigate to extended product list fragment by clicking "extend list" button
        binding.productListLayout.expand_lower_product_list_button.setOnClickListener {

            // set smaller number of spans if it is 1. Landscape mode; 2. Grid mode on;
            // 3. Goes from "full" to "normal" screen
            if(
                resources.getBoolean(R.bool.is_landscape) &&
                productShelfViewModel.isGrid &&
                productShelfViewModel.isListFullScreen.value!!
            ) {
                // total fragment width
                val fragmentWidth = binding.fragmentProductShelfContainer.width
                // list container normal (!fullScreen) width in landscape
                val listContainerWidth =
                    ((fragmentWidth - resources.getDimension(R.dimen.products_main_blocks_space)) / 2).toInt()
                // number of spans in normal mode
                val spans = productShelfViewModel.getNumberOfSpans(listContainerWidth)
                // set new number of spans to GridLayoutManager
                gridLayoutManager?.let {
                    it.spanCount = spans
                }
            }
            productShelfViewModel.setIsListFullScreen(!productShelfViewModel.isListFullScreen.value!!)
        }

        // to change span count in gridLayoutManager when it is needed in switching between fullscreen and normal
        // Restrict listener to only Landscape mode
        // Restrict firing on layout initialization
        // Restrict to going into "fullscreen" mode from normal (allowed); other way will be
        // triggered in "isFullScreen" observer to prevent recyclerview items change in width
        // in this case first - layout, second - larger number of spans
        // in other case first - number of spans, second - layout
        binding.productListLayout.recycler_view_container
            .addOnLayoutChangeListener { _,
                                         left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom ->

                // Restrict for landscape mode
                if (resources.getBoolean(R.bool.is_landscape)) {
                    // Do not allow firing in first initialization
                    if (!(oldLeft == 0 && oldRight == 0)) {
                        val oldWidth = oldRight - oldLeft
                        val width = right - left
                        val fragmentWidth = binding.fragmentProductShelfContainer.width
                        // only when goes into full screen mode
                        if ((width != oldWidth) && (width == fragmentWidth)) {
                            Log.v(
                                "LOG_WIDTH",
                                "$left $top $right $bottom $oldLeft $oldTop $oldRight $oldBottom"
                            )
                            Log.v("LOG_WIDTH", "$width")

                            gridLayoutManager?.apply {
                                requestLayout()
                                spanCount = productShelfViewModel.getNumberOfSpans(width)
                            }
                        }
                    }
                }
            }

        // observe isListFullScreen value from ViewModel and make appropriate decision on the layout
        productShelfViewModel.isListFullScreen.observe(
            viewLifecycleOwner,
            Observer { isListFullScreen ->
                binding.productListLayout.recycler_view_container.measure(0, 0)
                when (isListFullScreen) {
                    true -> {
                        binding.enterProductLayout.visibility = View.GONE
                        binding.productListLayout.expand_lower_product_list_button.setImageResource(
                            R.drawable.ic_lower
                        )
                    }
                    false -> {
                        binding.enterProductLayout.visibility = View.VISIBLE
                        binding.productListLayout.expand_lower_product_list_button.setImageResource(
                            R.drawable.ic_expand
                        )
                    }
                }
            })

        // "done" button listener for title/description edit text
        binding.enterProductLayout.enter_product_title_edit_text.apply {
            setOnEditorActionListener { textView, actionId, keyEvent ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        // passing this input into ViewModel
                        productShelfViewModel.enterTitleDescription(text.toString().trim())
                        // hide keyboard
                        textView.hideKeyboard()
                        clearFocus()
                        true
                    }
                    else -> false
                }
            }
        }

        // show date picker listener
        binding.enterProductLayout.date_picker_edit_field.setOnClickListener {
            showDatePicker()
        }

        // observing expiry date changes
        productShelfViewModel.expiryDate.observe(viewLifecycleOwner, Observer { expiryDate ->
            binding.enterProductLayout.date_picker_edit_field.setText(expiryDate)
        })

        return binding.root
    }

    private fun showDatePicker() {

        // build
        val datePicker = buildDatePicker()

        // add on positive button listener
        datePicker.addOnPositiveButtonClickListener { pickedDate ->
            // Date into String
            val date = SimpleDateFormat(ProductShelfViewModel.DATE_REPRESENTATION_PATTERN).format(
                Date(pickedDate)
            )
            // giving this value to ViewModel
            productShelfViewModel.enterExpiryDate(date)
            // setting up custom end icon - date is picked
            showExpiryDateCorrectIc()
        }

        // show
        datePicker.show(activity?.supportFragmentManager!!, "TAG_TO_SHOW")
    }


    private fun buildDatePicker(): MaterialDatePicker<Long> {
        // build date picker dialog
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText(R.string.expiry_date_text)

        // calendar constraints to prevent from picking earlier dates
        val constraintsBuilder = CalendarConstraints.Builder()
        constraintsBuilder.setValidator(DateValidatorPointForward.now())
        builder.setCalendarConstraints(constraintsBuilder.build())

        // build and return
        return builder.build()
    }

    // update expiry date field with "correct" icon
    private fun showExpiryDateCorrectIc() {
        binding.enterProductLayout.expiry_date_text_field.apply {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(R.drawable.ic_done_circle)
        }
    }

    // update text/description field with "correct" icon
    private fun showTitleCorrectIc() {
        binding.enterProductLayout.enter_product_text_field.apply {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(R.drawable.ic_done_circle)
        }
    }

    // remove "correct" ic from title/description
    private fun removeTitleCorrectIc() {
        binding.enterProductLayout.enter_product_text_field.endIconMode =
            TextInputLayout.END_ICON_NONE
    }

    // taking a picture of a product
    private fun takePicture() {

        val textField = binding.enterProductLayout.enter_product_text_field
        //hide keyboard, not needed anymore
        textField.hideKeyboard()
        //clear focus from enter product field if there is some
        if (textField.hasFocus())
            textField.clearFocus()

        //if system os is Marshmallow or above, permission request is needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity?.checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED ||
                activity?.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED
            ) {
                //permission was not enabled
                val permissions =
                    arrayOf(
                        Manifest.permission.CAMERA
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                //show popup to request permissions
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                }
            }
        }
    }

    private fun openCamera() {

        //camera intent
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {

            // check if there is any camera activity to handle intent
            // if resolveActivity = null then doesn't go into let
            resolveActivity(requireActivity().packageManager)?.let {

                // Create the File where the photo should go
                val photoFile: File? = try {
                    productShelfViewModel.createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }

                // Continue only if the File was successfully created
                photoFile?.let { file ->
                    val photoUri = FileProvider.getUriForFile(
                        requireContext(), "products.fresh.foods.fileprovider", file
                    )
                    // referring to intent's apply
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(this, TAKE_PICTURE_CODE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //called when image was captured by camera intent
        if (requestCode == TAKE_PICTURE_CODE && resultCode == Activity.RESULT_OK) {
            // show processing indicator
            productShelfViewModel.processImages()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // extension function on a view to hide keyboard
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

}