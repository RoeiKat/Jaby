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

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
): WebRTCClient.Listener {

    private var device:String?=null
    var listener: Listener? = null
    private var remoteView:SurfaceViewRenderer?=null
    private var userId:String? = null

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

    fun addWatcher(deviceName:String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.addWatcher(deviceName,isDone)
    }

    fun subscribeForUserLatestEvent(){
        firebaseClient.subscribeForUserLatestEvent(object :FirebaseClient.Listener {
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
                        webRTCClient.answer(device!!)
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


    fun initFirebase(){
        firebaseClient.subscribeForLatestEvent(device!!,object :FirebaseClient.Listener {
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
                        webRTCClient.answer(device!!)
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

    fun sendConnectionRequest(deviceName: String,isMonitor: Boolean, success : (Boolean) -> Unit) {
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                type = if(isMonitor) DataModelType.StartMonitoring else DataModelType.StartWatching,
                target = deviceName
            ),success
        )
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data){}
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
        fun endCall()
    }

    fun setDevice(device: String) {
        this.device = device
    }

    fun setFirebaseCurrentDevice(device:String) {
        firebaseClient.setCurrentDevice(device)
    }

    fun setCurrentUserId(userId:String) {
        this.userId = userId
        firebaseClient.setCurrentUserId(userId)
    }

    fun getCurrentWatcherId(): String {
        return firebaseClient.getCurrentWatcherId()
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
                    webRTCClient.sendIceCandidate(device,it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                if(newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    //1.change my status to in call
//                    changeDeviceStatus(DeviceStatus.IN_STREAM)
                    //2.clear latest event inside my user section in firebase database
//                    firebaseClient.clearLatestEvent()
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

    fun startCall(deviceName: String) {
        webRTCClient.call(deviceName)
    }

    fun endCall(){
        webRTCClient.closeConnection()
        firebaseClient.changeDeviceStatus(DeviceStatus.ONLINE)
    }

    fun sendEndCall() {
        onTransferEventToSocket(
            DataModel(
                type = DataModelType.EndCall,
                target = device!!
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



}