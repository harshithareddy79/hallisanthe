package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hallisanthi.digital.MainActivity
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.PasswordUtils
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.User
import kotlinx.coroutines.launch

/**
 * Full email + password Login / Register screen (v5).
 *
 * Tabs:
 *  LOGIN    — email + password
 *  REGISTER — name + email + phone + password + confirm + role
 *
 * Links:
 *  "Forgot Password?" → ForgotPasswordActivity
 */
class LoginActivity : AppCompatActivity() {

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var tabLogin: TextView
    private lateinit var tabRegister: TextView
    private lateinit var tabIndicator: View

    // Login panel
    private lateinit var panelLogin: View
    private lateinit var loginEmailLayout: TextInputLayout
    private lateinit var loginEmailEdit: TextInputEditText
    private lateinit var loginPassLayout: TextInputLayout
    private lateinit var loginPassEdit: TextInputEditText
    private lateinit var loginBtn: MaterialButton
    private lateinit var forgotPasswordLink: TextView

    // Register panel
    private lateinit var panelRegister: View
    private lateinit var regNameLayout: TextInputLayout
    private lateinit var regNameEdit: TextInputEditText
    private lateinit var regEmailLayout: TextInputLayout
    private lateinit var regEmailEdit: TextInputEditText
    private lateinit var regPhoneLayout: TextInputLayout
    private lateinit var regPhoneEdit: TextInputEditText
    private lateinit var regPassLayout: TextInputLayout
    private lateinit var regPassEdit: TextInputEditText
    private lateinit var regConfirmLayout: TextInputLayout
    private lateinit var regConfirmEdit: TextInputEditText
    private lateinit var regRoleGroup: RadioGroup
    private lateinit var regBtn: MaterialButton
    private lateinit var passwordStrengthBar: ProgressBar
    private lateinit var passwordStrengthLabel: TextView

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.isLoggedIn(this)) { goToMain(); return }
        setContentView(R.layout.activity_login)
        bindViews()
        setupTabs()
        setupLoginPanel()
        setupRegisterPanel()
    }

    private fun bindViews() {
        tabLogin    = findViewById(R.id.tabLogin)
        tabRegister = findViewById(R.id.tabRegister)
        tabIndicator = findViewById(R.id.tabIndicator)

        panelLogin  = findViewById(R.id.panelLogin)
        panelRegister = findViewById(R.id.panelRegister)

        loginEmailLayout = findViewById(R.id.loginEmailLayout)
        loginEmailEdit   = findViewById(R.id.loginEmailEdit)
        loginPassLayout  = findViewById(R.id.loginPassLayout)
        loginPassEdit    = findViewById(R.id.loginPassEdit)
        loginBtn         = findViewById(R.id.loginBtn)
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink)

        regNameLayout    = findViewById(R.id.regNameLayout)
        regNameEdit      = findViewById(R.id.regNameEdit)
        regEmailLayout   = findViewById(R.id.regEmailLayout)
        regEmailEdit     = findViewById(R.id.regEmailEdit)
        regPhoneLayout   = findViewById(R.id.regPhoneLayout)
        regPhoneEdit     = findViewById(R.id.regPhoneEdit)
        regPassLayout    = findViewById(R.id.regPassLayout)
        regPassEdit      = findViewById(R.id.regPassEdit)
        regConfirmLayout = findViewById(R.id.regConfirmLayout)
        regConfirmEdit   = findViewById(R.id.regConfirmEdit)
        regRoleGroup     = findViewById(R.id.regRoleGroup)
        regBtn           = findViewById(R.id.regBtn)
        passwordStrengthBar   = findViewById(R.id.passwordStrengthBar)
        passwordStrengthLabel = findViewById(R.id.passwordStrengthLabel)
        progressBar = findViewById(R.id.loginProgress)
    }

    private fun setupTabs() {
        tabLogin.setOnClickListener    { showLogin() }
        tabRegister.setOnClickListener { showRegister() }
        showLogin()
    }

    private fun showLogin() {
        panelLogin.visibility    = View.VISIBLE
        panelRegister.visibility = View.GONE
        tabLogin.setTextColor(getColor(R.color.primary))
        tabRegister.setTextColor(getColor(R.color.text_secondary))
        tabIndicator.animate().translationX(0f).setDuration(200).start()
    }

    private fun showRegister() {
        panelLogin.visibility    = View.GONE
        panelRegister.visibility = View.VISIBLE
        tabLogin.setTextColor(getColor(R.color.text_secondary))
        tabRegister.setTextColor(getColor(R.color.primary))
        val tabWidth = tabLogin.width.toFloat()
        tabIndicator.animate().translationX(tabWidth).setDuration(200).start()
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    private fun setupLoginPanel() {
        forgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginBtn.setOnClickListener {
            val email = loginEmailEdit.text.toString().trim().lowercase()
            val pass  = loginPassEdit.text.toString()

            var valid = true
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                loginEmailLayout.error = "Enter a valid email address"; valid = false
            } else loginEmailLayout.error = null

            if (pass.length < 6) {
                loginPassLayout.error = "Password must be at least 6 characters"; valid = false
            } else loginPassLayout.error = null

            if (!valid) return@setOnClickListener

            setLoading(true)
            lifecycleScope.launch {
                val db   = ProductDatabase.getDatabase(applicationContext)
                val user = db.userDao().getByEmail(email)
                runOnUiThread {
                    setLoading(false)
                    when {
                        user == null ->
                            loginEmailLayout.error = "No account found with this email"
                        !PasswordUtils.verify(pass, user.passwordHash) ->
                            loginPassLayout.error = "Incorrect password"
                        else -> {
                            lifecycleScope.launch { db.userDao().updateLastLogin(user.id) }
                            UserSession.login(this@LoginActivity, user)
                            goToMain()
                        }
                    }
                }
            }
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────
    private fun setupRegisterPanel() {
        // Live password-strength meter
        regPassEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val pw = s.toString()
                if (pw.isEmpty()) {
                    passwordStrengthBar.progress = 0
                    passwordStrengthLabel.text = ""
                    return
                }
                when (PasswordUtils.isStrong(pw)) {
                    PasswordUtils.PasswordStrength.WEAK -> {
                        passwordStrengthBar.progress = 33
                        passwordStrengthBar.progressTintList =
                            android.content.res.ColorStateList.valueOf(getColor(R.color.error_red))
                        passwordStrengthLabel.text = "Weak"
                        passwordStrengthLabel.setTextColor(getColor(R.color.error_red))
                    }
                    PasswordUtils.PasswordStrength.MEDIUM -> {
                        passwordStrengthBar.progress = 66
                        passwordStrengthBar.progressTintList =
                            android.content.res.ColorStateList.valueOf(getColor(R.color.warning_yellow))
                        passwordStrengthLabel.text = "Medium"
                        passwordStrengthLabel.setTextColor(getColor(R.color.warning_yellow))
                    }
                    PasswordUtils.PasswordStrength.STRONG -> {
                        passwordStrengthBar.progress = 100
                        passwordStrengthBar.progressTintList =
                            android.content.res.ColorStateList.valueOf(getColor(R.color.success_green))
                        passwordStrengthLabel.text = "Strong 💪"
                        passwordStrengthLabel.setTextColor(getColor(R.color.success_green))
                    }
                }
            }
        })

        regBtn.setOnClickListener {
            val name    = regNameEdit.text.toString().trim()
            val email   = regEmailEdit.text.toString().trim().lowercase()
            val phone   = regPhoneEdit.text.toString().replace(Regex("[^0-9]"), "")
            val pass    = regPassEdit.text.toString()
            val confirm = regConfirmEdit.text.toString()
            val role    = if (regRoleGroup.checkedRadioButtonId == R.id.regRadioBuyer)
                            User.ROLE_BUYER else User.ROLE_ARTISAN

            var valid = true
            if (name.length < 2) { regNameLayout.error = "Name must be at least 2 characters"; valid = false } else regNameLayout.error = null
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { regEmailLayout.error = "Enter a valid email"; valid = false } else regEmailLayout.error = null
            if (phone.length != 10) { regPhoneLayout.error = "Enter a valid 10-digit number"; valid = false } else regPhoneLayout.error = null
            if (PasswordUtils.isStrong(pass) == PasswordUtils.PasswordStrength.WEAK) { regPassLayout.error = "Password too weak (min 8 chars + uppercase + digit)"; valid = false } else regPassLayout.error = null
            if (pass != confirm) { regConfirmLayout.error = "Passwords do not match"; valid = false } else regConfirmLayout.error = null
            if (!valid) return@setOnClickListener

            setLoading(true)
            lifecycleScope.launch {
                val db   = ProductDatabase.getDatabase(applicationContext)
                val dao  = db.userDao()

                val emailTaken = dao.emailExists(email)
                val phoneTaken = dao.phoneExists(phone)

                runOnUiThread {
                    if (emailTaken) { regEmailLayout.error = "Email already registered"; setLoading(false); return@runOnUiThread }
                    if (phoneTaken) { regPhoneLayout.error = "Phone already registered"; setLoading(false); return@runOnUiThread }
                }

                if (!emailTaken && !phoneTaken) {
                    val newUser = User(
                        name         = name,
                        email        = email,
                        phone        = phone,
                        passwordHash = PasswordUtils.hash(pass),
                        role         = role,
                        createdAt    = System.currentTimeMillis(),
                        lastLoginAt  = System.currentTimeMillis()
                    )
                    val id = dao.insert(newUser)
                    runOnUiThread {
                        setLoading(false)
                        if (id > 0) {
                            UserSession.login(this@LoginActivity, newUser.copy(id = id))
                            Toast.makeText(this@LoginActivity, "Welcome to Halli-Santhe Digital! 🙏", Toast.LENGTH_SHORT).show()
                            goToMain()
                        } else {
                            regEmailLayout.error = "Registration failed. Try again."
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(on: Boolean) {
        progressBar.visibility = if (on) View.VISIBLE else View.GONE
        loginBtn.isEnabled    = !on
        regBtn.isEnabled      = !on
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
