package com.example.jaby.repository

import com.example.jaby.firebaseClient.FirebaseClient

import javax.inject.Inject

class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient
) {

    fun signOut() {
        firebaseClient.signOut()
    }

    fun login(username: String, password: String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.login(username,password,isDone)
    }
    fun signUp(username: String, password: String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.signUp(username,password,isDone)
    }

//    fun observeDevicesStatus(status: (List<Pair<String,String>>) -> Unit) {
//        firebaseClient.observeDevicesStatus(status)
//    }

}