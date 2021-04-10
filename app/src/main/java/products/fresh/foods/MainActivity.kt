package products.fresh.foods

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
