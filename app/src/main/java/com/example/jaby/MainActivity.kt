package com.example.jaby

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jaby.adapters.MainRecyclerViewAdapter
import com.example.jaby.databinding.ActivityMainBinding
import com.example.jaby.repository.MainRepository
import com.example.jaby.ui.home.HomeFragment
import com.example.jaby.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var homeFragment: HomeFragment

    @Inject lateinit var mAuth: FirebaseAuth
    @Inject lateinit var mainRepository: MainRepository
//    private var mainAdapter: MainRecyclerViewAdapter? = null


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(mAuth.currentUser == null) finish()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
//            signOut()
            addDevice()
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        homeFragment = HomeFragment()

        init()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun init() {
        //1.Observe other users status
        subscribeObservers()
        //2.Start foreground service to listen for negotiations and calls
//        startMyService()
    }

    private fun subscribeObservers(){
        mainRepository.observeDevicesStatus{
            Log.d(TAG,"subscribeObservers: $it")
            homeFragment.updateDevices(it)
        }


    }

//    private fun startMyService() {
//    }

//    private fun setupRecyclerView() {
//        mainAdapter = MainRecyclerViewAdapter(this)
//        val layoutManager = LinearLayoutManager(this)
//        binding.mainRecyclerView.apply{
//            setLayoutManager(layoutManager)
//            adapter = mainAdapter
//        }
//    }

//    override fun onVideoCallClicked(deviceName: String) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onAudioCallClicked(deviceName: String) {
//        TODO("Not yet implemented")
//    }

    private fun addDevice() {
        mainRepository.addDevice(
            "TEST"){
                isDone, reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                //start moving to our call activity
            }
        }
    }

    private fun signOut() {
        mAuth.signOut()
        moveToLoginActivity()
    }

    private fun moveToLoginActivity() {
        intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}