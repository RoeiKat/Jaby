package com.example.jaby.firebaseClient

import android.provider.ContactsContract.Data
import android.util.Log
import android.widget.Toast
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.example.jaby.utils.DeviceStatus
import com.example.jaby.utils.FirebaseFieldNames
import com.example.jaby.utils.FirebaseFieldNames.LATEST_EVENT
import com.example.jaby.utils.MyEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef:DatabaseReference,
    private val gson: Gson,
    private val mAuth: FirebaseAuth
) {
    private var currentUserId:String? = null
    private var watcherId:String? = null
    private var currentDevice:String? = null

    fun setCurrentDevice(device: String) {
        this.currentDevice = device
    }

    private fun setCurrentWatcherId(watcherId: String) {
        this.watcherId = watcherId
    }

    fun getCurrentWatcherId():String{
        return this.watcherId!!
    }

    fun subscribeForUserLatestEvent(listener:Listener) {
        try{
            dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                .child(LATEST_EVENT).addValueEventListener(
                    object: MyEventListener() {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            super.onDataChange(snapshot)
                            val event = try {
                                gson.fromJson(snapshot.value.toString(),DataModel::class.java)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                            event?.let {
                                listener.onLatestEventReceived(it)
                            }
                        }
                    }
                )
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addWatcher(
        device: String,
        done: (Boolean, String?) -> Unit
    ) {
        val watchersRef = dbRef
            .child(FirebaseFieldNames.USERS)
            .child(currentUserId!!)
            .child(FirebaseFieldNames.WATCHERS)

        watchersRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                val newWatcherId = "watcher${count + 1}"
                setCurrentWatcherId(newWatcherId)
                val watcherData = mapOf(
                    "watcher" to newWatcherId,
                    "deviceName" to device,
                    "timeStamp" to System.currentTimeMillis()
                )
                watchersRef.child(newWatcherId)
                    .setValue(watcherData)
                    .addOnCompleteListener { done(true, null) }
                    .addOnFailureListener { done(false, it.message) }
            }
        })
    }

    fun subscribeForLatestEvent(target:String,listener:Listener) {
        try{
            dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                .child(FirebaseFieldNames.DEVICES).child(target)
                .child(LATEST_EVENT).addValueEventListener(
                    object: MyEventListener() {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            super.onDataChange(snapshot)
                            val event = try {
                                gson.fromJson(snapshot.value.toString(),DataModel::class.java)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                            event?.let {
                                listener.onLatestEventReceived(it)
                            }
                        }
                    }
                )
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun setCurrentUserId(userId:String) {
        this.currentUserId = userId
    }

    fun sendMessageToOtherClient(message:DataModel, success:(Boolean) -> Unit) {
        val convertedMessage = gson.toJson(message.copy(sender = currentUserId))
        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
            .child(FirebaseFieldNames.DEVICES).child(message.target)
            .child(FirebaseFieldNames.LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener{
                success(true)
            }.addOnFailureListener{
                success(false)
            }
    }

    interface Listener {
        fun onLatestEventReceived(event:DataModel)
    }

    fun removeDevice(deviceName: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object: MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(mAuth.currentUser != null) {
                    if(snapshot.child(FirebaseFieldNames.USERS).child(currentUserId!!).child(FirebaseFieldNames.DEVICES).hasChild(deviceName))
                    {
                        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                            .child(FirebaseFieldNames.DEVICES).child(deviceName)
                            .removeValue()
                            .addOnCompleteListener{
                                done(true,null)
                            }.addOnFailureListener{
                                done(false,it.message)
                            }

                    } else {
                        done(false, "Device doesn't exists")
                    }
                } else {
                    done(false, "Not Authenticated")
                }
            }
        })
    }

    fun addDevice(deviceName: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object: MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(mAuth.currentUser != null) {
                    if(snapshot.child(FirebaseFieldNames.USERS).child(currentUserId!!).child(FirebaseFieldNames.DEVICES).hasChild(deviceName))
                    {
                        done(false, "Device already exists")
                    } else {
                        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                            .child(FirebaseFieldNames.DEVICES).child(deviceName)
                            .child(FirebaseFieldNames.STATUS).setValue(DeviceStatus.ONLINE)
                            .addOnCompleteListener{
                                done(true,null)
                            }.addOnFailureListener{
                                done(false,it.message)
                            }
                    }
                } else {
                    done(false, "Not Authenticated")
                }
            }
        })
    }

    fun observeDevicesStatus(status: (List<Pair<String,String>>) -> Unit) {
        dbRef.addValueEventListener(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(mAuth.currentUser == null) {
                    return
                } else {
                    val list = snapshot.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                        .child(FirebaseFieldNames.DEVICES).children.map {
                            it.key!! to it.child(FirebaseFieldNames.STATUS).value.toString()
                        }
                    status(list)
                }
            }
        })
    }

    fun signOut() {
        mAuth.signOut()
    }

    fun signUp(username:String, password: String, done: (Boolean,String?) -> Unit) {
        mAuth.createUserWithEmailAndPassword(username,password).addOnCompleteListener{
            task ->
            if(task.isSuccessful) {
                val userUID = mAuth.currentUser?.uid.toString()
                dbRef.child("Users").child(userUID).setValue(true).addOnCompleteListener{
                    setCurrentUserId(userUID)
                    done(true,null)
                }.addOnFailureListener{
                    done(false, it.message)
                }
            } else {
                done(false, "Something went wrong")
            }
        }.addOnFailureListener{
            done(false, it.message)
        }

    }

    fun login(username:String, password: String, done: (Boolean,String?) -> Unit) {
        mAuth.signInWithEmailAndPassword(username,password).addOnCompleteListener{
                task ->
            if(task.isSuccessful) {
                val userId = mAuth.currentUser!!.uid
                setCurrentUserId(userId)
                done(true, null)
            } else {
                done(false, "Email / Password incorrect")
            }
        }.addOnFailureListener{
            done(false, it.message)
        }
    }

    fun changeDeviceStatus(status: DeviceStatus) {
        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
            .child(FirebaseFieldNames.DEVICES).child("TEST")
            .child(FirebaseFieldNames.STATUS).setValue(status)
    }

    fun clearLatestEvent() {
        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
            .child(FirebaseFieldNames.DEVICES).child(FirebaseFieldNames.LATEST_EVENT)
            .setValue(null)
    }
}