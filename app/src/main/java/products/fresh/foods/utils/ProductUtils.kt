package products.fresh.foods.utils
import android.graphics.Color
import products.fresh.foods.GoodFoodApp
import products.fresh.foods.R

class ProductUtils {

    companion object {
        fun getTextLeftColor(timeLeft: Long): Int {
            // application context to work with resources
            val application = GoodFoodApp.instance

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
        fun buildTimeLeftText(timeLeft: Long): String {
            // application context to work with resources
            val resources = GoodFoodApp.instance.resources
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
    }
}