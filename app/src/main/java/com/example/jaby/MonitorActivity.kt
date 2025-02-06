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
    private var target:String?=null
    private var isVideoCall:Boolean = true
    private var isCaller:Boolean = true

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
        intent.getStringExtra("target")?.let{
            this.target = it
        }?: kotlin.run {
            finish()
        }
        intent.getStringExtra("userId")?.let {
            this.userId = it
        }?: kotlin.run {
            finish()
        }
        isVideoCall = intent.getBooleanExtra("isVideoCall",true)
        isCaller = intent.getBooleanExtra("isCaller",true)

        views.apply {
            monitorTitleTv.text = "Monitoring on Device $target"
            endMonitorButton.setOnClickListener{
                if(!isCaller){
                    removeDevice()
                } else {
                    //This is not from the monitoring device so go back to main activity.
                    startActivity(Intent(this@MonitorActivity, MainActivity::class.java))
                }
            }
            if(!isCaller) {
                startMyService()
                // This is when a phone is being used as a monitor
//                MainService.remoteSurfaceView = remoteView
//                mainServiceRepository.setUpViews(isVideoCall,isCaller,target!!,userId!!)
            } else {

            }
//            MainService.remoteSurfaceView = remoteView
//            MainService.localSurfaceView = localView
//            mainServiceRepository.setUpViews(isVideoCall,isCaller,target!!,userId!!)
        }
    }

    private fun startMyService() {
        mainServiceRepository.startService(userId!!)
    }

    override fun onCallReceived(model: DataModel) {
//        val isVideoCall = model.type  == DataModelType.StartVideoCall
        val isVideoCall = true

//        runOnUiThread {
//            binding.apply {
//                val isVideoCall = model.type  == DataModelType.StartVideoCall
//                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
//                incomingCallTitleTv.text = "${model.sender} is ${isVideoCallText} calling you"
//                incomingCallLayout.isVisible = true
//                  acceptButton.setOnClickListener {
//                      geCameraAndMicPermission {
//                          incomingCallLayout.isVisible = false
//                          //create an intent to go to video call activity
//
//                      }
//                  }
//                  declineButton.setOnClickListener {
//                      incomingCallLayout.isVisible = false
//                  }
//            }
//        }
    }

    private fun removeDevice() {
        mainRepository.removeDevice(target!!){isDone,reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}