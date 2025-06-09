package com.gdsc.nitcbustracker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentActivity : AppCompatActivity() {

    private lateinit var mapFragment: MapFragment
    private lateinit var studentProfileFragment: StudentProfileFragment
    private lateinit var studentNoticeFragment: StudentNoticeFragment
    private lateinit var bottomNavigationView: BottomNavigationView
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppCrash", "Uncaught exception in thread ${thread.name}", throwable)
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation_student)

        if (savedInstanceState == null) {
            mapFragment = MapFragment()
            studentProfileFragment = StudentProfileFragment()
            studentNoticeFragment = StudentNoticeFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_student_fragment, studentProfileFragment, "PROFILE")
                .hide(studentProfileFragment)
                .add(R.id.nav_student_fragment, mapFragment, "MAP")
                .add(R.id.nav_student_fragment, studentNoticeFragment, "NOTICE")
                .hide(studentNoticeFragment)
                .commit()

            activeFragment = mapFragment
        } else {
            mapFragment = supportFragmentManager.findFragmentByTag("MAP") as? MapFragment ?: MapFragment()
            studentProfileFragment = supportFragmentManager.findFragmentByTag("PROFILE") as? StudentProfileFragment ?: StudentProfileFragment()
            studentNoticeFragment = supportFragmentManager.findFragmentByTag("NOTICE") as? StudentNoticeFragment ?: StudentNoticeFragment()

            activeFragment = when (bottomNavigationView.selectedItemId) {
                R.id.nav_student_profile -> studentProfileFragment
                R.id.nav_map -> mapFragment
                R.id.nav_student_notice -> studentNoticeFragment
                else -> mapFragment
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragmentToShow = when (item.itemId) {
                R.id.nav_map -> mapFragment
                R.id.nav_student_profile -> studentProfileFragment
                R.id.nav_student_notice -> studentNoticeFragment
                else -> mapFragment
            }
            showFragment(fragmentToShow)
            true
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
