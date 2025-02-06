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
    private var username: String? = null

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
        val target = incomingIntent.getStringExtra("target")

        mainRepository.setTarget(target!!)
        //initialize our widgets and start streaming our video and audio source
        //and get prepared for call
        mainRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
        if(!isCaller) {
            //start the video call
            mainRepository.startCall()
        }
    }

    private fun handleStartService(incomingIntent: Intent) {
        //Start our foreground service
        if(!isServiceRunning) {
            isServiceRunning = true
            username = incomingIntent.getStringExtra("username")
            startServiceWithNotification()
            Log.d("Service", "Main service started")

            //setup my clients
            mainRepository.listener = this
            mainRepository.initFirebase()
            mainRepository.initWebrtcClient(username!!)
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
                .setContentTitle("Service Running")
                .setContentText("Your foreground service is active.")
                .setOngoing(true) // Mark as ongoing so it's not dismissible
                .build()
            startForeground(1, notification)
        }
    }

    override fun onLatestEventReceived(data: DataModel) {
        Log.d(TAG,"onLatestEventReceived: $data")
        if(data.isValid()) {
            when(data.type){
                DataModelType.StartVideoCall,
                    DataModelType.StartAudioCall -> {
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