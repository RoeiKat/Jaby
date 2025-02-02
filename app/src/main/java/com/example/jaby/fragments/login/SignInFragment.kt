package com.example.jaby.fragments.login

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.example.jaby.R
import com.example.jaby.repository.MainRepository
import com.example.jaby.ui.login.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import javax.inject.Inject


class SignInFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_sign_in, container, false)
        val activity = requireActivity() as LoginActivity

        val backBtn: MaterialTextView = view.findViewById(R.id.back_btn)
        val continueBtn: MaterialButton = view.findViewById(R.id.sign_in_btn)
        val googleBtn: MaterialButton = view.findViewById(R.id.google_sign_in_btn)
        val usernameET: EditText = view.findViewById(R.id.username_ET)
        val passwordET: EditText = view.findViewById(R.id.password_ET)


        backBtn.setOnClickListener{
            val fragment = EntryFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_left, R.anim.exit_to_right)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }

        continueBtn.setOnClickListener{
            activity.login(usernameET.text.toString(),passwordET.text.toString())
        }

        return view
    }


}