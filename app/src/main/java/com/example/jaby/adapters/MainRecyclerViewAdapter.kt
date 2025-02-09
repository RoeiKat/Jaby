package com.example.jaby.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.jaby.MonitorActivity
import com.example.jaby.databinding.ItemMainRecyclerViewBinding

class MainRecyclerViewAdapter(private val listener:Listener) : RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {

    private var devicesList:List<Pair<String,String>>?=null
    fun updateList(list:List<Pair<String,String>>){
        this.devicesList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val binding = ItemMainRecyclerViewBinding.inflate(
            LayoutInflater.from(parent.context),parent,false
        )
        return MainRecyclerViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return devicesList?.size?:0
    }

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        devicesList?.let { list->
            val user = list[position]
            holder.bind(user,{
                listener.onStartWatchClicked(it)
            })
        }
    }

    interface  Listener {
        fun onStartWatchClicked(deviceName:String)
    }

    class MainRecyclerViewHolder(private val binding: ItemMainRecyclerViewBinding):
        RecyclerView.ViewHolder(binding.root){
        private val context = binding.root.context

        fun bind(
            device:Pair<String,String>,
            startWatchClicked:(String) -> Unit,
        ){
            binding.apply {
                when (device.second) {
                    "ONLINE" -> {
                        monitorCard.setOnClickListener {
                            startWatchClicked.invoke(device.first)
                        }
                        statusTv.text = "Online"
                    }
                    "OFFLINE" -> {
                        statusTv.text = "Offline"
                    }
                    "IN_CALL" -> {
                        statusTv.text = "In Call"
                    }
                }

                deviceNameTv.text = device.first
            }
        }
    }
}
