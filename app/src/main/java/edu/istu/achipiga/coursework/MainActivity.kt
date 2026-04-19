package edu.istu.achipiga.coursework

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import edu.istu.achipiga.coursework.data.WeatherRepository
import edu.istu.achipiga.coursework.databinding.ActivityMainBinding
import edu.istu.achipiga.coursework.ui.WeatherRowAdapter
import edu.istu.achipiga.coursework.ui.WeatherViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModel.Factory(application, WeatherRepository())
    }
    private lateinit var adapter: WeatherRowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = WeatherRowAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.buttonRefresh.setOnClickListener { viewModel.refresh() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE
                    binding.buttonRefresh.isEnabled = !state.loading
                    if (state.error != null) {
                        binding.textError.visibility = View.VISIBLE
                        binding.textError.text = state.error
                    } else {
                        binding.textError.visibility = View.GONE
                    }
                    adapter.submit(state.rows)
                }
            }
        }
    }
}
