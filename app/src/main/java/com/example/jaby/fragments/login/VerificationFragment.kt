package com.example.jaby.fragments.login

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.jaby.R
import com.example.jaby.repository.MainRepository
import com.example.jaby.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VerificationFragment : Fragment() {

    @Inject lateinit var mAuth: FirebaseAuth
    @Inject lateinit var mainRepository: MainRepository

    private lateinit var activity: LoginActivity

    private val seconds = 3L
    private val handler = Handler(Looper.getMainLooper())


    private val verificationCheckRunnable = object: Runnable {
        override fun run() {
            Log.d("VerificationCheckRunnable", "Checking User Verification")
            mAuth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (mAuth.currentUser?.isEmailVerified == true) {
                        activity.moveToMainActivity()
                        handler.removeCallbacks(this)
                    } else {
                        handler.postDelayed(this, seconds * 1000)
                    }
                } else {
                    Log.e("VerificationFragment", "Error reloading user", task.exception)
                    handler.postDelayed(this, seconds * 1000)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity = requireActivity() as LoginActivity

        mainRepository.checkUser(){
                isDone,reason ->
            if(!isDone) {
                Toast.makeText(activity, reason, Toast.LENGTH_SHORT).show()
                moveToEntryFragment()
            }
        }

        if(mAuth.currentUser!!.isEmailVerified) {
            activity.moveToMainActivity()
        } else {
            handler.postDelayed(verificationCheckRunnable, seconds * 1000)
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(verificationCheckRunnable)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val views = inflater.inflate(R.layout.fragment_verification, container, false)



        return views
    }

    private fun moveToEntryFragment(){
        val fragment = EntryFragment()
        activity.supportFragmentManager.beginTransaction().apply{
            setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left)
            replace(R.id.nav_container, fragment)
            commit()
        }
    }


}