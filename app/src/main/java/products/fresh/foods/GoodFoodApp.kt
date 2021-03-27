package products.fresh.foods

import android.app.Application

class GoodFoodApp : Application() {
    companion object {
        lateinit var instance: GoodFoodApp private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}