package com.example.jaby.repository

import android.util.Log
import com.example.jaby.firebaseClient.FirebaseClient
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType

import javax.inject.Inject

class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient
) {

    var listener: Listener? = null

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
        firebaseClient.subscribeForLatestEvent(object :FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                listener?.onLatestEventReceived(event)
                when(event.type) {

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
    }

    fun setCurrentUserId(userId:String) {
        firebaseClient.setCurrentUserId(userId)
    }

}