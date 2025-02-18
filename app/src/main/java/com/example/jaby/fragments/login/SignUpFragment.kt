package com.example.jaby.fragments.login

import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.jaby.R
import com.example.jaby.repository.MainRepository
import com.example.jaby.ui.login.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import javax.inject.Inject


class SignUpFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)
        val activity  = requireActivity() as LoginActivity

        val btnBack: ImageButton = view.findViewById(R.id.btn_back)
        val btnSignUp: Button = view.findViewById(R.id.btn_sign_up)
        val btnGoogleSignUp: MaterialButton = view.findViewById(R.id.btn_sign_up_google)

        val etEmail: EditText = view.findViewById(R.id.et_email)
        val etPassword: EditText = view.findViewById(R.id.et_password)

        val tvSignIn: TextView = view.findViewById(R.id.tv_sign_in)
        tvSignIn.paintFlags = tvSignIn.paintFlags or Paint.UNDERLINE_TEXT_FLAG


        btnBack.setOnClickListener{
            val fragment = EntryFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }
        btnSignUp.setOnClickListener{
            activity.signUp(etEmail.text.toString(),etPassword.text.toString())
        }
        btnGoogleSignUp.setOnClickListener{
            activity.signInWithGoogle()
        }
        tvSignIn.setOnClickListener {
            val fragment = SignInFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }

        return view
    }
}