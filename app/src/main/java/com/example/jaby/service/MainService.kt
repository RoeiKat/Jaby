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
    private var device: String? = null

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
        Log.d("NotificationManager","Initalized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{ incomingIntent ->
            when(incomingIntent.action){
                MainServiceActions.START_SERVICE.name -> handleStartService(incomingIntent)
                MainServiceActions.SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                else -> Unit

            }
        }

        return START_STICKY
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        val isCaller = incomingIntent.getBooleanExtra("isCaller", false)
        val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall", false)
        val device = incomingIntent.getStringExtra("device")

        mainRepository.setTarget(device!!)
        //initialize our widgets and start streaming our video and audio source
        //and get prepared for call
        mainRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
        if(!isCaller) {
            //start the video call
            mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
            mainRepository.startCall()
        }
    }

    private fun handleStartService(incomingIntent: Intent) {
        //Start our foreground service
        if(!isServiceRunning) {
            isServiceRunning = true
            device = incomingIntent.getStringExtra("device")
            startServiceWithNotification()
            Log.d("Service", "Main service started")

            //setup my clients
            mainRepository.listener = this
            mainRepository.setTarget(device!!)
            mainRepository.initFirebase()
            mainRepository.initWebrtcClient(device!!)
        }
    }

    private fun startServiceWithNotification(){
        val channelId = "foreground_channel_v2" // Changed channel id
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
                .setOngoing(true) // Mark as ongoing so it's not dismissible
                .build()
            startForeground(1, notification)
        }
    }

    override fun onLatestEventReceived(data: DataModel) {
        Log.d(TAG,"onLatestEventReceived: $data")
        if(data.isValid()) {
            Log.d("DataModelType", "Data is valid")
            when(data.type){
                DataModelType.StartVideoCall,
                    DataModelType.StartAudioCall -> {
                    Log.d("DataModelType", "Received call ${data.type}")
                    listener?.onCallReceived(data)
                    }
                else -> Unit
            }
        }
    }

    override fun endCall() {

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    interface Listener {
        fun onCallReceived(model:DataModel)
    }
}