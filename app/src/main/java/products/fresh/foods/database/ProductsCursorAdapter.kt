package products.fresh.foods.database

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import products.fresh.foods.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ProductsCursorAdapter(val context: Context?, cursor: Cursor): CursorAdapter(context, cursor, true) {

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false)
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        val iv = view.findViewById(R.id.product_image_ic) as ImageView
        val title_tv = view.findViewById(R.id.product_title) as TextView
        val expiry_date_tv = view.findViewById(R.id.product_expiry_date) as TextView
        val days_left_tv = view.findViewById(R.id.days_left_text_view) as TextView
        val days_left_hint = view.findViewById(R.id.days_left_hint) as TextView


//        iv.setImageURI(Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ProductsContract.ProductsEntry.COLUMN_IMAGE))))
        if (cursor.getString(cursor.getColumnIndexOrThrow(ProductsContract.ProductsEntry.COLUMN_IMAGE)) != null) {
//            val bitmap = MediaStore.Images.Media.getBitmap(
//                context?.contentResolver,
//                Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ProductsContract.ProductsEntry.COLUMN_IMAGE))));
//            iv.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 270, 270, ThumbnailUtils.OPTIONS_RECYCLE_INPUT))
            iv.setImageResource(R.drawable.bird_270)
        }

        title_tv.setText(cursor.getString(cursor.getColumnIndexOrThrow(ProductsContract.ProductsEntry.COLUMN_TITLE)))
        val expiryDate = cursor.getString(cursor.getColumnIndexOrThrow(ProductsContract.ProductsEntry.COLUMN_EXPIRY_DATE))
        expiry_date_tv.setText(expiryDate)
        //calculate days left
        val millsDiff = SimpleDateFormat("dd-MM-yyyy").parse(expiryDate).time - Calendar.getInstance().timeInMillis
        val daysLeft = TimeUnit.MILLISECONDS.toDays(millsDiff).toString()
        days_left_tv.setText(daysLeft)
        var colorIdx = Integer.parseInt(daysLeft); if (colorIdx > 6) colorIdx = 6 else if(colorIdx < 0) colorIdx = 0

        if (context != null) {
            val color = Color.parseColor(context.resources.getStringArray(R.array.left_days_indicators)[colorIdx])
            days_left_tv.setTextColor(color)
            days_left_hint.setTextColor(color)
        };
    }
}