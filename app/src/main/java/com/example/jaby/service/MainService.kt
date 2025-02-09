package com.example.jaby.service

import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.AndroidEntryPoint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract.Data
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jaby.R
import com.example.jaby.repository.MainRepository
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.example.jaby.utils.isValid
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@AndroidEntryPoint
class MainService: Service(),MainRepository.Listener {
    private val TAG = "MainService"

    private var isServiceRunning = false
    private var monitorDevice: String? = null

    private lateinit var notificationManager: NotificationManager


    @Inject lateinit var mainRepository: MainRepository

    companion object {
        var listener:Listener?=null
        var localSurfaceView: SurfaceViewRenderer? = null
        var remoteSurfaceView: SurfaceViewRenderer? = null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{ incomingIntent ->
            when(incomingIntent.action){
                MainServiceActions.START_SERVICE.name -> handleStartService(incomingIntent)
                MainServiceActions.SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                MainServiceActions.STOP_SERVICE.name -> handleStopService()
                MainServiceActions.END_MONITORING.name -> handleEndMonitoring()
                else -> Unit

            }
        }

        return START_STICKY
    }

    private fun handleEndMonitoring() {
        //1. we have to send a signal to other peer that call is ended
        mainRepository.sendEndMonitoring()
        //2.end out call process and restart our webrtc client
        endMonitoringAndRestartRepository()
    }

    private fun endMonitoringAndRestartRepository() {
        mainRepository.closeWebRTCConnection()
        mainRepository.initWebrtcClient("null")
    }

    private fun handleSetupViews(incomingIntent: Intent) {
//        val userId = incomingIntent.getStringExtra("userId")
//        val monitorDevice = incomingIntent.getStringExtra("device")
        val isMonitor = incomingIntent.getBooleanExtra("isMonitor", false)
        mainRepository.initLocalSurfaceView(localSurfaceView!!, isMonitor)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
    }

    private fun handleStopService() {
        isServiceRunning = false
        stopForeground(true)
        stopSelf()
    }

    private fun handleStartService(incomingIntent: Intent) {
        //Start our foreground service
        if(!isServiceRunning) {
            isServiceRunning = true
            monitorDevice = incomingIntent.getStringExtra("device")
            startServiceWithNotification()
            //setup my clients
            mainRepository.listener = this
            mainRepository.initFirebase()
            mainRepository.initWebrtcClient(monitorDevice!!)
        }
    }

    private fun startServiceWithNotification(){
        val channelId = "foreground_channel_v2"
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                "Foreground Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Monitoring Service Running")
                .setContentText("Currently Monitoring on the device.")
                .setOngoing(true)
                .build()
            startForeground(1, notification)
        }
    }

    override fun onLatestEventReceived(data: DataModel) {
        if(data.isValid()) {
            when(data.type){
                DataModelType.StartWatching-> {
                    listener?.onWatchRequestReceived(data)
                    }
                else -> Unit
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    interface Listener {
        fun onWatchRequestReceived(model:DataModel)
    }
}