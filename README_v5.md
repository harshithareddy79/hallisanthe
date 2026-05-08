# Halli-Santhe Digital — v5 Upgrade Notes

## 🆕 New Features in v5

### 🔐 Full Email + Password Authentication
- **Register** with: Full Name, Email, Phone, Password, Role (Buyer/Artisan)
- **Login** with: Email + Password
- Passwords are hashed using SHA-256 + random salt (secure local storage)
- **Password strength meter** — real-time Weak / Medium / Strong indicator
- Minimum requirements: 8+ chars, uppercase, digit, special character

### 🔑 Forgot Password (3-step flow)
1. Enter registered email
2. Enter 6-digit reset code (shown on screen in dev mode; in production this would be emailed)
3. Set a new strong password

### 🛡️ Change Password (in-app)
- From Profile → "Change Password"
- Verifies current password before allowing update
- Shows strength meter for new password

### 👤 Enhanced Profile Screen
- Displays Email, Phone, Role, Name
- Edit Name inline
- Change Password button
- Secure logout with confirmation

## 🗄️ Database Migration
- Room DB version bumped: 4 → 5
- Users table rebuilt with new columns:
  - `email` (unique index)
  - `passwordHash`
  - `bio`, `location`, `profileImagePath`
  - `isEmailVerified`, `resetToken`, `resetTokenExpiry`
  - `lastLoginAt`

## 📁 New Files
- `ui/ForgotPasswordActivity.kt`
- `ui/ChangePasswordActivity.kt`
- `data/PasswordUtils.kt`
- `res/layout/activity_forgot_password.xml`
- `res/layout/activity_forgot_password_step2.xml`
- `res/layout/activity_forgot_password_step3.xml`
- `res/layout/activity_change_password.xml`

## 🔧 Modified Files
- `models/User.kt` — added email, passwordHash, and new fields
- `data/dao/UserDao.kt` — added email/reset token queries
- `data/UserSession.kt` — added email caching
- `data/db/ProductDatabase.kt` — migration 4→5
- `ui/LoginActivity.kt` — full rebuild with tab UI
- `ui/ProfileActivity.kt` — email display + change password
- `res/layout/activity_login.xml` — new tab-based login/register UI
- `res/layout/activity_profile.xml` — email row + change password button
- `AndroidManifest.xml` — registered new activities

## ▶️ How to Build
1. Open in Android Studio Giraffe (or newer)
2. `Build > Make Project`
3. Run on emulator or device (min SDK 21 / Android 5.0)

## ⚠️ Notes for Production
- Replace SHA-256 password hashing with **bcrypt** (e.g., `BCrypt` library) for stronger security
- Replace the dev-mode token display with a real email/SMS delivery service (Firebase, Twilio, etc.)
- Consider adding biometric (fingerprint) login using `BiometricPrompt` API
