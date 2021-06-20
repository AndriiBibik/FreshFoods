package products.fresh.foods

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import products.fresh.foods.notifications.NotificationConstants
import products.fresh.foods.productshelf.NotificationOptionsHelper


class MainActivity : AppCompatActivity() {

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
//        // negative button
//        dialogBuilder.setNegativeButton(R.string.notify_no) { _, _ ->
//            // uncheck notify checkbox
//            binding.enterProductLayout.notify_or_not_checkbox.isChecked = false
//            // put product both with expiry date into database
//            productShelfViewModel.onPutProduct()
//        }
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // setting up Up button
        NavigationUI.setupActionBarWithNavController(
            this,
            this.findNavController(R.id.main_fragment_holder)
        )
    }

    fun setActionBarTitle(title: String) {
        title?.let {
            supportActionBar?.title = title
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.main_fragment_holder)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
