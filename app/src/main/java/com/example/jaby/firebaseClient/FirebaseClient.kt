package com.example.jaby.firebaseClient

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract.Data
import android.util.Log
import android.widget.Toast
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.example.jaby.utils.DeviceStatus
import com.example.jaby.utils.FirebaseFieldNames
import com.example.jaby.utils.MyEventListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.typeOf

@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef:DatabaseReference,
    private val gson: Gson,
    private val mAuth: FirebaseAuth,
    ) {
    private var currentUserId:String? = null
    private var currentDevice:String? = null
    private var isMonitor:Boolean = false

    fun setIsMonitor(isMonitor:Boolean) {
        this.isMonitor = isMonitor
    }

    private fun setCurrentDevice(device: String?) {
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
                                if (snapshot.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                                        .child(FirebaseFieldNames.DEVICES).hasChild(device)
                                ) {
                                    devicesRef.child(device)
                                    .child(FirebaseFieldNames.WATCHERS)
                                    .setValue(newWatcherId)
                                }
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
        if (message.target == currentDevice) {
            success(false)
            return
        }
        val field = if (isMonitor) FirebaseFieldNames.WATCHERS else FirebaseFieldNames.DEVICES
        val targetRef = dbRef.child(FirebaseFieldNames.USERS)
            .child(currentUserId!!)
            .child(field)
            .child(message.target)
        targetRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val convertedMessage = gson.toJson(message.copy(sender = currentDevice))
                    targetRef.child(FirebaseFieldNames.LATEST_EVENT)
                        .setValue(convertedMessage)
                        .addOnCompleteListener {
                            success(true)
                        }
                        .addOnFailureListener {
                            success(false)
                        }
                } else {
                    success(false)
                }
            }})
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

    fun sendResetPasswordMail(email: String, done: (Boolean, String?) -> Unit) {
        if (email.isEmpty()) {
            done(false, "Email cannot be empty")
            return
        }
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    done(true, null)
                } else {
                    done(false, task.exception?.message ?: "Something went wrong")
                }
            }
    }


    fun sendJabyVerificationMail(done: (Boolean,String?) -> Unit) {
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

    fun signUp(email: String, password: String, done: (Boolean, String?) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            done(false, "Email and password cannot be empty")
            return
        }
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendJabyVerificationMail(done)
                } else {
                    val exception = task.exception
                    val errorMessage = if (exception != null && exception.message?.contains("Password") == true) {
                        "Password is too weak. Please choose a stronger password."
                    } else {
                        exception?.message ?: "Something went wrong"
                    }
                    done(false, errorMessage)
                }
            }
    }

    fun getGoogleIdTokenFromIntent(data: Intent?): String? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken
        } catch (e: ApiException) {
            Log.w("FirebaseClient", "Google sign in failed", e)
            null
        }
    }

    fun loginWithGoogleToken(googleIdToken: String, done: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = mAuth.currentUser?.uid
                    if (userId != null) {
                        setCurrentUserId(userId)
                        done(true, null)
                    } else {
                        done(false, "Failed retrieving user data")
                    }
                } else {
                    done(false, "Authentication with Google failed")
                }
            }
            .addOnFailureListener { done(false, it.message) }
    }

    fun login(email:String, password: String, done: (Boolean,String?) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            done(false, "Email and password cannot be empty")
            return
        }
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener{
                task ->
            if(task.isSuccessful) {
                val userId = mAuth.currentUser?.uid
                if(userId != null) {
                    setCurrentUserId(userId)
                    done(true, null)
                } else {
                    done(false, "The Email or Password you entered is incorrect. Please try again")
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