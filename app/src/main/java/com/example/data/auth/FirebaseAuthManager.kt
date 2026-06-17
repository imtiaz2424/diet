package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String = ""
)

class FirebaseAuthManager(private val context: Context) {
    private var firebaseAuth: FirebaseAuth? = null
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _isFirebaseReal = MutableStateFlow(false)
    val isFirebaseReal: StateFlow<Boolean> = _isFirebaseReal.asStateFlow()

    init {
        try {
            // Check if Firebase keys are in BuildConfig or if there's any active FirebaseApp
            val apiKey = try { BuildConfig::class.java.getField("FIREBASE_API_KEY").get(null) as? String } catch (e: Exception) { null } ?: ""
            val appId = try { BuildConfig::class.java.getField("FIREBASE_APP_ID").get(null) as? String } catch (e: Exception) { null } ?: ""
            val projectId = try { BuildConfig::class.java.getField("FIREBASE_PROJECT_ID").get(null) as? String } catch (e: Exception) { null } ?: ""

            if (apiKey.isNotEmpty() && appId.isNotEmpty() && projectId.isNotEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                
                FirebaseApp.initializeApp(context.applicationContext, options)
                firebaseAuth = FirebaseAuth.getInstance()
                _isFirebaseReal.value = true
                Log.d("FirebaseAuthManager", "Firebase successfully initialized manually as REAL!")
                
                // Track user
                firebaseAuth?.currentUser?.let { user ->
                    _currentUser.value = AuthUser(user.uid, user.email ?: "")
                }
            } else {
                Log.d("FirebaseAuthManager", "Firebase api keys are missing or blank. Starting in SIMULATED mode.")
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Error initializing REAL Firebase: ${e.message}. Using simulated mode.", e)
        }
        
        // If simulated mode, restore last logged-in user from SharedPreferences
        if (firebaseAuth == null) {
            val sharedPrefs = context.getSharedPreferences("anexsopz_auth_simulated", Context.MODE_PRIVATE)
            val uid = sharedPrefs.getString("uid", null)
            val email = sharedPrefs.getString("email", null)
            if (uid != null && email != null) {
                _currentUser.value = AuthUser(uid, email)
            }
        }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val authUser = AuthUser(user.uid, user.email ?: "")
                            _currentUser.value = authUser
                            onResult(true, null)
                        } else {
                            onResult(false, "User is null after account creation")
                        }
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Unknown creation error")
                    }
                }
        } else {
            // Simulated Flow - save to shared preferences dummy db
            val sharedPrefs = context.getSharedPreferences("anexsopz_auth_simulated_db", Context.MODE_PRIVATE)
            if (sharedPrefs.contains(email)) {
                onResult(false, if (email.contains("@")) "Email already registered!" else "Account already exists!")
                return
            }
            sharedPrefs.edit().putString(email, password).apply()
            
            // Set current user
            val uid = "sim_" + Math.abs(email.hashCode()).toString()
            val authUser = AuthUser(uid, email)
            _currentUser.value = authUser
            
            // Persist session
            val sessionPrefs = context.getSharedPreferences("anexsopz_auth_simulated", Context.MODE_PRIVATE)
            sessionPrefs.edit().putString("uid", uid).putString("email", email).apply()
            
            onResult(true, null)
        }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val authUser = AuthUser(user.uid, user.email ?: "")
                            _currentUser.value = authUser
                            onResult(true, null)
                        } else {
                            onResult(false, "User is null after sign in")
                        }
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Invalid email or password")
                    }
                }
        } else {
            // Simulated Flow
            val sharedPrefs = context.getSharedPreferences("anexsopz_auth_simulated_db", Context.MODE_PRIVATE)
            val savedPassword = sharedPrefs.getString(email, null)
            
            // Allow Quick Demo Login / Default member easily
            if ((email == "user@anexsopz.com" && password == "password123") || savedPassword == password) {
                val uid = "sim_" + Math.abs(email.hashCode()).toString()
                val authUser = AuthUser(uid, email)
                _currentUser.value = authUser
                
                // Persist session
                val sessionPrefs = context.getSharedPreferences("anexsopz_auth_simulated", Context.MODE_PRIVATE)
                sessionPrefs.edit().putString("uid", uid).putString("email", email).apply()
                
                onResult(true, null)
            } else {
                onResult(false, "Invalid credentials or password! If new, please Sign Up.")
            }
        }
    }

    fun signOut() {
        firebaseAuth?.signOut()
        _currentUser.value = null
        
        val sessionPrefs = context.getSharedPreferences("anexsopz_auth_simulated", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().apply()
    }
}
