package com.example.jaby

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jaby.databinding.ActivityMonitorBinding
import com.example.jaby.repository.MainRepository
import com.example.jaby.service.MainService
import com.example.jaby.service.MainServiceRepository
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.example.jaby.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject


@AndroidEntryPoint
class MonitorActivity : AppCompatActivity(), MainService.Listener {
    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var mainServiceRepository: MainServiceRepository

    //Used by watching device
    private var monitorDevice:String?=null
    //Used by both devices
    private var userId:String?=null
    private var isMonitor:Boolean = false
    private var currentDevice:String?=null
    private var isEarSpeakerMode = false
    //Surface view renderers
    private var remoteViewRenderer: SurfaceViewRenderer? = null
    private var localViewRenderer: SurfaceViewRenderer? = null

    private lateinit var views:ActivityMonitorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMonitorBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        intent.getStringExtra("device")?.let{
            this.monitorDevice = it
        }?: kotlin.run {
            finish()
        }
        intent.getStringExtra("userId")?.let {
            this.userId = it
        }?: kotlin.run {
            finish()
        }
        isMonitor = intent.getBooleanExtra("isMonitor",false)
        currentDevice = mainRepository.getCurrentDevice()
        if(currentDevice.isNullOrEmpty()) {
            kotlin.run {
                finish()
            }
        }
        // New Code for both devices service operation
        startMyService()
        MainService.listener = this
        mainRepository.initWebrtcClient(currentDevice!!)
        views.apply {
            localViewRenderer = localView
            remoteViewRenderer = remoteView
            MainService.localSurfaceView = localViewRenderer
            MainService.remoteSurfaceView = remoteViewRenderer
            mainServiceRepository.setUpViews(currentDevice!!,userId!!)
            setupToggleAudioDevice()
            if(!isMonitor) {
                //Views for watcher
                monitorTitleTv.text = "Watching Device $monitorDevice"
                endMonitorButton.setOnClickListener{
                    mainServiceRepository.sendEndStreaming()
                    removeWatcher()
                }
                //Send watching request
                mainServiceRepository.sendStartWatching(monitorDevice!!)
            } else {
                //Views for monitor
                monitorTitleTv.text = "Monitoring on $monitorDevice"
                endMonitorButton.setOnClickListener{
                    mainServiceRepository.sendEndStreaming()
                    removeDevice()
                }
            }
        }
        // Old code
//        if(isMonitor) {
//            startMyService(monitorDevice!!)
//            MainService.listener = this
//            mainRepository.initWebrtcClient(monitorDevice!!)
//            views.apply {
//                localViewRenderer = localView
//                remoteViewRenderer = remoteView
//                MainService.remoteSurfaceView = remoteViewRenderer
//                MainService.localSurfaceView = localViewRenderer
//                mainServiceRepository.setUpViews(monitorDevice!!,userId!!)
//
//            }
//        } else {
//            currentDevice = mainRepository.getCurrentDevice()
////            startMyService(currentDevice!!)
//            MainService.listener = this
//            mainRepository.initWebrtcClient(currentDevice!!)
//            views.apply {
//                localViewRenderer = localView
//                remoteViewRenderer = remoteView
//                MainService.remoteSurfaceView = remoteViewRenderer
//                MainService.localSurfaceView = localViewRenderer
//                mainServiceRepository.setUpViews(monitorDevice!!,userId!!)
//
//            }
//            mainRepository.setIsMonitor(false)
//            mainRepository.listener = this
//            mainRepository.initFirebase()
//            mainRepository.setTarget(monitorDevice!!)
//            mainRepository.initWebrtcClient(currentDevice!!)
//            views.apply {
//                localViewRenderer = localView
//                remoteViewRenderer = remoteView
//                mainRepository.initLocalSurfaceView(localViewRenderer!!, isMonitor)
                // only for debugging isMonitor = true , to video call the 2 devices
//                mainRepository.initLocalSurfaceView(localViewRenderer!!, true)
//                mainRepository.initRemoteSurfaceView(remoteViewRenderer!!)
//                monitorTitleTv.text = "Monitoring on Device $monitorDevice"
//                endMonitorButton.setOnClickListener{
//                    removeWatcher()
//                }
//            }
//            mainRepository.sendStartWatching()
//        }


    }

//    override fun onLatestEventReceived(data: DataModel) {
//        if(data.type == DataModelType.EndMonitoring) {
//            removeWatcher()
//        } else if(data.type == DataModelType.EndWatching){
//            remoteViewRenderer?.clearImage()
//            remoteViewRenderer?.release()
//            remoteViewRenderer = null
//            mainRepository.closeWebRTCConnection()
//            mainRepository.initWebrtcClient(monitorDevice!!)
//        }
//    }

    private fun setupToggleAudioDevice(){
//        views.apply {  }
        if(isEarSpeakerMode) {
            //Set it to earpiece mode
            mainServiceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)
        } else {
            //Set it to speaker mode
            mainServiceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)
        }
    }

    private fun endMyService(){
        mainServiceRepository.endService()
    }

    private fun startMyService() {
        mainServiceRepository.startService(userId!!,currentDevice!!, isMonitor)
    }


    override fun onEndMonitoringReceived() {
        removeWatcher()
    }
    override fun onEndWatchingReceived() {
        MainService.remoteSurfaceView?.clearImage()
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null
        MainService.localSurfaceView?.clearImage()
        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null

        mainRepository.closeWebRTCConnection()
        mainRepository.initWebrtcClient(currentDevice!!)
        MainService.localSurfaceView = localViewRenderer
        MainService.remoteSurfaceView = remoteViewRenderer
        mainServiceRepository.setUpViews(currentDevice!!,userId!!)
    }

    override fun onWatchRequestReceived(model: DataModel) {
        mainRepository.setTarget(model.sender!!)
        mainRepository.sendConnectionRequest()
    }

    private fun removeWatcher() {
        mainRepository.removeWatcher(){isDone,reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                removeData()
                moveToMainActivity()
                finish()
            }
        }
    }

    private fun removeDevice() {
        mainRepository.removeDevice(currentDevice!!){isDone,reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                removeData()
                moveToMainActivity()
                finish()
            }
        }
    }

    private fun moveToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun removeData() {
            MainService.remoteSurfaceView?.release()
            MainService.remoteSurfaceView = null
            MainService.localSurfaceView?.release()
            MainService.localSurfaceView = null
            endMyService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isFinishing) {
            removeData()
        }
    }
}