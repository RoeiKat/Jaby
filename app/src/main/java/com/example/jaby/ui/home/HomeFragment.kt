package com.example.jaby.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jaby.MainActivity
import com.example.jaby.MonitorActivity
import com.example.jaby.R
import com.example.jaby.adapters.MainRecyclerViewAdapter
import com.example.jaby.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), MainRecyclerViewAdapter.Listener {

    private var _binding: FragmentHomeBinding? = null

    private var mainAdapter: MainRecyclerViewAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.mainRecyclerView.layoutManager = layoutManager
        binding.mainRecyclerView.adapter = mainAdapter
    }


    fun updateDevices(devicesList: List<Pair<String, String>>) {
        if (_binding == null) return

        mainAdapter?.updateList(devicesList)
        if (devicesList.isEmpty()) {
            binding.mainRecyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.mainRecyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }


    override fun onStartWatchClicked(deviceName: String) {
        (activity as? MainActivity)?.onStartWatchClicked(deviceName)
    }

    override fun onCloseMonitorClicked(deviceName: String) {
        (activity as? MainActivity)?.onCloseMonitorClicked(deviceName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
