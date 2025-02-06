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
        isVideoCall = intent.getBooleanExtra("isVideoCall",true)
        isCaller = intent.getBooleanExtra("isCaller",true)

        if(!isCaller) {
            // This is when a phone is being used as a monitor
            startMyService()
            MainService.listener = this
        } else {
            Log.d("Device", "$device")
            Log.d("UserId", "$userId")
            Log.d("isCaller", "$isCaller")
        }

        views.apply {
            monitorTitleTv.text = "Monitoring on Device $device"
            endMonitorButton.setOnClickListener{
                if(!isCaller){
                    //From the monitoring device
                    removeDevice()
                } else {
                    //This is not from the monitoring device so go back to main activity.
                    startActivity(Intent(this@MonitorActivity, MainActivity::class.java))
                }
            }
            MainService.remoteSurfaceView = remoteView
            MainService.localSurfaceView = localView
            mainServiceRepository.setUpViews(isVideoCall,isCaller,device!!,userId!!)
//            mainServiceRepository.setUpViews(isVideoCall,isCaller,device!!,userId!!)
        }

    }

    private fun startMyService() {
        mainServiceRepository.startService(device!!)
    }

    override fun onCallReceived(model: DataModel) {
        val isVideoCall = true
        Log.d("ReceivedCall", "${model.toString()}")
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
        mainRepository.removeDevice(device!!){isDone,reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}