package com.example.jaby.repository

import android.bluetooth.BluetoothClass.Device
import android.content.Intent
import android.provider.ContactsContract.Data
import android.util.Log
import com.example.jaby.firebaseClient.FirebaseClient
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.example.jaby.utils.DeviceStatus
import com.example.jaby.webrtc.MyPeerObserver
import com.example.jaby.webrtc.WebRTCClient
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
): WebRTCClient.Listener {

    var listener: Listener? = null

    private var userId:String? = null
    private var target:String? = null

    private var isMonitor = false
    private var remoteView:SurfaceViewRenderer?=null

    fun setIsMonitor(isMonitor: Boolean) {
        this.isMonitor = isMonitor
        firebaseClient.setIsMonitor(isMonitor)
    }

    fun getCurrentDevice():String {
        return firebaseClient.getCurrentDevice()
    }


    fun signOut() {
        firebaseClient.signOut()
    }

    fun checkUser(isDone:(Boolean,String?) -> Unit){
        firebaseClient.checkUser(isDone)
    }

    fun login(username: String, password: String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.login(username,password,isDone)
    }
    fun signUp(username: String, password: String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.signUp(username,password,isDone)
    }

    fun getGoogleIdTokenFromIntent(data: Intent?): String? {
        return firebaseClient.getGoogleIdTokenFromIntent(data)
    }

    fun loginWithGoogleToken(googleIdToken: String, isDone: (Boolean, String?) -> Unit) {
        Log.d("GOOGLE_TAG", googleIdToken)
        firebaseClient.loginWithGoogleToken(googleIdToken,isDone)
    }

    fun sendVerificationEmail(isDone:(Boolean,String?) -> Unit) {
        firebaseClient.sendJabyVerificationMail(isDone)
    }

    fun sendResetPasswordMail(email: String,isDone:(Boolean,String?) -> Unit){
        firebaseClient.sendResetPasswordMail(email,isDone)
    }

    fun addDevice(deviceName: String, isDone:(Boolean,String?) -> Unit){
        firebaseClient.addDevice(deviceName, isDone)
    }

    fun removeDevice(deviceName: String, isDone:(Boolean, String?) -> Unit) {
        firebaseClient.removeDevice(deviceName, isDone)
    }

    fun observeDevicesStatus(status: (List<Pair<String,String>>) -> Unit) {
        firebaseClient.observeDevicesStatus(status)
    }

    fun addWatcher(deviceName:String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.addWatcher(deviceName,isDone)
    }

    fun removeWatcher(isDone:(Boolean,String?)->Unit){
        firebaseClient.removeWatcher(isDone)
    }

    fun initFirebase(){
        firebaseClient.subscribeForLatestEvent(object :FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                listener?.onLatestEventReceived(event)
                when(event.type) {
                    DataModelType.Offer-> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data.toString()
                            )
                        )
                        webRTCClient.answer(target!!)
                    }
                    DataModelType.Answer-> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data.toString()
                            )
                        )
                    }
                    DataModelType.IceCandidates-> {
                        val candidate: IceCandidate? = try {
                            gson.fromJson(event.data.toString(),IceCandidate::class.java)
                        }catch (e:Exception){
                            null
                        }
                        candidate?.let {
                            webRTCClient.addIceCandidateToPeer(it)
                        }
                    }
                    DataModelType.EndWatching -> {
                        firebaseClient.clearLatestEvent()
                    }
                    else -> Unit
                }
            }
        })
    }

    fun sendConnectionRequest() {
        webRTCClient.call(this.target!!)
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data){}
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
    }

    fun setTarget(target:String){
        this.target = target
    }


    fun setCurrentUserId(userId:String) {
        this.userId = userId
        firebaseClient.setCurrentUserId(userId)
    }

    fun initWebrtcClient(device: String) {
        webRTCClient.listener = this
        webRTCClient.initializeWebrtcClient(device, object: MyPeerObserver() {
            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                try{
                    Log.d("GotToOnAddStream", "${p0?.videoTracks}")
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let{
                    webRTCClient.sendIceCandidate(target!!,it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                Log.d("WebRTC_CONNECTION_STATE",newState.toString())
                if(newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    //1.change my status to in call
                    // changeDeviceStatus(DeviceStatus.IN_STREAM)
                    //2.clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                } else if(newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    if(!isMonitor) {
                        sendEndMonitoringToSelf()
                    }
                }
            }
        })
    }

    fun initLocalSurfaceView(view:SurfaceViewRenderer, isMonitor: Boolean) {
        webRTCClient.initLocalSurfaceView(view,isMonitor)
    }

    fun initRemoteSurfaceView(view:SurfaceViewRenderer) {
        webRTCClient.initRemoteSurfaceView(view)
        this.remoteView = view
    }

    fun closeWebRTCConnection(){
        webRTCClient.closeConnection()
    }

    fun sendToggleAudioOn(){
        if(!target.isNullOrEmpty()) {
            onTransferEventToSocket(
                DataModel(
                    type = DataModelType.ToggleMonitorAudioOn,
                    target = target!!
                )
            )
        }
    }

    fun sendToggleAudioOff(){
        if(!target.isNullOrEmpty()) {
            onTransferEventToSocket(
                DataModel(
                    type = DataModelType.ToggleMonitorAudioOff,
                    target = target!!
                )
            )
        }
    }

    fun sendSwitchMonitorCamera(){
        if(!target.isNullOrEmpty()) {
            onTransferEventToSocket(
                DataModel(
                    type = DataModelType.SwitchMonitorCamera,
                    target = target!!
                )
            )
        }
    }

    fun sendEndWatching(){
        if(!target.isNullOrEmpty()){
            firebaseClient.sendMessageToOtherClient(
                DataModel(
                    type = DataModelType.EndWatching,
                    target = target!!
                )
            ){}
        }
    }

    fun sendStartWatching(){
        if(!target.isNullOrEmpty()){
            firebaseClient.sendMessageToOtherClient(
                DataModel(
                    type = DataModelType.StartWatching,
                    target = target!!
                )
            ){}
        }
    }

    fun sendCloseMonitor(deviceName: String){
        if(deviceName.isNotEmpty()){
            firebaseClient.sendMessageToOtherClient(
                DataModel(
                    type = DataModelType.CloseMonitor,
                    target = deviceName
                )
            ){}
        }
    }

    fun sendEndMonitoring(){
        if(!target.isNullOrEmpty()) {
            onTransferEventToSocket(
                DataModel(
                    type = DataModelType.EndMonitoring,
                    target = target!!
                )
            )
        }
    }

    private fun sendEndMonitoringToSelf(){
        firebaseClient.sendEndMonitoringToSelf(){}
    }

    fun resetTarget() {
        this.target = null
    }

    private fun changeDeviceStatus(status: DeviceStatus) {
        firebaseClient.changeDeviceStatus(status)
    }

    fun toggleAudio(shouldBeMuted: Boolean){
        webRTCClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

}