package com.example.jaby.fragments.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.jaby.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView


class SignUpFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)
        val backBtn: MaterialTextView = view.findViewById(R.id.back_btn)
        val continueBtn: MaterialButton = view.findViewById(R.id.sign_up_btn)
        val googleBtn: MaterialButton = view.findViewById(R.id.google_sign_up_btn)

        backBtn.setOnClickListener{
            val fragment = EntryFragment()
            requireActivity().supportFragmentManager.beginTransaction().apply{
                setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                replace(R.id.nav_container, fragment)
                commit()
            }
        }

        return view
    }
}