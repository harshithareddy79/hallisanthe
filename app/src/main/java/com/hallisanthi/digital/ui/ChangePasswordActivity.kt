package com.hallisanthi.digital.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.PasswordUtils
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Change Password"

        val currentLayout  = findViewById<TextInputLayout>(R.id.cpCurrentLayout)
        val currentEdit    = findViewById<TextInputEditText>(R.id.cpCurrentEdit)
        val newLayout      = findViewById<TextInputLayout>(R.id.cpNewLayout)
        val newEdit        = findViewById<TextInputEditText>(R.id.cpNewEdit)
        val confirmLayout  = findViewById<TextInputLayout>(R.id.cpConfirmLayout)
        val confirmEdit    = findViewById<TextInputEditText>(R.id.cpConfirmEdit)
        val strengthBar    = findViewById<ProgressBar>(R.id.cpStrengthBar)
        val strengthLabel  = findViewById<TextView>(R.id.cpStrengthLabel)
        val saveBtn        = findViewById<MaterialButton>(R.id.cpSaveBtn)

        newEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val pw = s.toString()
                when (PasswordUtils.isStrong(pw)) {
                    PasswordUtils.PasswordStrength.WEAK   -> { strengthBar.progress = 33; strengthLabel.text = "Weak"; strengthLabel.setTextColor(getColor(R.color.error_red)) }
                    PasswordUtils.PasswordStrength.MEDIUM -> { strengthBar.progress = 66; strengthLabel.text = "Medium"; strengthLabel.setTextColor(getColor(R.color.warning_yellow)) }
                    PasswordUtils.PasswordStrength.STRONG -> { strengthBar.progress = 100; strengthLabel.text = "Strong 💪"; strengthLabel.setTextColor(getColor(R.color.success_green)) }
                }
            }
        })

        saveBtn.setOnClickListener {
            val current = currentEdit.text.toString()
            val newPass  = newEdit.text.toString()
            val confirm  = confirmEdit.text.toString()
            var valid = true
            if (current.isBlank()) { currentLayout.error = "Enter current password"; valid = false } else currentLayout.error = null
            if (PasswordUtils.isStrong(newPass) == PasswordUtils.PasswordStrength.WEAK) { newLayout.error = "New password too weak"; valid = false } else newLayout.error = null
            if (newPass != confirm) { confirmLayout.error = "Passwords do not match"; valid = false } else confirmLayout.error = null
            if (!valid) return@setOnClickListener

            saveBtn.isEnabled = false
            lifecycleScope.launch {
                val db   = ProductDatabase.getDatabase(applicationContext)
                val user = db.userDao().getById(UserSession.getUserId(this@ChangePasswordActivity))
                runOnUiThread {
                    if (user == null || !PasswordUtils.verify(current, user.passwordHash)) {
                        currentLayout.error = "Current password is incorrect"
                        saveBtn.isEnabled = true
                        return@runOnUiThread
                    }
                    lifecycleScope.launch {
                        db.userDao().updatePassword(user.id, PasswordUtils.hash(newPass))
                        runOnUiThread {
                            Toast.makeText(this@ChangePasswordActivity, "Password changed successfully ✅", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
