package com.gdsc.nitcbustracker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging

class DriverActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var driverTrackingFragment: DriverTrackingFragment
    private lateinit var noticeFragment: DriverNoticeFragment
    private lateinit var driverProfileFragment: DriverProfileFragment
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        bottomNavigationView = findViewById(R.id.bottom_navigation_driver)

        if (savedInstanceState == null) {
            driverTrackingFragment = DriverTrackingFragment()
            noticeFragment = DriverNoticeFragment()
            driverProfileFragment = DriverProfileFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_driver_fragment, driverTrackingFragment, "TRACKING")
                .add(R.id.nav_driver_fragment, noticeFragment, "NOTICE")
                .add(R.id.nav_driver_fragment, driverProfileFragment, "PROFILE")
                .hide(noticeFragment)
                .hide(driverProfileFragment)
                .commit()

            activeFragment = driverTrackingFragment
        } else {
            noticeFragment = supportFragmentManager.findFragmentByTag("NOTICE") as? DriverNoticeFragment ?: DriverNoticeFragment()
            driverProfileFragment = supportFragmentManager.findFragmentByTag("PROFILE") as? DriverProfileFragment ?: DriverProfileFragment()

            activeFragment = when (bottomNavigationView.selectedItemId) {
                R.id.nav_driver -> driverTrackingFragment
                R.id.nav_driver_notice -> noticeFragment
                R.id.nav_driver_profile -> driverProfileFragment
                else -> driverTrackingFragment
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragmentToShow = when (item.itemId) {
                R.id.nav_driver -> driverTrackingFragment
                R.id.nav_driver_notice -> noticeFragment
                R.id.nav_driver_profile -> driverProfileFragment
                else -> driverTrackingFragment
            }
            showFragment(fragmentToShow)
            true
        }

        FirebaseMessaging.getInstance().subscribeToTopic("notifications-driver")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to notifications-driver topic")
                }
            }
    }

    private fun showFragment(fragmentToShow: Fragment) {
        if (activeFragment == fragmentToShow) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(fragmentToShow)
            .commit()

        activeFragment = fragmentToShow
    }
}
