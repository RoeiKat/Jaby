package com.example.jaby

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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MonitorActivity : AppCompatActivity(), MainService.Listener {
    //isCaller = false means that this device is a monitor, isCaller = true means that this is a viewer
    private var userId:String?=null
    private var device:String?=null
    private var watcherId:String?=null
    private var isMonitor:Boolean = false

    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var mainServiceRepository: MainServiceRepository


    private lateinit var views:ActivityMonitorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMonitorBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init() {
        intent.getStringExtra("device")?.let{
            this.device = it
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
            startMyService()
            MainService.listener = this
            views.apply {
                monitorTitleTv.text = "Monitoring on Device $device"
                endMonitorButton.setOnClickListener{
                    removeDevice()
                }
                MainService.remoteSurfaceView = remoteView
                MainService.localSurfaceView = localView
                mainServiceRepository.setUpViews(device!!,userId!!,isMonitor)
            }
        } else {
            watcherId = mainRepository.getCurrentWatcherId()
            mainRepository.setFirebaseCurrentDevice(device!!)
            mainRepository.initWebrtcClient(watcherId!!)
            views.apply {
                monitorTitleTv.text = "Monitoring on Device $device"
                endMonitorButton.setOnClickListener{
                    startActivity(Intent(this@MonitorActivity, MainActivity::class.java))
                }
                mainRepository.initLocalSurfaceView(remoteView, isMonitor)
                mainRepository.initRemoteSurfaceView(localView)
                mainRepository.startCall(device!!)
            }
        }


    }

    private fun startMyService() {
        mainServiceRepository.startService(userId!!,device!!)
    }

    override fun onWatchRequestReceived(model: DataModel) {
        Log.d("WatchReqReceived", "${model.toString()}")

    }

    private fun removeDevice() {
        mainRepository.removeDevice(device!!){isDone,reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}