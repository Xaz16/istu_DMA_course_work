package edu.istu.achipiga.coursework.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.istu.achipiga.coursework.databinding.ItemWeatherRowBinding

class WeatherRowAdapter : RecyclerView.Adapter<WeatherRowAdapter.VH>() {

    private var items: List<CityWeatherRow> = emptyList()

    fun submit(list: List<CityWeatherRow>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWeatherRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.binding.textCity.text = row.city
        holder.binding.textLine.text = row.text
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemWeatherRowBinding) : RecyclerView.ViewHolder(binding.root)
}
