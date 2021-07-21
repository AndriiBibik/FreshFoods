package products.fresh.foods.productshelf

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.slider.Slider
import products.fresh.foods.R
import products.fresh.foods.notifications.NotificationConstants
import products.fresh.foods.notifications.NotificationConstants.Companion.DEFAULT_DELETE_DAYS
import products.fresh.foods.notifications.NotificationConstants.Companion.DELETE_DAYS_KEY
import products.fresh.foods.notifications.NotificationConstants.Companion.NEVER_DELETE_CHECKBOX_KEY

class NotificationOptionsHelper(val context: Context) {

    // notification shared preferences
    private val sharedPreferences = context
        .getSharedPreferences(
        NotificationConstants.NOTIFICATION_SHARED_PREFERENCES,
        Context.MODE_PRIVATE)
    // notification shared preferences editor
    private val editor = sharedPreferences.edit()

    // build options or notification options layout (depending on argument)
    // daysBefore argument - full days before expiry
    fun buildOptionsLayout(daysBefore: Int, optionsLayout: Int): View {
        // restrict to max number of days before
        val days = when {
            daysBefore > NotificationConstants.MAX_DAYS_BEFORE -> NotificationConstants.MAX_DAYS_BEFORE
            else -> daysBefore
        }

        // inflate base layout
        val baseLayout =
            when(optionsLayout) {
                NotificationConstants.GENERAL_OPTIONS_LAYOUT -> LayoutInflater.from(context).inflate(
                    R.layout.options_layout, null,  false
                )
                else -> LayoutInflater.from(context).inflate(
                    R.layout.notification_options_layout, null, false)
            }
        // hour text view
        val hourTextView = baseLayout.findViewById<TextView>(R.id.notification_hour)
        // hour slider
        val hourSlider = baseLayout.findViewById<Slider>(R.id.hour_picker_slider)
        // format label
        hourSlider.setLabelFormatter { value -> "${value.toInt()}:00" }

        // set on touch listener
        hourSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Do nothing
            }

            override fun onStopTrackingTouch(slider: Slider) {
                val hour = slider.value.toInt()
                hourTextView.text = "${hour}:00"
                setNotificationHour(hour)
            }
        })
        // set hour right away
        val hour = getNotificationHour()
        hourTextView.text = "${hour}:00"
        hourSlider.value = hour.toFloat()
        // flexbox
        val flexbox = baseLayout.findViewById(R.id.days_before_checkboxes_flexbox) as FlexboxLayout
        // build checkboxes layouts
        val checkboxesLayouts = buildCheckboxesLayouts(days)
        // add
        checkboxesLayouts.forEach { layout ->
            flexbox.addView(layout)
        }

        // add subtract days to delete after expiry
        if(optionsLayout == NotificationConstants.GENERAL_OPTIONS_LAYOUT) {
            val daysTextView = baseLayout.findViewById<TextView>(R.id.days_after_expiry_text)
            val addButton = baseLayout.findViewById<MaterialButton>(R.id.add_days_button)
            val subtractButton = baseLayout.findViewById<MaterialButton>(R.id.subtract_days_button)
            val neverCheckBox = baseLayout.findViewById<CheckBox>(R.id.never_delete_checkbox)
            // set value of days from shared preferences
            val days = sharedPreferences.getInt(DELETE_DAYS_KEY, DEFAULT_DELETE_DAYS)
            daysTextView.text = days.toString()
            subtractButton.setOnClickListener { _ ->
                val days = sharedPreferences.getInt(DELETE_DAYS_KEY, DEFAULT_DELETE_DAYS)
                if (days > 0) {
                    val newDays = days - 1
                    daysTextView.text = newDays.toString()
                    editor.putInt(DELETE_DAYS_KEY, newDays).apply()
                }
            }
            addButton.setOnClickListener { _ ->
                val days = sharedPreferences.getInt(DELETE_DAYS_KEY, DEFAULT_DELETE_DAYS)
                val newDays = days + 1
                daysTextView.text = newDays.toString()
                editor.putInt(DELETE_DAYS_KEY, newDays).apply()
            }
            // default never delete checkbox value
            val isNeverDeleteChecked = sharedPreferences.getBoolean(NEVER_DELETE_CHECKBOX_KEY, false)
            if (isNeverDeleteChecked) {
                // disabled
                val disabledTypedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.disabledTextColorInOptions, disabledTypedValue, true)
                val disabledTextColor = disabledTypedValue.data

                neverCheckBox.isChecked = isNeverDeleteChecked
                daysTextView.setTextColor(disabledTextColor)
            }
            neverCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                // to get color attributes
                // base
                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColor, typedValue, true)
                val baseTextColor = typedValue.data
                // disabled
                val disabledTypedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.disabledTextColorInOptions, disabledTypedValue, true)
                val disabledTextColor = disabledTypedValue.data
                //

                when(isChecked) {
                    true -> {
                        daysTextView.setTextColor(disabledTextColor)
                        editor.putBoolean(NEVER_DELETE_CHECKBOX_KEY, true).apply()
                    }
                    false -> {
                        daysTextView.setTextColor(baseTextColor)
                        editor.putBoolean(NEVER_DELETE_CHECKBOX_KEY, false).apply()
                    }
                }
            }
        }
        return baseLayout
    }

    // get notification hour from shared preferences
    fun getNotificationHour(): Int {
        return sharedPreferences.getInt(
            NotificationConstants.NOTIFICATION_HOUR_KEY,
            NotificationConstants.NOTIFICATION_DEFAULT_HOUR)
    }

    // set notification hour into shared preferences
    private fun setNotificationHour(hour: Int) {
        editor.putInt(NotificationConstants.NOTIFICATION_HOUR_KEY, hour).apply()
    }

    // get days before checkboxes checked array
    fun getDaysBeforeArray(): Array<Boolean> {
        val pattern = sharedPreferences.getString(
            NotificationConstants.NOTIFICATION_DAYS_BEFORE_KEY,
            NotificationConstants.NOTIFICATION_DEFAULT_DAYS_BEFORE)
        val arrayString = pattern!!.split(NotificationConstants.DIVIDER)
        return Array(arrayString.size) { idx ->
            // string to boolean
            arrayString[idx].equals(true.toString())
        }
    }

    // set days before array
    private fun setDaysBeforeArray(array: Array<Boolean>) {
        var pattern = ""
        array.forEachIndexed { idx, b ->
            pattern += b
            if (idx != array.lastIndex) pattern += NotificationConstants.DIVIDER
        }
        editor.putString(NotificationConstants.NOTIFICATION_DAYS_BEFORE_KEY, pattern).apply()
    }

    // build set of checkboxes layouts
    private fun buildCheckboxesLayouts(daysBefore: Int): ArrayList<LinearLayout> {
        val list = ArrayList<LinearLayout>()
        if (daysBefore > 0) {
            val daysBeforeChecked = getDaysBeforeArray()
            for (days in 0 until daysBefore) {
                list.add(buildCheckboxLayout(days, daysBeforeChecked[days]))
            }
        }
        return list
    }

    // build checkbox layout
    private fun buildCheckboxLayout(daysBefore: Int, isChecked: Boolean): LinearLayout {
        // text above checkbox
        val titleTextView = TextView(context).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        // title for example "1d"
        val title = "$daysBefore${context.resources.getString(R.string.days_shortcut)}"
        // set this text to textview
        titleTextView.text = title
        // checkbox. notify or not for days left
        val notifyCheckBox = MaterialCheckBox(context).apply {
            setChecked(isChecked)
            gravity = Gravity.CENTER_HORIZONTAL
            minimumWidth = 0
            minimumHeight = 0
            minWidth = 0
        }
        // on checkbox listener
        notifyCheckBox.setOnCheckedChangeListener { compoundButton, isChecked ->
            val checkedArray = getDaysBeforeArray()
            // switch value
            checkedArray[daysBefore] = isChecked
            // save checked array to preferences
            setDaysBeforeArray(checkedArray)
        }
        // set tag to get this checkbox later
        notifyCheckBox.setTag(daysBefore)
        // wrapping layout
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleTextView)
            addView(notifyCheckBox)
        }
    }
}