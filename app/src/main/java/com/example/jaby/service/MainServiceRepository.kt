package com.example.jaby.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import javax.inject.Inject

class MainServiceRepository @Inject constructor(
    private val context: Context
) {

    fun startService(userId:String, device:String) {
        Thread{
            val intent = Intent(context, MainService::class.java)
            intent.putExtra("userId",userId)
            intent.putExtra("device",device)
            intent.action = MainServiceActions.START_SERVICE.name
            startServiceIntent(intent)
        }.start()
    }


    private fun startServiceIntent(intent: Intent){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        }else {
            context.startService(intent)
        }
    }

    fun setUpViews(device: String, userId:String,isMonitor: Boolean){
        val intent = Intent(context,MainService::class.java)
        intent.apply {
            action = MainServiceActions.SETUP_VIEWS.name
            putExtra("userId",userId)
            putExtra("device",device)
            putExtra("isMonitor", isMonitor)
        }
        startServiceIntent(intent)
    }


}