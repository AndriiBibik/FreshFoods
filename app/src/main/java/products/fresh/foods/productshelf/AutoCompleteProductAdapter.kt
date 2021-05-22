package products.fresh.foods.productshelf

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import products.fresh.foods.R
import products.fresh.foods.database.Product

class AutoCompleteProductAdapter(context: Context, products: List<Product>): ArrayAdapter<Product>(context, 0, products) {

    // copy of original list to always have all items
    private val productsListFull = products.toList()

    override fun getFilter(): Filter {
        return productsFilter
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var layout = convertView

        if (layout == null) {
            layout = LayoutInflater.from(context)
                .inflate(R.layout.product_suggested_list_item, parent, false)
        }

        // product item
        val product = getItem(position)

        // null check for a product item
        product?.let { product ->

            // setting title for suggestion
            layout?.findViewById<TextView>(R.id.suggested_product_title)?.text = product.title

            // perform image reading from file on a background
            CoroutineScope(Dispatchers.IO).launch {
                val thumbnail = BitmapFactory.decodeFile(product.thumbnail)
                withContext(Dispatchers.Main) {
                    // set image
                    layout?.findViewById<ImageView>(R.id.suggested_product_image)?.setImageBitmap(thumbnail)
                }
            }

        }



        return layout!!
    }

    private val productsFilter = object : Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val suggestions = arrayListOf<Product>()

            if (constraint.isNullOrEmpty()) {
                suggestions.addAll(productsListFull)
            } else {
                val filterPattern = constraint.toString().toLowerCase().trim()
                productsListFull.forEach { product ->
                    if (product.title.toLowerCase().contains(filterPattern)) {
                        suggestions.add(product)
                    }
                }
            }

            results.values = suggestions
            results.count = suggestions.size

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            addAll(results?.values as List<Product>)
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as Product).title
        }
    }

}