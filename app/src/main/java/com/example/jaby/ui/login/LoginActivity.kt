package com.example.jaby.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.example.jaby.MainActivity
import com.example.jaby.databinding.ActivityLoginBinding

import com.example.jaby.R
import com.example.jaby.fragments.login.EntryFragment
import com.example.jaby.repository.MainRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    @Inject lateinit var mAuth: FirebaseAuth
    @Inject lateinit var mainRepository: MainRepository

    override fun onStart() {
        super.onStart()
        if(mAuth.currentUser != null) {
            moveToMainActivity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager.beginTransaction().replace(R.id.nav_container, EntryFragment()).commit()
    }

    fun login(username: String, password: String) {
        mainRepository.login(
            username,password){
                isDone, reason ->
            if(!isDone) {
                Toast.makeText(this, reason,Toast.LENGTH_SHORT).show()
            } else {
                //start moving to our main activity
                moveToMainActivity()
            }
        }
    }
    fun signUp(username: String, password: String) {
        mainRepository.signUp(
            username,password){
                isDone, reason ->
            if(!isDone) {
                Toast.makeText(this, reason,Toast.LENGTH_SHORT).show()
            } else {
                //start moving to our main activity
                moveToMainActivity()
            }
        }
    }

    private fun moveToMainActivity() {
        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}