package products.fresh.foods.productdetails

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import products.fresh.foods.MainActivity
import products.fresh.foods.R
import products.fresh.foods.database.ProductDatabase
import products.fresh.foods.databinding.FragmentProductDetailsBinding
import java.util.*

class ProductDetailsFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailsBinding

    private val args: ProductDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Get a reference to the binding object and inflate the fragment views.
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_product_details, container, false
        )

        // start initializing ViewModel
        //
        val application = requireNotNull(this.activity).application
        val expiryDateId = args.expiryDateId
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        val productDetailsFragmentFactory =
            ProductDetailsViewModelFactory(expiryDateId, dataSource, application)
        val productDetailsViewModel = ViewModelProvider(this, productDetailsFragmentFactory)
            .get(ProductDetailsViewModel::class.java)
        //

        productDetailsViewModel.image.observe(viewLifecycleOwner, Observer { bitmap ->
            binding.productImage.setImageBitmap(bitmap)
        })
        productDetailsViewModel.title.observe(viewLifecycleOwner, Observer { title ->
            // set title
            binding.productTitle.text = title

        })
        productDetailsViewModel.expiryDate.observe(viewLifecycleOwner, Observer { expiryDate ->
            binding.productExpiryDate.text = expiryDate
        })
        //TODO(to implement days left observer)

        return binding.root
    }
}