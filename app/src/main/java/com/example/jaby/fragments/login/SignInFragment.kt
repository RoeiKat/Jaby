package com.example.jaby.fragments.login

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.jaby.R
import com.example.jaby.ui.login.LoginActivity


class SignInFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_sign_in, container, false)
        val activity = requireActivity() as LoginActivity

        val btnBack: ImageButton = view.findViewById(R.id.btn_back)
        val btnSignIn: Button = view.findViewById(R.id.btn_sign_in)
        val btnGoogleSignIn: Button = view.findViewById(R.id.btn_sign_in_google)

        val etEmail: EditText = view.findViewById(R.id.et_email)
        val etPassword: EditText = view.findViewById(R.id.et_password)

        val tvSignUp: TextView = view.findViewById(R.id.tv_sign_up)
        tvSignUp.paintFlags = tvSignUp.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        val tvForgotPassword: TextView = view.findViewById(R.id.tv_forgot_password)


        btnBack.setOnClickListener{
            val fragment = EntryFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_left, R.anim.exit_to_right)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }

        tvSignUp.setOnClickListener{
            val fragment = SignUpFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }



        btnSignIn.setOnClickListener{
            activity.login(etEmail.text.toString(),etPassword.text.toString())
        }

        return view
    }

}