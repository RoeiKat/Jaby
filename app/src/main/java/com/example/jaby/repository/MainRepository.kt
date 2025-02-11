package com.example.jaby.repository

import android.bluetooth.BluetoothClass.Device
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
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

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

    private var remoteView:SurfaceViewRenderer?=null

    fun setIsMonitor(isMonitor: Boolean) {
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
                setTarget(event.sender!!)
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
                        resetTarget()
                        Log.d("RESET_TARGET", "target: $target")
                    }
                    else -> Unit
                }
            }
        })
    }

    fun sendConnectionRequest(target: String, success : (Boolean) -> Unit) {
        webRTCClient.call(target)
        success(true)
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
                Log.d("NEW_STATE",newState.toString())
                if(newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    Log.d("EVENT_CONNECTED","CONNCETED")
                    //1.change my status to in call
//                    changeDeviceStatus(DeviceStatus.IN_STREAM)
                    //2.clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                }
                if(newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    firebaseClient.clearLatestEvent()
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

    fun sendEndWatching(){
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                type = DataModelType.EndWatching,
                target = target!!
            )
        ){}
    }

    fun sendEndMonitoring(){
        if(target !== null) {
            onTransferEventToSocket(
                DataModel(
                    type = DataModelType.EndMonitoring,
                    target = target!!
                )
            )
        }
    }

    private fun resetTarget() {
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