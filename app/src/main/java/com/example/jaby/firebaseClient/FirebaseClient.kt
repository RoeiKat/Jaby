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

    fun setCurrentUserId(userId:String) {
        this.currentUserId = userId
    }


    fun subscribeForLatestEvent(listener:Listener) {
        try{
            dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
                .child(FirebaseFieldNames.DEVICES).child("TEST")
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

    fun sendMessageToOtherClient(message:DataModel, success:(Boolean) -> Unit) {
        val convertedMessage = gson.toJson(message.copy(sender = currentUserId))
        dbRef.child(FirebaseFieldNames.USERS).child(currentUserId!!)
            .child(FirebaseFieldNames.DEVICES).child(message.target)
            .child(LATEST_EVENT).setValue(convertedMessage)
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
        dbRef.addValueEventListener(object: MyEventListener() {
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
        dbRef.addValueEventListener(object: MyEventListener() {
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
                done(false, null)
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
//    fun observeDevicesStatus(status: (List<Pair<String,String>>) -> Unit) {
//
//
//    }
}