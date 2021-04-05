package products.fresh.foods.productdetails

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import products.fresh.foods.R
import products.fresh.foods.database.ProductDatabase
import products.fresh.foods.databinding.FragmentProductDetailsBinding

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

        return binding.root
    }
}