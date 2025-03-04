package com.example.jaby

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
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
    private var isMonitorMicrophoneMuted = true
    //Used by both devices
    private var userId:String?=null
    private var isMonitor:Boolean = false
    private var currentDevice:String?=null
    private var isEarSpeakerMode = false
    private var isMicrophoneMuted = true
    //Surface view renderers
    private var remoteViewRenderer: SurfaceViewRenderer? = null
    private var localViewRenderer: SurfaceViewRenderer? = null


    //Blink animation for record img
    private val blinkAnimation = AlphaAnimation(0.0f, 1.0f).apply {
        duration = 500
        startOffset = 20
        repeatMode = Animation.REVERSE
        repeatCount = Animation.INFINITE
    }

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
            localViewRenderer = monitorView
            remoteViewRenderer = remoteView
            if(isMonitor) {
                MainService.localSurfaceView = localViewRenderer
                MainService.remoteSurfaceView = remoteViewRenderer
            } else {
                MainService.localSurfaceView = remoteViewRenderer
                MainService.remoteSurfaceView = localViewRenderer
            }
            mainServiceRepository.setUpViews(currentDevice!!,userId!!)

            setupToggleAudioDevice()

            setupMonitorAudioToggleClicked()

            setupMicToggleClicked()

            remoteView.visibility = View.INVISIBLE
            monitorRecordImg.startAnimation(blinkAnimation)
            if(!isMonitor) {
                //Views for watcher
                monitorTitleTv.text = "Watching Device $monitorDevice"
                toggleMonitorCamera.setOnClickListener{
                    mainServiceRepository.sendSwitchMonitorCamera()
                }
                endMonitorButton.setOnClickListener{
                    mainServiceRepository.sendEndStreaming()
                    removeWatcher()
                }
                //Send watching request
                mainServiceRepository.sendStartWatching(monitorDevice!!)
            } else {
                //Views for monitor
                toggleMonitorCamera.setOnClickListener{
                    mainServiceRepository.switchCamera()
                }
                monitorTitleTv.text = "Monitoring on $monitorDevice"
                endMonitorButton.setOnClickListener{
                    mainServiceRepository.sendEndStreaming()
                    removeDevice()
                }
            }
        }

    }

    private fun setupMonitorAudioToggleClicked(){
        views.apply {
            if (isMonitor) {
                toggleMonitorMicrophoneButton.visibility = View.GONE
            } else {
                if(isMonitorMicrophoneMuted) {
                    toggleMonitorMicrophoneButton.setImageResource(R.drawable.ic_speaker_on)
                } else {
                    toggleMonitorMicrophoneButton.setImageResource(R.drawable.ic_speaker_off)
                }
                isMonitorMicrophoneMuted = !isMonitorMicrophoneMuted
                // Button Setup
                toggleMonitorMicrophoneButton.setOnClickListener {
                    if (isMonitorMicrophoneMuted){
                        mainServiceRepository.sendToggleMonitorMicrophoneOn()
                        toggleMonitorMicrophoneButton.setImageResource(R.drawable.ic_speaker_on)
                    }else{
                        mainServiceRepository.sendToggleMonitorMicrophoneOff()
                        toggleMonitorMicrophoneButton.setImageResource(R.drawable.ic_speaker_off)
                    }
                    isMonitorMicrophoneMuted = !isMonitorMicrophoneMuted
                    }
                }
            }
    }

    private fun setupMicToggleClicked(){
        views.apply {
            //Init
            if(isMicrophoneMuted) {
                mainServiceRepository.toggleAudio(true)
                toggleMicrophoneButton.setImageResource(R.drawable.ic_microphone_off)
            } else {
                mainServiceRepository.toggleAudio(false)
                toggleMicrophoneButton.setImageResource(R.drawable.ic_microphone_on)
            }
            isMicrophoneMuted = !isMicrophoneMuted
            // Button setup
            toggleMicrophoneButton.setOnClickListener {
                if (isMicrophoneMuted){
                    mainServiceRepository.toggleAudio(true)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_microphone_off)
                }else{
                    mainServiceRepository.toggleAudio(false)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_microphone_on)
                }
                isMicrophoneMuted = !isMicrophoneMuted
            }
        }
    }

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

    override fun onToggleMonitorAudioOnReceived() {
        views.apply {
            mainServiceRepository.toggleAudio(false)
            toggleMicrophoneButton.setImageResource(R.drawable.ic_microphone_on)
            isMicrophoneMuted = true
        }
    }

    override fun onToggleMonitorAudioOffReceived() {
        views.apply {
            mainServiceRepository.toggleAudio(true)
            toggleMicrophoneButton.setImageResource(R.drawable.ic_microphone_off)
            isMicrophoneMuted = false
        }
    }

    override fun onSwitchCameraReceived() {
        mainServiceRepository.switchCamera()
    }

    override fun onEndMonitoringReceived() {
        removeWatcher()
    }
    override fun onEndWatchingReceived() {
        remoteViewRenderer?.clearImage()
        mainRepository.resetTarget()
    }

    override fun closeMonitorReceived() {
        mainServiceRepository.sendEndStreaming()
        removeDevice()
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
                mainRepository.closeWebRTCConnection()
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
                mainRepository.closeWebRTCConnection()
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
        removeData()
    }
}