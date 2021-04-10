package products.fresh.foods.productshelf

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import products.fresh.foods.R
import products.fresh.foods.database.ProductAndExpiryDate
import products.fresh.foods.utils.ProductUtils

interface OnItemClickListener {
    fun onItemClick(item: ProductAndExpiryDate)
}

class ProductAndExpiryDateAdapter(
    private val layoutManager: GridLayoutManager,
    private val clickListener: OnItemClickListener
) : ListAdapter<ProductAndExpiryDate, ProductAndExpiryDateAdapter.ViewHolder>(
    ProductAndExpiryDateDiffCallback()
) {

    companion object {
        private const val VIEW_TYPE_LIST = 0
        private const val VIEW_TYPE_GRID = 1
        private const val SPAN_COUNT_ONE = 1
    }

    class ViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
        private var image: ImageView
        private var title: TextView
        private var expiryDate: TextView? = null
        private var daysLeft: TextView

        init {
            if (viewType == VIEW_TYPE_LIST) {
                image = itemView.findViewById(R.id.product_image_list)
                title = itemView.findViewById(R.id.product_title_list)
                expiryDate = itemView.findViewById(R.id.product_expiry_date_list)
                daysLeft = itemView.findViewById(R.id.product_days_left_list)
            } else {
                image = itemView.findViewById(R.id.product_image_grid)
                title = itemView.findViewById(R.id.product_title_grid)
                daysLeft = itemView.findViewById(R.id.product_days_left_grid)
            }
        }

        companion object {
            // getting appropriate ViewHolder by viewType
            fun from(parent: ViewGroup, viewType: Int): ViewHolder {
                // layout inflater
                val inflater = LayoutInflater.from(parent.context)
                // construct ViewHolder depending on a view type
                val view = when (viewType) {
                    VIEW_TYPE_LIST -> inflater.inflate(R.layout.product_item_list, parent, false)
                    else -> inflater.inflate(R.layout.product_item_grid, parent, false)
                }
                return ViewHolder(view, viewType)
            }
        }

        fun bind(item: ProductAndExpiryDate, clickListener: OnItemClickListener) {

            // setting click listener
            itemView.setOnClickListener {
                clickListener.onItemClick(item)
            }

            // perform image reading from file on a background
            CoroutineScope(Dispatchers.IO).launch {
                val thumbnail = BitmapFactory.decodeFile(item.product.thumbnail)
                withContext(Dispatchers.Main) {
                    // set image
                    image.setImageBitmap(thumbnail)
                }
            }

            // set title
            title.text = item.product.title
            // when list - set expiry date
            if (itemViewType == VIEW_TYPE_LIST) {
                expiryDate?.text =
                    ProductUtils.convertExpiryDateForUi(item.expiryDate.expiryDate)
            }

            //
            // for time left
            val expiryDate = item.expiryDate.expiryDate

            // set days left color based on daysLeft
            val color = ProductUtils.getTextLeftColor(expiryDate)
            // set time left text color
            // and set text
            daysLeft.run {
                setTextColor(color)
                val daysLeftText = ProductUtils.buildTimeLeftText(expiryDate)
                text = daysLeftText
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (layoutManager.spanCount) {
            SPAN_COUNT_ONE -> VIEW_TYPE_LIST
            else -> VIEW_TYPE_GRID
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        // encapsulating logic of getting appropriate ViewHolder into ViewHolder class
        return ViewHolder.from(parent, viewType)
    }

    override fun onBindViewHolder(holder: ProductAndExpiryDateAdapter.ViewHolder, position: Int) {
        // binding encapsulated inside ViewHolder class
        holder.bind(getItem(position), clickListener)
    }

    class ProductAndExpiryDateDiffCallback : DiffUtil.ItemCallback<ProductAndExpiryDate>() {
        override fun areItemsTheSame(
            oldItem: ProductAndExpiryDate,
            newItem: ProductAndExpiryDate
        ): Boolean {
            return oldItem.product.productId == newItem.product.productId
        }

        override fun areContentsTheSame(
            oldItem: ProductAndExpiryDate,
            newItem: ProductAndExpiryDate
        ): Boolean {
            return oldItem == newItem
        }
    }
}