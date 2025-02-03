package com.example.jaby.fragments.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.jaby.R
import com.google.android.material.button.MaterialButton


class EntryFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_entry, container, false)
        val signUpBtn: MaterialButton = view.findViewById(R.id.btn_sign_up)
        val signInBtn: MaterialButton = view.findViewById(R.id.btn_sign_in)

        signUpBtn.setOnClickListener{
            val fragment = SignUpFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }

        signInBtn.setOnClickListener{
            val fragment = SignInFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }



        return view
    }


}