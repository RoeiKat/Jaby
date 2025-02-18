package com.example.jaby.fragments.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import com.example.jaby.R
import com.example.jaby.ui.login.LoginActivity


class ResetPasswordFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reset_password, container, false)
        val activity  = requireActivity() as LoginActivity

        val etEmail: EditText = view.findViewById(R.id.et_email)
        val btnBack: ImageButton = view.findViewById(R.id.btn_back)
        val btnResetPassword: Button = view.findViewById(R.id.btn_reset_password)

        btnBack.setOnClickListener{
            val fragment = SignInFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }

        btnResetPassword.setOnClickListener{
            val email = etEmail.text.toString()
            if(email.isNotEmpty()) {
                activity.sendResetPasswordEmail(email)
            }
        }



        return view
    }


}