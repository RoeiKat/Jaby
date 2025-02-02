package com.example.jaby.repository

import com.example.jaby.firebaseClient.FirebaseClient

import javax.inject.Inject

class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient
) {

    fun getMainRepository() : MainRepository {
        return this
    }

    fun login(username: String, password: String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.login(username,password,isDone)
    }
    fun signUp(username: String, password: String, isDone:(Boolean,String?) -> Unit) {
        firebaseClient.signUp(username,password,isDone)
    }

}