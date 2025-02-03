package com.example.jaby.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jaby.databinding.ItemMainRecyclerViewBinding

class MainRecyclerViewAdapter(private val listener:Listener): RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {

    private var devicesList:List<Pair<String,String>>? = null

    fun updateList(list:List<Pair<String,String>>) {
        this.devicesList = list
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
        devicesList?.let { list->{
            val device = list[position]
            holder.bind(device,{
                listener.onVideoCallClicked(it)
            },{
                listener.onAudioCallClicked(it)
            })
        } }
    }

    interface Listener {
        fun onVideoCallClicked(deviceName: String)
        fun onAudioCallClicked(deviceName: String)
    }

    class MainRecyclerViewHolder(private val binding: ItemMainRecyclerViewBinding):
            RecyclerView.ViewHolder(binding.root){
                private val context = binding.root.context


            fun bind(
                device:Pair<String,String>,
                videoCallClicked:(String) -> Unit,
                audioCallClicked:(String) -> Unit
            ){

            }
            }
}