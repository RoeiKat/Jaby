package com.example.jaby.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.jaby.MainActivity
import com.example.jaby.databinding.ActivityLoginBinding
import com.example.jaby.R
import com.example.jaby.fragments.login.EntryFragment
import com.example.jaby.fragments.login.VerificationFragment
import com.example.jaby.repository.MainRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject



@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    @Inject lateinit var mAuth: FirebaseAuth
    @Inject lateinit var mainRepository: MainRepository

    override fun onStart() {
        super.onStart()
        if(mAuth.currentUser != null) {
            if(mAuth.currentUser!!.isEmailVerified) {
                moveToMainActivity()
            } else {
                moveToVerificationFragment()
            }
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
                if(mAuth.currentUser != null && !mAuth.currentUser!!.isEmailVerified){
                    Log.d("Verification", "User not verified")
                    moveToVerificationFragment()
                } else {
                    moveToMainActivity()
                }
            }
        }
    }
    fun signUp(email: String, password: String) {
        mainRepository.signUp(
            email,password){
                isDone, reason ->
            if(!isDone) {
                Toast.makeText(this, reason,Toast.LENGTH_SHORT).show()
            } else {
                if(mAuth.currentUser != null && !mAuth.currentUser!!.isEmailVerified){
                    Log.d("Verification", "User not verified")
                    moveToVerificationFragment()
                } else {
                    moveToMainActivity()
                }
            }
        }
    }

    fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Your web client ID from the Firebase console
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    fun moveToMainActivity() {
        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun sendResetPasswordEmail(email:String) {
        mainRepository.sendResetPasswordMail(email){
            isDone,reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sent password reset successfully", Toast.LENGTH_SHORT).show()
                moveToEntryFragment()
            }
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            val idToken = mainRepository.getGoogleIdTokenFromIntent(intent)
            idToken?.let {
                mainRepository.loginWithGoogleToken(it) {
                    isDone,reason ->
                        if(!isDone) {
                            Toast.makeText(this, reason,Toast.LENGTH_SHORT).show()
                        } else {
                            if(mAuth.currentUser != null && !mAuth.currentUser!!.isEmailVerified){
                                Log.d("Verification", "User not verified")
                                moveToVerificationFragment()
                            } else {
                                moveToMainActivity()
                            }
                        }
                }
            } ?: run {
                Toast.makeText(this, "Failed generating Google Token",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun moveToEntryFragment() {
        val fragment = EntryFragment()
        supportFragmentManager.beginTransaction().apply{
            setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left)
            replace(R.id.nav_container, fragment)
            commit()
        }
    }

    private fun moveToVerificationFragment() {
        val fragment = VerificationFragment()
        supportFragmentManager.beginTransaction().apply{
            setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
            replace(R.id.nav_container, fragment)
            commit()
        }
    }

}