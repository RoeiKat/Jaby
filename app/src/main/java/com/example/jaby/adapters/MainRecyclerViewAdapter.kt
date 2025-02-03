package com.example.jaby.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jaby.databinding.ItemMainRecyclerViewBinding

class MainRecyclerViewAdapter(private val listener: Listener) : RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {

    // Using a non-nullable list initialized to empty to avoid null checks
    private var devicesList: List<Pair<String, String>> = emptyList()

    fun updateList(list: List<Pair<String, String>>) {
        devicesList = list
        notifyDataSetChanged()
        Log.d("MainRecyclerViewAdapter", "List updated with size: ${devicesList.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val binding = ItemMainRecyclerViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MainRecyclerViewHolder(binding)
    }

    override fun getItemCount(): Int = devicesList.size

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        val device = devicesList[position]
        holder.bind(device, {
            listener.onVideoCallClicked(it)
        }, {
            listener.onAudioCallClicked(it)
        })
    }

    interface Listener {
        fun onVideoCallClicked(deviceName: String)
        fun onAudioCallClicked(deviceName: String)
    }

    class MainRecyclerViewHolder(private val binding: ItemMainRecyclerViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Pair<String, String>, videoCallClicked: (String) -> Unit, audioCallClicked: (String) -> Unit) {
            Log.d("ViewAdapter", "binding ${device.first} and ${device.second}")
            binding.apply {
                statusTv.text = when (device.second) {
                    "ONLINE" -> "Online"
                    "OFFLINE" -> "Offline"
                    "IN_CALL" -> "In Call"
                    else -> "Unknown Status"
                }
                deviceNameTv.text = device.first
                Log.d("ViewHolder", "Binding device: ${device.first}, Status: ${device.second}")
            }
        }
    }
}
