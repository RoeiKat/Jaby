package com.example.jaby

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jaby.adapters.MainRecyclerViewAdapter
import com.example.jaby.databinding.ActivityMainBinding
import com.example.jaby.repository.MainRepository
import com.example.jaby.service.MainService
import com.example.jaby.service.MainServiceRepository
import com.example.jaby.ui.home.HomeFragment
import com.example.jaby.ui.login.LoginActivity
import com.example.jaby.utils.DataModel
import com.example.jaby.utils.DataModelType
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val PERMISSIONS_REQUEST_CODE = 1001

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener {

    private var homeFragment: HomeFragment? = null

    @Inject lateinit var mAuth: FirebaseAuth
    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var mainServiceRepository: MainServiceRepository


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var views: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(mAuth.currentUser == null) signOut()

        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        setSupportActionBar(views.appBarMain.toolbar)

        views.appBarMain.startMonitoringFAB.setOnClickListener { _ ->
            // Create an AlertDialog Builder
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter Device Name")

            // Create an EditText for user input
            val input = EditText(this)
            input.hint = "Device Name"
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            // Set up the positive ("OK") button action
            builder.setPositiveButton("OK") { dialog, _ ->
                val deviceName = input.text.toString().trim()
                if (deviceName.isNotEmpty()) {
                    addDevice(deviceName)
                } else {
                    // Optionally, show a message if the input is empty
                    Toast.makeText(this, "Please enter a device name.", Toast.LENGTH_SHORT).show()
                }
            }

            // Set up the negative ("Cancel") button action
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            // Show the dialog
            builder.show()
        }
//            onVideoCallClicked("TESTGIT")
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null)
//                .setAnchorView(R.id.startMonitoringFAB).show()
        val drawerLayout: DrawerLayout = views.drawerLayout
        val navView: NavigationView = views.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        homeFragment = getHomeFragment()


        // Microphone and Camera permissions code
        val permissionsNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+ include POST_NOTIFICATIONS as well as Camera and Microphone.
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // On older versions, just request Camera and Microphone.
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        }

        // Check if any permission is not granted.
        val permissionsToRequest = permissionsNeeded.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Request the missing permissions.
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            // All permissions already granted; proceed with initialization.
            init()
        }
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
        //2.Set username
        setUserId()
        //3.Start waiting for calls
//        MainService.listener = this
        //4.Start foreground service to listen for negotiations and calls
//        startMyService()
    }

    override fun onStartWatchClicked(deviceName: String) {
        //Not monitor, user wanna watch the monitor stream
        mainRepository.sendConnectionRequest(deviceName, false) {
            if(it) {
                    //we wanna create an intent to move to monitor activity
                    startActivity(Intent(this,MonitorActivity::class.java).apply {
                    putExtra("userId",mAuth.currentUser!!.uid)
                    putExtra("device",deviceName)
                    putExtra("isMonitor", false)
                })
            }
        }

    }

    private fun getHomeFragment(): HomeFragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.fragments?.firstOrNull { it is HomeFragment } as? HomeFragment
    }

    private fun subscribeObservers(){
        mainRepository.observeDevicesStatus{
            Log.d(TAG, "subscribeObservers: $it")
            homeFragment?.updateDevices(it)
        }
    }

    private fun setUserId(){
        mainRepository.setCurrentUserId(mAuth.currentUser!!.uid)
    }


    private fun addDevice(deviceName: String){
        mainRepository.addDevice(
            deviceName){
                isDone, reason ->
            if(!isDone) {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            } else {
                //start moving to our monitor activity
                startActivity(Intent(this, MonitorActivity::class.java).apply {
                    putExtra("userId", mAuth.currentUser!!.uid)
                    putExtra("device", deviceName)
                    putExtra("isMonitor", true)
                })
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // Check if all requested permissions are granted.
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                init()
            } else {
                Toast.makeText(
                    this,
                    "Camera, microphone, and notification permissions are required for this feature.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}