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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.jaby.R
import com.example.jaby.repository.MainRepository
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.example.jaby.utils.isValid
import com.example.jaby.webrtc.RTCAudioManager
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@AndroidEntryPoint
class MainService: Service(),MainRepository.Listener {
    private val TAG = "MainService"

    private var isMonitor = false
    private var isServiceRunning = false

    private var userId: String? = null
    private var device: String? = null
    private var target: String? = null


    private lateinit var notificationManager: NotificationManager
    private lateinit var rtcAudioManager: RTCAudioManager


    @Inject lateinit var mainRepository: MainRepository

    companion object {
        var listener:Listener?=null
        var localSurfaceView: SurfaceViewRenderer? = null
        var remoteSurfaceView: SurfaceViewRenderer? = null
    }

    override fun onCreate() {
        super.onCreate()
        rtcAudioManager = RTCAudioManager.create(this)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{ incomingIntent ->
            when(incomingIntent.action){
                MainServiceActions.START_SERVICE.name -> handleStartService(incomingIntent)
                MainServiceActions.SETUP_VIEWS.name -> handleSetupViews()
                MainServiceActions.STOP_SERVICE.name -> handleStopService()
                MainServiceActions.START_WATCHING.name -> handleStartWatching(incomingIntent)
                MainServiceActions.SWITCH_CAMERA.name -> handleSwitchCamera()
                MainServiceActions.TOGGLE_AUDIO.name -> handleToggleAudio(incomingIntent)
                MainServiceActions.SEND_SWITCH_CAMERA.name -> handleSendSwitchCamera()
                MainServiceActions.SEND_TOGGLE_AUDIO_ON.name -> handleSendToggleAudioOn()
                MainServiceActions.SEND_TOGGLE_AUDIO_OFF.name -> handleSendToggleAudioOff()
                MainServiceActions.END_STREAMING.name -> handleEndStreaming()
                MainServiceActions.TOGGLE_AUDIO_DEVICE.name -> handleToggleAudioDevice(incomingIntent)

                else -> Unit

            }
        }

        return START_STICKY
    }

    private fun handleSendToggleAudioOn() {
        mainRepository.sendToggleAudioOn()
    }

    private fun handleSendToggleAudioOff() {
        mainRepository.sendToggleAudioOff()
    }

    private fun handleSendSwitchCamera() {
        mainRepository.sendSwitchMonitorCamera()
    }

    private fun handleSwitchCamera() {
        mainRepository.switchCamera()
    }

    private fun handleStartWatching(incomingIntent: Intent) {
        target = incomingIntent.getStringExtra("target")
        if(target.isNullOrEmpty()) {
            return
        } else {
            mainRepository.setTarget(target!!)
            mainRepository.sendStartWatching()
        }
    }

    private fun handleEndStreaming() {
        //1. We have to send a signal to other peer to tell that call is ended
        if(isMonitor) {
            mainRepository.sendEndMonitoring()
        } else {
            mainRepository.sendEndWatching()
        }
        //2. End our monitor process and restart our webrtc client
//        endMonitoringAndRestartRepository()
    }

    private fun endMonitoringAndRestartRepository() {
        mainRepository.closeWebRTCConnection()
//        mainRepository.initWebrtcClient(device!!)
    }

    private fun handleSetupViews() {
        mainRepository.initLocalSurfaceView(localSurfaceView!!, isMonitor)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted",true)
        Log.d(TAG,shouldBeMuted.toString())
        mainRepository.toggleAudio(shouldBeMuted)
    }

    private fun handleToggleAudioDevice(incomingIntent: Intent) {
        val type = when(incomingIntent.getStringExtra("type")){
            RTCAudioManager.AudioDevice.EARPIECE.name -> RTCAudioManager.AudioDevice.EARPIECE
            RTCAudioManager.AudioDevice.SPEAKER_PHONE.name -> RTCAudioManager.AudioDevice.SPEAKER_PHONE
            else -> null
        }
        type?.let {
            rtcAudioManager.setDefaultAudioDevice(it)
            rtcAudioManager.selectAudioDevice(it)
            Log.d(TAG,"handleToggleAudioDevice: $it")
        }
    }

    private fun handleStopService() {
        isServiceRunning = false
        stopSelf()
    }

    private fun handleStartService(incomingIntent: Intent) {
        //Start our foreground service
        if(!isServiceRunning) {
            isServiceRunning = true
            userId = incomingIntent.getStringExtra("userId")
            device = incomingIntent.getStringExtra("device")
            isMonitor = incomingIntent.getBooleanExtra("isMonitor", false)

            startServiceWithNotification()
            mainRepository.setIsMonitor(isMonitor)
            mainRepository.listener = this
            mainRepository.initFirebase()
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
                DataModelType.EndWatching-> {
                    listener?.onEndWatchingReceived()
                }
                DataModelType.EndMonitoring-> {
                    listener?.onEndMonitoringReceived()
                }
                DataModelType.CloseMonitor -> {
                    listener?.closeMonitorReceived()
                }
                DataModelType.SwitchMonitorCamera -> {
                    listener?.onSwitchCameraReceived()
                }
                DataModelType.ToggleMonitorAudioOn -> {
                    listener?.onToggleMonitorAudioOnReceived()
                }
                DataModelType.ToggleMonitorAudioOff -> {
                    listener?.onToggleMonitorAudioOffReceived()
                }
                else -> Unit
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    interface Listener {
        fun onSwitchCameraReceived()
        fun onToggleMonitorAudioOnReceived()
        fun onToggleMonitorAudioOffReceived()
        fun onEndWatchingReceived()
        fun onEndMonitoringReceived()
        fun closeMonitorReceived()
        fun onWatchRequestReceived(model:DataModel)
    }
}