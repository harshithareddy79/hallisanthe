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
import com.hallisanthi.digital.data.db.ProductDatabase
import kotlinx.coroutines.launch

/**
 * 3-step forgot-password flow:
 *  Step 1 → Enter email
 *  Step 2 → Enter 6-digit token shown on screen (simulates email delivery)
 *  Step 3 → Set new password
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private var userId  = -1L
    private var storedToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reset Password"

        showStep1()
    }

    // ── Step 1: Email entry ───────────────────────────────────────────────────
    private fun showStep1() {
        setContentView(R.layout.activity_forgot_password)
        val emailLayout = findViewById<TextInputLayout>(R.id.fpEmailLayout)
        val emailEdit   = findViewById<TextInputEditText>(R.id.fpEmailEdit)
        val sendBtn     = findViewById<MaterialButton>(R.id.fpSendBtn)
        val progress    = findViewById<ProgressBar>(R.id.fpProgress)

        sendBtn.setOnClickListener {
            val email = emailEdit.text.toString().trim().lowercase()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Enter a valid email"; return@setOnClickListener
            }
            emailLayout.error = null
            progress.visibility = View.VISIBLE
            sendBtn.isEnabled = false

            lifecycleScope.launch {
                val db   = ProductDatabase.getDatabase(applicationContext)
                val user = db.userDao().getByEmail(email)
                runOnUiThread {
                    progress.visibility = View.GONE
                    sendBtn.isEnabled = true
                    if (user == null) {
                        emailLayout.error = "No account found with this email"
                    } else {
                        userId = user.id
                        val token  = PasswordUtils.generateResetToken()
                        val expiry = System.currentTimeMillis() + 15 * 60 * 1000L // 15 min
                        lifecycleScope.launch {
                            db.userDao().setResetToken(userId, token, expiry)
                            storedToken = token
                            runOnUiThread { showStep2(email, token) }
                        }
                    }
                }
            }
        }
    }

    // ── Step 2: Token verification ────────────────────────────────────────────
    private fun showStep2(email: String, token: String) {
        setContentView(R.layout.activity_forgot_password_step2)
        supportActionBar?.title = "Enter Reset Code"

        val infoText  = findViewById<TextView>(R.id.fpStep2Info)
        val tokenLayout = findViewById<TextInputLayout>(R.id.fpTokenLayout)
        val tokenEdit   = findViewById<TextInputEditText>(R.id.fpTokenEdit)
        val verifyBtn   = findViewById<MaterialButton>(R.id.fpVerifyBtn)
        val devToken    = findViewById<TextView>(R.id.fpDevTokenHint)

        infoText.text = "A reset code was sent to $email"
        // In a real app this would be emailed; for now we show it on screen
        devToken.text = "⚠️ Dev mode — your code: $token"
        devToken.visibility = View.VISIBLE

        verifyBtn.setOnClickListener {
            val entered = tokenEdit.text.toString().trim()
            if (entered == storedToken) {
                tokenLayout.error = null
                showStep3()
            } else {
                tokenLayout.error = "Invalid code. Try again."
            }
        }
    }

    // ── Step 3: New password ──────────────────────────────────────────────────
    private fun showStep3() {
        setContentView(R.layout.activity_forgot_password_step3)
        supportActionBar?.title = "Set New Password"

        val passLayout    = findViewById<TextInputLayout>(R.id.fpNewPassLayout)
        val passEdit      = findViewById<TextInputEditText>(R.id.fpNewPassEdit)
        val confirmLayout = findViewById<TextInputLayout>(R.id.fpConfirmLayout)
        val confirmEdit   = findViewById<TextInputEditText>(R.id.fpConfirmEdit)
        val saveBtn       = findViewById<MaterialButton>(R.id.fpSaveBtn)
        val strengthBar   = findViewById<ProgressBar>(R.id.fpStrengthBar)
        val strengthLabel = findViewById<TextView>(R.id.fpStrengthLabel)

        passEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val pw = s.toString()
                when (PasswordUtils.isStrong(pw)) {
                    PasswordUtils.PasswordStrength.WEAK   -> { strengthBar.progress = 33; strengthLabel.text = "Weak" }
                    PasswordUtils.PasswordStrength.MEDIUM -> { strengthBar.progress = 66; strengthLabel.text = "Medium" }
                    PasswordUtils.PasswordStrength.STRONG -> { strengthBar.progress = 100; strengthLabel.text = "Strong 💪" }
                }
            }
        })

        saveBtn.setOnClickListener {
            val pass    = passEdit.text.toString()
            val confirm = confirmEdit.text.toString()
            var valid = true
            if (PasswordUtils.isStrong(pass) == PasswordUtils.PasswordStrength.WEAK) {
                passLayout.error = "Password too weak"; valid = false
            } else passLayout.error = null
            if (pass != confirm) { confirmLayout.error = "Passwords do not match"; valid = false } else confirmLayout.error = null
            if (!valid) return@setOnClickListener

            lifecycleScope.launch {
                val db = ProductDatabase.getDatabase(applicationContext)
                db.userDao().updatePassword(userId, PasswordUtils.hash(pass))
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity,
                        "Password updated! Please log in. ✅", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
