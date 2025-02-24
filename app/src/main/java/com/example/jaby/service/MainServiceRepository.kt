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
        }
        startServiceIntent(intent)
    }

    fun sendSwitchMonitorCamera() {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.SEND_SWITCH_CAMERA.name
        startServiceIntent(intent)
    }

    fun sendStartWatching(target: String) {
        val intent = Intent(context,MainService::class.java)
        intent.apply {
            action = MainServiceActions.START_WATCHING.name
            intent.putExtra("target", target)
        }
        startServiceIntent(intent)
    }

    fun sendEndStreaming() {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.END_STREAMING.name
        startServiceIntent(intent)
    }

    fun switchCamera() {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.SWITCH_CAMERA.name
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_VIDEO.name
        intent.putExtra("shouldBeMuted",shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO.name
        intent.putExtra("shouldBeMuted",shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleAudioDevice(type: String) {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO_DEVICE.name
        intent.putExtra("type",type)
        startServiceIntent(intent)
    }

}