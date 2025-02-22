package com.example.jaby.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import javax.inject.Inject

class MainServiceRepository @Inject constructor(
    private val context: Context
) {

    fun startService(userId:String, device:String, isMonitor: Boolean) {
        Thread{
            val intent = Intent(context, MainService::class.java)
            intent.putExtra("userId",userId)
            intent.putExtra("device",device)
            intent.putExtra("isMonitor",isMonitor)
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

    fun endService() {
        val stopIntent = Intent(context, MainService::class.java).apply {
            action = MainServiceActions.STOP_SERVICE.name
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(stopIntent)
        } else {
            context.startService(stopIntent)
        }
    }

    fun setUpViews(device: String, userId:String){
        val intent = Intent(context,MainService::class.java)
        intent.apply {
            action = MainServiceActions.SETUP_VIEWS.name
            putExtra("userId",userId)
            putExtra("device",device)
        }
        startServiceIntent(intent)
    }

    fun sendEndMonitoring() {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.END_MONITORING.name
        startServiceIntent(intent)
    }


}