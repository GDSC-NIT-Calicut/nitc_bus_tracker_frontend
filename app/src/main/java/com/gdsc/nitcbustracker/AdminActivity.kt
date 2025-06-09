package com.gdsc.nitcbustracker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var adminNoticeFragment: AdminNoticeFragment
    private lateinit var adminFragment: AdminFragment
    private lateinit var adminProfileFragment: AdminProfileFragment
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        bottomNavigationView = findViewById(R.id.bottom_navigation_admin)

        if (savedInstanceState == null) {
            adminNoticeFragment = AdminNoticeFragment()
            adminFragment = AdminFragment()
            adminProfileFragment = AdminProfileFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_admin_fragment, adminNoticeFragment, "POST NOTICE")
                .add(R.id.nav_admin_fragment, adminFragment, "ADMIN")
                .add(R.id.nav_admin_fragment, adminProfileFragment, "PROFILE")
                .hide(adminNoticeFragment)
                .hide(adminProfileFragment)
                .commit()

            activeFragment = adminFragment
        } else {
            adminNoticeFragment = supportFragmentManager.findFragmentByTag("POST NOTICE") as? AdminNoticeFragment ?: AdminNoticeFragment()
            adminFragment = supportFragmentManager.findFragmentByTag("ADMIN") as? AdminFragment ?: AdminFragment()

            activeFragment = when (bottomNavigationView.selectedItemId) {
                R.id.nav_admin -> adminFragment
                R.id.nav_admin_notice -> adminNoticeFragment
                R.id.nav_admin_profile -> adminProfileFragment
                else -> adminFragment
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragmentToShow = when (item.itemId) {
                R.id.nav_admin -> adminFragment
                R.id.nav_admin_notice -> adminNoticeFragment
                R.id.nav_admin_profile -> adminProfileFragment
                else -> adminFragment
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

    fun hideBottomNav() {
        findViewById<View>(R.id.bottom_navigation_admin).visibility = View.GONE
    }

    fun showBottomNav() {
        findViewById<View>(R.id.bottom_navigation_admin).visibility = View.VISIBLE
    }
}
