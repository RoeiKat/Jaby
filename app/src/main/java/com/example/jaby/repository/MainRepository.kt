package com.example.jaby.repository

import android.bluetooth.BluetoothClass.Device
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

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
): WebRTCClient.Listener {

    private var target:String?=null
    var listener: Listener? = null
    private var remoteView:SurfaceViewRenderer?=null

    fun signOut() {
        firebaseClient.signOut()
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

    fun initFirebase(){
        Log.d("SubscribeForLatestEvent","Listening for events on ${target}")
        firebaseClient.subscribeForLatestEvent(target!!,object :FirebaseClient.Listener {
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
                    DataModelType.EndCall-> {
                        listener?.endCall()
                    }
                    else -> Unit
                }
            }
        })
    }

    fun sendConnectionRequest(target: String, isVideoCall:Boolean, success : (Boolean) -> Unit) {
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                type = if(isVideoCall) DataModelType.StartVideoCall else DataModelType.StartAudioCall,
                target = target
            ),success
        )
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
        fun endCall()
    }

    fun setTarget(target: String) {
        this.target = target
    }

    fun setCurrentUserId(userId:String) {
        firebaseClient.setCurrentUserId(userId)
    }

    fun initWebrtcClient(device: String) {
        webRTCClient.listener = this
        webRTCClient.initalizeWebrtcClient(device, object: MyPeerObserver() {
            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                //notify the creator of this class that there is a new stream available
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
                if(newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    //1.change my status to in call
                    changeDeviceStatus(DeviceStatus.IN_STREAM)
                    //2.clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                }
            }
        })
    }

    fun initLocalSurfaceView(view:SurfaceViewRenderer, isVideoCall: Boolean) {
        webRTCClient.initLocalSurfaceView(view,isVideoCall)
    }

    fun initRemoteSurfaceView(view:SurfaceViewRenderer) {
        webRTCClient.initRemoteSurfaceView(view)
        this.remoteView = view
    }

    fun startCall() {
        Log.d("StartCall", "Got to start call")
        webRTCClient.call(target!!)
    }

    fun endCall(){
        webRTCClient.closeConnection()
        firebaseClient.changeDeviceStatus(DeviceStatus.ONLINE)
    }

    fun sendEndCall() {
        onTransferEventToSocket(
            DataModel(
                type = DataModelType.EndCall,
                target = target!!
            )
        )
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

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data){}
    }

}