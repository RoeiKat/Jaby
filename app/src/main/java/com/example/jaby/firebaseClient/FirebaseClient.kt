package com.example.jaby.firebaseClient

import android.util.Log
import android.widget.Toast
import com.example.jaby.utils.DeviceStatus
import com.example.jaby.utils.FirebaseFieldNames
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import javax.inject.Inject

class FirebaseClient @Inject constructor(
    private val dbRef:DatabaseReference,
    private val gson: Gson,
    private val mAuth: FirebaseAuth
) {
    private var currentUserName:String? = null

    private fun setUserName(username:String) {
        this.currentUserName = username
    }

    fun signUp(username:String, password: String, done: (Boolean,String?) -> Unit) {
        mAuth.createUserWithEmailAndPassword(username,password).addOnCompleteListener{
            task ->
            if(task.isSuccessful) {
                Log.d("Auth", "Created user successfully")
                val user = mAuth.currentUser?.uid
                Log.d("UID", user.toString())
            } else {
                Log.d("Auth", "Created user unsuccessfully")

            }
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
    }

    fun login(username:String, password: String, done: (Boolean,String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(username)) {
                    //User exists, check password
                    val dbPassword = snapshot.child(username).child(FirebaseFieldNames.PASSWORD).value
                    if(password == dbPassword) {
                        //password is correct and sign in
                        dbRef.child(username).child(FirebaseFieldNames.STATUS).setValue(DeviceStatus.ONLINE)
                            .addOnCompleteListener{
                                setUserName(username)
                                done(true,null)
                            }.addOnFailureListener{
                                done(false,"${it.message}")
                            }

                    } else {
                        //password is wrong notify user
                        done(false, "Password is wrong")
                    }
                } else {
                    done(false, "User / Password are incorrect")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}