package products.fresh.foods.productshelf

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.item_sort_spinner.view.*
import products.fresh.foods.R

class SortSpinnerAdapter(context: Context, items: List<SortSpinnerItem>) : ArrayAdapter<SortSpinnerItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position,convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position,convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {

        return getItem(position).let { item ->
            LayoutInflater.from(context).inflate(R.layout.item_sort_spinner, parent, false)
                .apply {
                    // put some size span on text
                    item?.let {
                        SpannableString(it.text).apply {
                            val fraction = 0.04f
                            var proportion = when(it.isAsc) {
                                true -> (1f - this.length*fraction)
                                false -> {
                                    1f
                                }
                            }
                            for (i in 0 until length) {
                                setSpan(
                                    RelativeSizeSpan(proportion),
                                    i,
                                    (i+1),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                when(item.isAsc) {
                                    true -> proportion += fraction
                                    false -> proportion -= fraction
                                }
                            }
                        }.also {
                            item_spinner_text.text = it
                        }

                    }
                    //

                    item?.let { item_spinner_ic_res.setImageResource(it.ic) }
            }
        }
    }
}