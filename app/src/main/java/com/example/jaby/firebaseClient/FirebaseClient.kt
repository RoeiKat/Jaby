package com.example.jaby.firebaseClient

import android.provider.ContactsContract.Data
import android.util.Log
import android.widget.Toast
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.example.jaby.utils.DeviceStatus
import com.example.jaby.utils.FirebaseFieldNames
import com.example.jaby.utils.MyEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
    private var currentDevice:String? = null
    private var isMonitor:Boolean = false

    fun setIsMonitor(isMonitor:Boolean) {
        this.isMonitor = isMonitor
    }

    fun setCurrentDevice(device: String?) {
        this.currentDevice = device
    }

    fun getCurrentDevice():String{
        return this.currentDevice!!
    }

    private fun resetFirebaseClient() {
        this.currentDevice = null
        this.isMonitor = false
    }

    fun removeWatcher(done: (Boolean,String?)-> Unit) {
        val watchersRef = dbRef
            .child(FirebaseFieldNames.USERS)
            .child(currentUserId!!)
            .child(FirebaseFieldNames.WATCHERS)
        watchersRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                watchersRef.child(currentDevice!!)
                    .removeValue()
                    .addOnCompleteListener{
                        done(true,null)
                    }
                    .addOnFailureListener{
                        done(false,it.message)
                    }
            }
        })
    }

    fun addWatcher(device: String, done: (Boolean, String?) -> Unit) {
        val watchersRef = dbRef
            .child(FirebaseFieldNames.USERS)
            .child(currentUserId!!)
            .child(FirebaseFieldNames.WATCHERS)

        val devicesRef = dbRef
            .child(FirebaseFieldNames.USERS)
            .child(currentUserId!!)
            .child(FirebaseFieldNames.DEVICES)

        watchersRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                val newWatcherId = "watcher${count + 1}"
                setCurrentDevice(newWatcherId)
                val watcherData = mapOf(
                    "watcher" to newWatcherId,
                    "deviceName" to device,
                    "timeStamp" to System.currentTimeMillis()
                )
                watchersRef.child(newWatcherId)
                    .setValue(watcherData)
                    .addOnCompleteListener {
                        devicesRef.addListenerForSingleValueEvent(object:MyEventListener() {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                super.onDataChange(snapshot)
                                devicesRef.child(device)
                                    .child(FirebaseFieldNames.WATCHERS)
                                    .setValue(newWatcherId)
                            }
                        })
                        watchersRef.child(newWatcherId).onDisconnect().removeValue()
                        done(true, null) }
                    .addOnFailureListener { done(false, it.message) }
            }
        })
    }

    fun subscribeForLatestEvent(listener:Listener) {
        val field = if(isMonitor) FirebaseFieldNames.DEVICES else FirebaseFieldNames.WATCHERS
        try{
            dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                .child(field).child(currentDevice!!)
                .child(FirebaseFieldNames.LATEST_EVENT).addValueEventListener(
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
        if(message.target == currentDevice) {
            success(false)
            return
        }
        val field = if(isMonitor) FirebaseFieldNames.WATCHERS else FirebaseFieldNames.DEVICES
        val convertedMessage = gson.toJson(message.copy(sender = currentDevice))
        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
            .child(field).child(message.target)
            .child(FirebaseFieldNames.LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener{
                success(true)
            }.addOnFailureListener{
                success(false)
            }
    }

    fun removeDevice(deviceName: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object: MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
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
            }
        })
    }

    fun addDevice(deviceName: String, done: (Boolean, String?) -> Unit) {
        try {
            dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                            .child(FirebaseFieldNames.DEVICES).hasChild(deviceName)
                    ) {
                        done(false, "Device already exists")
                    } else {
                        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                            .child(FirebaseFieldNames.DEVICES).child(deviceName)
                            .child(FirebaseFieldNames.STATUS).setValue(DeviceStatus.ONLINE)
                            .addOnCompleteListener {
                                setCurrentDevice(deviceName)
                                dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                                    .child(FirebaseFieldNames.DEVICES).child(deviceName)
                                    .onDisconnect().removeValue()
                                done(true, null)
                            }.addOnFailureListener {
                                done(false, it.message)
                            }
                    }
                }
            })
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observeDevicesStatus(status: (List<Pair<String,String>>) -> Unit) {
        dbRef.addValueEventListener(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                        .child(FirebaseFieldNames.DEVICES).children.map {
                            it.key!! to it.child(FirebaseFieldNames.STATUS).value.toString()
                        }
                    status(list)
            }
        })
    }

    fun signOut() {
        mAuth.signOut()
    }

    fun checkUser(done:(Boolean, String?) -> Unit) {
        val user = mAuth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val refreshedUser = mAuth.currentUser
                    if (refreshedUser == null) {
                        done(false, "User has been deleted.")
                    } else {
                        done(true,null)
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthException && exception.errorCode == "ERROR_USER_NOT_FOUND") {
                        done(false, "User not found (deleted).")
                    } else {
                        done(false, "Reload failed")
                    }
                }
            }
        } else {
            done(false, "No user is signed in.")
        }
    }

    private fun sendJabyVerificationMail(done: (Boolean,String?) -> Unit) {
        if(mAuth.currentUser != null) {
            mAuth.currentUser!!.sendEmailVerification().addOnCompleteListener{
                task ->
                if(task.isSuccessful) {
                    val userId = mAuth.currentUser?.uid
                    if(userId != null) {
                        setCurrentUserId(userId)
                        done(true,null)
                    } else {
                        done(false,"Failed retrieving user data ")
                    }
                } else {
                    done(false,"Something went wrong")
                }
            }.addOnFailureListener{
                done(false,it.message)
            }
        }
    }

    fun signUp(email:String, password: String, done: (Boolean,String?) -> Unit) {
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{
            task ->
            if(task.isSuccessful) {
                sendJabyVerificationMail(done)
            } else {
                done(false, "Something went wrong")
            }
        }.addOnFailureListener{
            done(false, it.message)
        }

    }

    fun login(email:String, password: String, done: (Boolean,String?) -> Unit) {
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener{
                task ->
            if(task.isSuccessful) {
                val userId = mAuth.currentUser?.uid
                if(userId != null) {
                    setCurrentUserId(userId)
                    done(true, null)
                } else {
                    done(false, "Failed retrieving user data")
                }
            } else {
                done(false, "The Email or Password you entered is incorrect. Please try again.")
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
        val field = if(isMonitor) FirebaseFieldNames.DEVICES else FirebaseFieldNames.WATCHERS
        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
            .child(field).child(currentDevice!!)
            .child(FirebaseFieldNames.LATEST_EVENT)
            .setValue(null)
    }



    interface Listener {
        fun onLatestEventReceived(event:DataModel)
    }
}