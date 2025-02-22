package com.example.jaby

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.jaby.databinding.ActivityMonitorBinding
import com.example.jaby.repository.MainRepository
import com.example.jaby.service.MainService
import com.example.jaby.service.MainServiceRepository
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject


@AndroidEntryPoint
class MonitorActivity : AppCompatActivity(), MainService.Listener,MainRepository.Listener {
    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var mainServiceRepository: MainServiceRepository

    private var userId:String?=null
    private var monitorDevice:String?=null
    private var isMonitor:Boolean = false
    private var currentDevice:String?=null
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
        if(isMonitor) {
            startMyService(monitorDevice!!)
            MainService.listener = this
            mainRepository.initWebrtcClient(monitorDevice!!)
            views.apply {
                localViewRenderer = localView
                remoteViewRenderer = remoteView
                MainService.remoteSurfaceView = remoteViewRenderer
                MainService.localSurfaceView = localViewRenderer
                mainServiceRepository.setUpViews(monitorDevice!!,userId!!)
                monitorTitleTv.text = "Monitoring on Device $monitorDevice"
                endMonitorButton.setOnClickListener{
                    mainServiceRepository.sendEndMonitoring()
                    removeDevice()
                }
            }
        } else {
            currentDevice = mainRepository.getCurrentDevice()
            startMyService(currentDevice!!)
            MainService.listener = this
            mainRepository.initWebrtcClient(currentDevice!!)
            views.apply {
                localViewRenderer = localView
                remoteViewRenderer = remoteView
                MainService.remoteSurfaceView = remoteViewRenderer
                MainService.localSurfaceView = localViewRenderer
                mainServiceRepository.setUpViews(monitorDevice!!,userId!!)
                monitorTitleTv.text = "Monitoring on Device $monitorDevice"
                endMonitorButton.setOnClickListener{
                    removeWatcher()
                }
            }
            mainRepository.sendStartWatching()
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
        }


    }

    override fun onLatestEventReceived(data: DataModel) {
        if(data.type == DataModelType.EndMonitoring) {
            removeWatcher()
        } else if(data.type == DataModelType.EndWatching){
            remoteViewRenderer?.clearImage()
            remoteViewRenderer?.release()
            remoteViewRenderer = null
            mainRepository.closeWebRTCConnection()
            mainRepository.initWebrtcClient(monitorDevice!!)
        }
    }

    private fun endMyService(){
        mainServiceRepository.endService()
    }

    private fun startMyService(device:String) {
        mainServiceRepository.startService(userId!!,device, isMonitor)
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
                endMyService()
                moveToMainActivity()
                finish()
            }
        }
    }

    private fun removeDevice() {
        mainRepository.removeDevice(monitorDevice!!){isDone,reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                moveToMainActivity()
                finish()
            }
        }
    }

    private fun moveToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun removeData() {
        if(isMonitor) {
            MainService.remoteSurfaceView?.release()
            MainService.remoteSurfaceView = null
            MainService.localSurfaceView?.release()
            MainService.localSurfaceView = null
            mainRepository.removeDevice(monitorDevice!!) { isDone, reason ->
                if (isDone) {
                    Log.d("MonitorActivity", "Device '$monitorDevice' removed successfully.")
                } else {
                    Log.e("MonitorActivity", "Failed to remove device '$monitorDevice': $reason")
                }
            }
            endMyService()
        } else {
            localViewRenderer?.release()
            localViewRenderer = null
            remoteViewRenderer?.release()
            remoteViewRenderer = null
            mainRepository.removeWatcher(){isDone,reason ->
                if (isDone) {
                    Log.d("MonitorActivity", "Watcher '$currentDevice' removed successfully.")
                } else {
                    Log.e("MonitorActivity", "Failed to remove device '$currentDevice': $reason")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isFinishing) {
            removeData()
        }
    }
}