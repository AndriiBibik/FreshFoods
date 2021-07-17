package products.fresh.foods.productshelf

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import products.fresh.foods.GoodFoodApp
import products.fresh.foods.R
import products.fresh.foods.database.ProductAndExpiryDateWithNotifications
import products.fresh.foods.notifications.NotificationConstants
import products.fresh.foods.notifications.NotificationConstants.Companion.NOTIFICATION_SHARED_PREFERENCES
import products.fresh.foods.utils.ProductUtils
import java.util.regex.Pattern

interface OnItemClickListener {
    fun onItemClick(item: ProductAndExpiryDateWithNotifications)
}

class ProductAndExpiryDateWithNotificationsAdapter(
    private val layoutManager: GridLayoutManager,
    private val clickListener: OnItemClickListener
) : ListAdapter<ProductAndExpiryDateWithNotifications, ProductAndExpiryDateWithNotificationsAdapter.ViewHolder>(
    ProductAndExpiryDateWithNotificationsDiffCallback()
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
        private var notificationsIc: ImageView
        private val appContext = GoodFoodApp.instance

        init {
            if (viewType == VIEW_TYPE_LIST) {
                image = itemView.findViewById(R.id.product_image_list)
                title = itemView.findViewById(R.id.product_title_list)
                expiryDate = itemView.findViewById(R.id.product_expiry_date_list)
                daysLeft = itemView.findViewById(R.id.product_days_left_list)
                notificationsIc = itemView.findViewById(R.id.product_notification_ic_list)
            } else {
                image = itemView.findViewById(R.id.product_image_grid)
                title = itemView.findViewById(R.id.product_title_grid)
                daysLeft = itemView.findViewById(R.id.product_days_left_grid)
                notificationsIc = itemView.findViewById(R.id.product_notification_ic_grid)
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

        fun bind(item: ProductAndExpiryDateWithNotifications, clickListener: OnItemClickListener) {

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

            // set appropriate notification indicator icon
            // if there are notifications or there are no notifications
            val sp = GoodFoodApp.instance.getSharedPreferences(
                NOTIFICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE)

            val idsString = sp.getString(NotificationConstants.NOTIFICATIONS_TO_DELETE_KEY, "")
            val matcher = Pattern.compile("\\d+").matcher(idsString)
            val notifIdsArr = arrayListOf<Long>().apply {
                item.expiryDateWithNotifications.notifications.forEach {
                    add(it.notificationId) }
            }
            val notifIdsToDeleteArr = arrayListOf<Long>().apply {
                while (matcher.find()) {
                    add(matcher.group().toLong())
                }
            }
            notifIdsArr.removeAll(notifIdsToDeleteArr)
            if (notifIdsArr.isNotEmpty()) {
                notificationsIc.setImageResource(R.drawable.ic_notification_on)
                notificationsIc.setColorFilter(ContextCompat.getColor(appContext, R.color.colorAccent))
            } else {
                notificationsIc.setImageResource(R.drawable.ic_notification_off)
                notificationsIc.setColorFilter(ContextCompat.getColor(appContext, R.color.no_notifications_ic_color))
            }

            // set title
            title.text = item.product.title
            // when list - set expiry date
            if (itemViewType == VIEW_TYPE_LIST) {
                expiryDate?.text =
                    ProductUtils.convertExpiryDateForUi(item.expiryDateWithNotifications.expiryDate.expiryDate)
            }

            //
            // for time left
            val expiryDate = item.expiryDateWithNotifications.expiryDate.expiryDate

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

    override fun onBindViewHolder(holder: ProductAndExpiryDateWithNotificationsAdapter.ViewHolder, position: Int) {
        // binding encapsulated inside ViewHolder class
        holder.bind(getItem(position), clickListener)
    }

    class ProductAndExpiryDateWithNotificationsDiffCallback : DiffUtil.ItemCallback<ProductAndExpiryDateWithNotifications>() {
        override fun areItemsTheSame(
            oldItem: ProductAndExpiryDateWithNotifications,
            newItem: ProductAndExpiryDateWithNotifications
        ): Boolean {
            return oldItem.product.productId == newItem.product.productId
        }

        override fun areContentsTheSame(
            oldItem: ProductAndExpiryDateWithNotifications,
            newItem: ProductAndExpiryDateWithNotifications
        ): Boolean {
            return oldItem == newItem
        }
    }
}