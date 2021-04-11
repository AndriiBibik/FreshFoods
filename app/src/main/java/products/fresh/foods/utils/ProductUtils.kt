package products.fresh.foods.utils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import products.fresh.foods.GoodFoodApp
import products.fresh.foods.R
import java.text.SimpleDateFormat
import java.util.*

class ProductUtils {

    companion object {

        const val DATE_REPRESENTATION_PATTERN = "dd-MM-yyyy"
        const val DATE_DATABASE_PATTERN = "yyyyMMdd"

        // to convert database int representation of a date into ui representation
        fun convertExpiryDateForUi(expiryDate: Int): String? {
            return SimpleDateFormat(DATE_DATABASE_PATTERN).parse(expiryDate.toString())?.let {
                SimpleDateFormat(DATE_REPRESENTATION_PATTERN).format(it)
            }
        }

        // to convert UI expiryDate String representation into database representation
        fun convertExpiryDateForDatabase(expiryDate: String): Int? {
            val date = SimpleDateFormat(ProductUtils.DATE_REPRESENTATION_PATTERN).parse(expiryDate)
            return SimpleDateFormat(ProductUtils.DATE_DATABASE_PATTERN).format(date).toIntOrNull()
        }

        // to convert expiryDate database Int value into time left in milliseconds
        private fun convertExpiryDateToTimeLeft(expiryDate: Int): Long {
            return (SimpleDateFormat(DATE_DATABASE_PATTERN).parse(expiryDate.toString()).time
                    + (24 * 60 * 60 * 1000 - 1) - Date().time)
        }

        fun getTextLeftColor(expiryDate: Int): Int {
            // application context to work with resources
            val application = GoodFoodApp.instance

            val timeLeft = convertExpiryDateToTimeLeft(expiryDate)

            val daysCount = (timeLeft / (1000 * 60 * 60 * 24)).toInt()
            // set days left color based on daysLeft
            val colorsArray =
                application.resources.getStringArray(R.array.left_days_indicators)

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
            return Color.parseColor(colorsArray[colorIndex])
        }

        fun buildTimeLeftText(expiryDate: Int): String {
            // application context to work with resources
            val resources = GoodFoodApp.instance.resources

            val timeLeft = convertExpiryDateToTimeLeft(expiryDate)

            val daysCount = (timeLeft / (1000 * 60 * 60 * 24)).toInt()

            return when (daysCount) {
                0 -> {
                    val hours = timeLeft / (60 * 60 * 1000)
                    val minutes = (timeLeft / (60 * 1000)) % 60
                        when {
                            timeLeft < 0 -> String.format(
                                "-%02d${resources.getString(R.string.hours_shortcut)}:%02d${resources.getString(R.string.minutes_shortcut)}",
                                Math.abs(hours),
                                Math.abs(minutes)
                            )
                            else -> String.format(
                                "%02d${resources.getString(R.string.hours_shortcut)}:%02d${resources.getString(R.string.minutes_shortcut)}",
                                hours,
                                minutes
                            )
                        }
                }
                else -> when {
                    daysCount < 0 -> "${resources.getString(R.string.expired_shortcut)}$daysCount ${resources.getString(R.string.days_shortcut)}"
                    else -> "$daysCount${resources.getString(R.string.days_left_text)}"
                }
            }
        }

        // to read Bitmap from file in the background
        suspend fun readBitmapInIO(path: String): Bitmap? {
            return withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(path)
            }
        }

    }
}