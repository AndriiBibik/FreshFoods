package products.fresh.foods

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import kotlinx.android.synthetic.main.activity_main.*
import products.fresh.foods.GoodFoodApp.Companion.APP_SHARED_PREFERENCES
import products.fresh.foods.GoodFoodApp.Companion.IS_NIGHT_MODE
import products.fresh.foods.notifications.NotificationConstants
import products.fresh.foods.productshelf.NotificationOptionsHelper
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE as MODE_PRIVATE1


class MainActivity : AppCompatActivity() {

    lateinit var appSharedPreferences: SharedPreferences

    // menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // notification options dialog
    // to show notify before expiration? dialog. only if user select product form suggestions -
    // -> fast track
    private fun showNotifyQuestionDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(R.string.options_title)
        // max days left to expiry to display all checkboxes
        val maxDays = NotificationConstants.MAX_DAYS_BEFORE
        // custom dialog layout
        val layout =
            NotificationOptionsHelper(this).buildOptionsLayout(maxDays, NotificationConstants.GENERAL_OPTIONS_LAYOUT)
        // set custom layout
        dialogBuilder.setView(layout)
        // positive button
        dialogBuilder.setPositiveButton(R.string.notification_options_button) { _, _ ->
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    // actions when menu items selected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.notification_options -> {
                showNotifyQuestionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        appSharedPreferences = getSharedPreferences(APP_SHARED_PREFERENCES, MODE_PRIVATE)
        val isNightMode = when(appSharedPreferences.contains(IS_NIGHT_MODE)) {
            true -> appSharedPreferences.getBoolean(IS_NIGHT_MODE, false)
            false -> AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        }

        // setting day or night theme
        when(isNightMode) {
            true -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.DarkAppTheme)
            }
            false -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.BaseAppTheme)
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // set appropriate dark or light mode
        when(isNightMode) {
            true -> findViewById<ImageView>(R.id.theme_switch_button)
            .setImageResource(R.drawable.ic_button_day_mode)
            false -> findViewById<ImageView>(R.id.theme_switch_button)
                .setImageResource(R.drawable.ic_button_night_mode)

        }

        // setting up Up button
        NavigationUI.setupActionBarWithNavController(
            this,
            this.findNavController(R.id.main_fragment_holder)
        )

        // listener for theme switch button
        findViewById<ImageView>(R.id.theme_switch_button).setOnClickListener {
            val isNightMode = when(appSharedPreferences.contains(IS_NIGHT_MODE)) {
                true -> appSharedPreferences.getBoolean(IS_NIGHT_MODE, false)
                false -> AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            }
            when(isNightMode) {
                true -> appSharedPreferences.edit().putBoolean(IS_NIGHT_MODE, false).apply()
                false -> appSharedPreferences.edit().putBoolean(IS_NIGHT_MODE, true).apply()
            }
            recreate()
        }
    }

    fun setToolBarTitle(title: String) {
        title?.let {
            toolbar_title.text = it
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.main_fragment_holder)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
