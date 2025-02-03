package com.example.jaby.firebaseClient

import android.util.Log
import android.widget.Toast
import com.example.jaby.utils.DeviceStatus
import com.example.jaby.utils.FirebaseFieldNames
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
    private var currentUserName:String? = null

    private fun setUserName(username:String) {
        this.currentUserName = username
    }

    //        dbRef.addListenerForSingleValueEvent(object: ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.hasChild(username)) {
//                    //User exists, notify the user
//                    done(false, "User already exists")
//                } else {
//                    // User dosent exits, register the user
//                    dbRef.child(username).child(FirebaseFieldNames.PASSWORD).setValue(password).addOnCompleteListener{
//                        dbRef.child(username).child(FirebaseFieldNames.STATUS).setValue(DeviceStatus.ONLINE)
//                            .addOnCompleteListener{
//                                setUserName(username)
//                                done(true, null)
//                            }.addOnFailureListener{
//                                done(false, it.message)
//                            }
//                    }.addOnFailureListener{
//                        done(false,it.message)
//                    }
//                }
//            }
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//        })

    fun removeDevice(deviceName: String, done: (Boolean, String?) -> Unit) {
        dbRef.addValueEventListener(object: MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(mAuth.currentUser != null) {
                    if(snapshot.child(FirebaseFieldNames.USERS).child(mAuth.currentUser!!.uid).child(FirebaseFieldNames.DEVICES).hasChild(deviceName))
                    {
                        dbRef.child(FirebaseFieldNames.USERS).child(mAuth.currentUser!!.uid)
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
                    if(snapshot.child(FirebaseFieldNames.USERS).child(mAuth.currentUser!!.uid).child(FirebaseFieldNames.DEVICES).hasChild(deviceName))
                    {
                        done(false, "Device already exists")
                    } else {
                        dbRef.child(FirebaseFieldNames.USERS).child(mAuth.currentUser!!.uid)
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
                    val list = snapshot.child(FirebaseFieldNames.USERS).child(mAuth.currentUser!!.uid)
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
                val user = mAuth.currentUser
                done(true, null)
            } else {
                done(false, "Email / Password incorrect")
            }
        }.addOnFailureListener{
            done(false, it.message)
        }
//        dbRef.addListenerForSingleValueEvent(object: ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.hasChild(username)) {
//                    //User exists, check password
//                    val dbPassword = snapshot.child(username).child(FirebaseFieldNames.PASSWORD).value
//                    if(password == dbPassword) {
//                        //password is correct and sign in
//                        dbRef.child(username).child(FirebaseFieldNames.STATUS).setValue(DeviceStatus.ONLINE)
//                            .addOnCompleteListener{
//                                setUserName(username)
//                                done(true,null)
//                            }.addOnFailureListener{
//                                done(false,"${it.message}")
//                            }
//
//                    } else {
//                        //password is wrong notify user
//                        done(false, "Password is wrong")
//                    }
//                } else {
//                    done(false, "User / Password are incorrect")
//                }
//            }
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//        })
    }
//    fun observeDevicesStatus(status: (List<Pair<String,String>>) -> Unit) {
//
//
//    }
}