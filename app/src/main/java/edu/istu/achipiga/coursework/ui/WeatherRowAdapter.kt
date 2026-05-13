package edu.istu.achipiga.coursework.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.istu.achipiga.coursework.databinding.ItemWeatherHourBinding
import edu.istu.achipiga.coursework.databinding.ItemWeatherRowBinding

data class HourChipUi(
    val clock: String,
    val tempC: Int,
    @DrawableRes val iconRes: Int
)

class WeatherRowAdapter : RecyclerView.Adapter<WeatherRowAdapter.VH>() {

    private var items: List<CityWeatherRow> = emptyList()

    init {
        setHasStableIds(true)
    }

    fun submit(list: List<CityWeatherRow>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long = items[position].cityId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWeatherRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.binding.textCity.text = row.city
        holder.binding.viewCardBackground.setBackgroundResource(row.cardBackgroundRes)
        if (row.ok) {
            holder.binding.textError.visibility = View.GONE
            holder.binding.recyclerHours.visibility = View.VISIBLE
            holder.binding.textTemp.visibility = View.VISIBLE
            holder.binding.textCondition.visibility = View.VISIBLE
            holder.binding.textHighLow.visibility = View.VISIBLE
            holder.binding.textTemp.text = "${row.tempC}°"
            holder.binding.textCondition.text = row.conditionLabel
            holder.binding.textHighLow.text = row.highLowLabel
            holder.hourAdapter.submit(row.hourly)
        } else {
            holder.binding.textError.visibility = View.VISIBLE
            holder.binding.recyclerHours.visibility = View.GONE
            holder.binding.textTemp.visibility = View.GONE
            holder.binding.textCondition.visibility = View.GONE
            holder.binding.textHighLow.visibility = View.GONE
            holder.binding.textError.text = row.errorMessage
            holder.hourAdapter.submit(emptyList())
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemWeatherRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val hourAdapter = HourListAdapter()

        init {
            binding.recyclerHours.layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerHours.adapter = hourAdapter
            binding.recyclerHours.isNestedScrollingEnabled = false
        }
    }

    private class HourListAdapter : RecyclerView.Adapter<HourListAdapter.HourVH>() {

        private var hours: List<HourChipUi> = emptyList()

        fun submit(list: List<HourChipUi>) {
            hours = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourVH {
            val binding = ItemWeatherHourBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return HourVH(binding)
        }

        override fun onBindViewHolder(holder: HourVH, position: Int) {
            val h = hours[position]
            holder.binding.textClock.text = h.clock
            holder.binding.textTemp.text = "${h.tempC}°"
            holder.binding.iconWx.setImageResource(h.iconRes)
        }

        override fun getItemCount(): Int = hours.size

        class HourVH(val binding: ItemWeatherHourBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
