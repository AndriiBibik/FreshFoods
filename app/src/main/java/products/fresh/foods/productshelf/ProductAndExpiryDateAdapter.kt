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
import products.fresh.foods.R
import products.fresh.foods.database.ProductAndExpiryDate

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

            //TODO(to run this on the background thread)
            val thumbnail = BitmapFactory.decodeFile(item.product.thumbnail)
            // set image
            image.setImageBitmap(thumbnail)
            // set title
            title.text = item.product.title
            // when list - set expiry date
            if (itemViewType == VIEW_TYPE_LIST) {
                expiryDate?.text =
                    ProductShelfViewModel.convertExpiryDateForUi(item.expiryDate.expiryDate)
            }

            //
            // for time left
            val timeLeft = ProductShelfViewModel.convertExpiryDateToTimeLeft(item.expiryDate.expiryDate)
            val daysCount = (timeLeft / (1000 * 60 * 60 * 24)).toInt()
            // set days left color based on daysLeft
            Log.v("LOG_W", "time left: $timeLeft")
            Log.v("LOG_W", "days cont: $daysCount")
            val colorsArray =
                itemView.context.resources.getStringArray(R.array.left_days_indicators)
            // getting right color array index
            val colorIndex = daysCount.let { daysN ->
                when {
                    daysN <= 0 -> {
                        when {
                            timeLeft < 0 -> 7
                            else -> 0
                        }
                    }
                    daysN in 1..5 -> daysN
                    else -> 6
                }
            }
            // set time left text color
            // and set text
            daysLeft.run {
                setTextColor(Color.parseColor(colorsArray[colorIndex]))
                when (daysCount) {
                    0 -> {
                        val hours = timeLeft / (60 * 60 * 1000)
                        val minutes = (timeLeft / (60 * 1000)) % 60
                        itemView.context.resources.let {
                            text = when {
                                timeLeft < 0 -> String.format(
                                    "-%02d${it.getString(R.string.hours_shortcut)}:%02d${it.getString(R.string.minutes_shortcut)}",
                                    Math.abs(hours),
                                    Math.abs(minutes)
                                )
                                else -> String.format(
                                    "%02d${it.getString(R.string.hours_shortcut)}:%02d${it.getString(R.string.minutes_shortcut)}",
                                    hours,
                                    minutes
                                )
                            }
                        }
                    }
                    else -> text = when {
                        daysCount < 0 -> "${itemView.context.resources.getString(R.string.expired_shortcut)}$daysCount ${itemView.context.resources.getString(R.string.days_shortcut)}"
                        else -> "$daysCount${itemView.context.resources.getString(R.string.days_left_text)}"
                    }

                }

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
            //TODO to take a look later how it is works. if ti is works right
            return oldItem == newItem
        }
    }
}