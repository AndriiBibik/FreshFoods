package products.fresh.foods.productshelf

import products.fresh.foods.GoodFoodApp
import products.fresh.foods.R

data class SortSpinnerItem(val text: String, val ic: Int, val isAsc: Boolean) {

    companion object {

        val ITEMS = GoodFoodApp.instance.resources.getStringArray(R.array.sort_by_array).let { arrStr ->

            // array of list items
            ArrayList<SortSpinnerItem>().apply {

                val ascDescArrString =
                    GoodFoodApp.instance.resources.getStringArray(R.array.sort_by_array_is_asc)

                for (i in arrStr.indices) {
                    when(ascDescArrString[i].toBoolean()) {
                        true -> add(SortSpinnerItem(arrStr[i], R.drawable.ic_carrot_up, true))
                        false -> add(SortSpinnerItem(arrStr[i], R.drawable.ic_carrot_down, false))
                    }
                }
            }
            //
        }
    }
}