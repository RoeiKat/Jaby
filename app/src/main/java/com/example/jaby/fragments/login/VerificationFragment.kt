package com.example.jaby.fragments.login

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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

    private lateinit var tvTimer: TextView
    private lateinit var tvResend: TextView

    private var canSendAgain = true
    private val secondsTillCanResendAgain = 180L

    private val secondsTillNextVerificationCheck = 3L
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
                        handler.postDelayed(this, secondsTillNextVerificationCheck * 1000)
                    }
                } else {
                    Log.e("VerificationFragment", "Error reloading user", task.exception)
                    handler.postDelayed(this, secondsTillNextVerificationCheck * 1000)
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
            handler.postDelayed(verificationCheckRunnable, secondsTillNextVerificationCheck * 1000)
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

        val view = inflater.inflate(R.layout.fragment_verification, container, false)
        tvTimer = view.findViewById(R.id.tv_timer)
        tvResend = view.findViewById(R.id.tv_resend)
        tvResend.paintFlags = tvResend.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        startCountdownTimer()
        tvResend.setOnClickListener{
            if(canSendAgain) {
                sendVerificationEmail()
                startCountdownTimer()
            }
        }

        return view
    }

    private fun sendVerificationEmail() {
        mainRepository.sendVerificationEmail(){
            isDone,_ ->
            if(!isDone) {
                Toast.makeText(activity, "Failed to send verification E-mail", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Verification email sent", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCountdownTimer() {
        tvResend.setTextColor(ContextCompat.getColor(requireContext(),R.color.color_hint))
        canSendAgain = false
        object : CountDownTimer(secondsTillCanResendAgain * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                tvTimer.text = formatTime(secondsRemaining)
            }
            override fun onFinish() {
                canSendAgain = true
                tvResend.setTextColor(ContextCompat.getColor(requireContext(),R.color.color_primary))
                tvTimer.text = ""
            }
        }.start()
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
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