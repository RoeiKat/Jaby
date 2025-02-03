package com.example.jaby.ui.home

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
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        Log.d("HomeFragment", "Setting up RecyclerView")
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.mainRecyclerView.layoutManager = layoutManager
        binding.mainRecyclerView.adapter = mainAdapter
        Log.d("HomeFragment", "RecyclerView setup complete")
    }


    fun updateDevices(devicesList: List<Pair<String, String>>) {
        Log.d("HomeFragment", "Updating List with ${devicesList[0].first}")
        mainAdapter?.updateList(devicesList)
        Log.d("Main Adapter", "item count: ${mainAdapter?.itemCount}")
    }

    override fun onAudioCallClicked(deviceName: String) {
        TODO("Not yet implemented")
    }

    override fun onVideoCallClicked(deviceName: String) {
        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
