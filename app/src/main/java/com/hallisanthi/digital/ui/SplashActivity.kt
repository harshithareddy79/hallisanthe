package com.hallisanthi.digital.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.hallisanthi.digital.MainActivity
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scaleIn  = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        val fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)

        binding.appLogoImageView.startAnimation(scaleIn)
        binding.appNameTextView.startAnimation(fadeInUp)
        binding.appTaglineTextView.startAnimation(fadeInUp)

        Handler(Looper.getMainLooper()).postDelayed({
            val destination = when {
                OnboardingActivity.shouldShow(this) -> OnboardingActivity::class.java
                UserSession.isLoggedIn(this)        -> MainActivity::class.java
                else                                -> LoginActivity::class.java
            }
            startActivity(Intent(this, destination))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1800)
    }
}
