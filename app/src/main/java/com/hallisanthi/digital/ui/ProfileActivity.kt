package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.User
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"

        val nameView  = findViewById<TextView>(R.id.profileName)
        val emailView = findViewById<TextView>(R.id.profileEmail)
        val phoneView = findViewById<TextView>(R.id.profilePhone)
        val roleView  = findViewById<TextView>(R.id.profileRole)

        refreshUI(nameView, emailView, phoneView, roleView)

        // Account section
        findViewById<LinearLayout>(R.id.editProfileButton).setOnClickListener {
            showEditDialog(nameView, emailView, phoneView, roleView)
        }
        findViewById<LinearLayout>(R.id.changePasswordButton).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.myChatsButton).setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        // Activity section
        findViewById<LinearLayout?>(R.id.recentlyViewedButton)?.setOnClickListener {
            startActivity(Intent(this, RecentlyViewedActivity::class.java))
        }
        findViewById<LinearLayout?>(R.id.myOrdersButton)?.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }
        if (UserSession.isArtisan(this)) {
            findViewById<LinearLayout?>(R.id.salesDashButton)?.apply {
                visibility = android.view.View.VISIBLE
                setOnClickListener { startActivity(Intent(this@ProfileActivity, SalesDashboardActivity::class.java)) }
            }
        }

        // Preferences section
        findViewById<LinearLayout?>(R.id.notificationsButton)?.setOnClickListener {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }
        findViewById<LinearLayout?>(R.id.settingsButton)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Logout
        findViewById<LinearLayout>(R.id.logoutButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    UserSession.logout(this)
                    startActivity(Intent(this, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun refreshUI(nameView: TextView, emailView: TextView, phoneView: TextView, roleView: TextView) {
        nameView.text  = UserSession.getName(this)
        emailView.text = UserSession.getEmail(this)
        phoneView.text = UserSession.getPhone(this)
        roleView.text  = if (UserSession.getRole(this) == User.ROLE_ARTISAN)
                            "🧑‍🎨 Artisan / Seller" else "🛒 Buyer"
    }

    private fun showEditDialog(nameView: TextView, emailView: TextView,
                               phoneView: TextView, roleView: TextView) {
        val view     = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.editNameEditText)
        nameEdit.setText(UserSession.getName(this))

        AlertDialog.Builder(this)
            .setTitle("Edit Name")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameEdit.text.toString().trim()
                if (newName.length < 2) {
                    Toast.makeText(this, "Name too short", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val userId = UserSession.getUserId(this)
                lifecycleScope.launch {
                    val db   = ProductDatabase.getDatabase(applicationContext)
                    val user = db.userDao().getById(userId)
                    if (user != null) {
                        db.userDao().update(user.copy(name = newName))
                        UserSession.updateCachedProfile(this@ProfileActivity, newName, user.phone, user.email)
                        runOnUiThread {
                            refreshUI(nameView, emailView, phoneView, roleView)
                            Toast.makeText(this@ProfileActivity, "Profile updated ✅", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
