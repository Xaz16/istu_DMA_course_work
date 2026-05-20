package edu.istu.achipiga.coursework.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import edu.istu.achipiga.coursework.CourseWorkApp
import edu.istu.achipiga.coursework.R
import edu.istu.achipiga.coursework.data.WeatherRepository
import edu.istu.achipiga.coursework.databinding.DialogAddCityBinding
import edu.istu.achipiga.coursework.databinding.FragmentWeatherBinding
import kotlinx.coroutines.launch

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by activityViewModels {
        val app = requireActivity().application as CourseWorkApp
        WeatherViewModel.Factory(
            requireActivity().application,
            WeatherRepository(),
            app.database.savedCityDao()
        )
    }

    private lateinit var adapter: WeatherRowAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_refresh) {
                viewModel.refresh()
                true
            } else {
                false
            }
        }

        val primary = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorPrimary,
            Color.BLACK
        )
        binding.swipeRefresh.setColorSchemeColors(primary)

        adapter = WeatherRowAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.fabAdd.setOnClickListener { showAddCityDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.loading
                    binding.textEmpty.visibility =
                        if (state.isEmpty && !state.loading) View.VISIBLE else View.GONE
                    if (state.error != null) {
                        binding.textError.visibility = View.VISIBLE
                        binding.textError.text = state.error
                    } else {
                        binding.textError.visibility = View.GONE
                    }
                    adapter.submit(state.rows)
                    state.snackbarMessage?.let { text ->
                        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
                        viewModel.clearSnackbar()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showAddCityDialog() {
        val dialogBinding = DialogAddCityBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_city_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.add_city_cancel, null)
            .setPositiveButton(R.string.add_city_positive) { _, _ ->
                viewModel.addCity(dialogBinding.inputCityName.text?.toString().orEmpty())
            }
            .show()
    }
}
