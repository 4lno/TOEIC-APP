package com.luudev.vocalens.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.luudev.vocalens.BuildConfig

object FirebaseServices {
    const val REGION = "asia-southeast1"
    private var emulatorApp: FirebaseApp? = null
    private fun app(): FirebaseApp = emulatorApp ?: FirebaseApp.getInstance()

    fun configureEmulators(context: Context) {
        val host = BuildConfig.FIREBASE_EMULATOR_HOST
        if (BuildConfig.DEBUG && host.isNotBlank()) {
            // Separate app/auth cache and demo project keep local tests away from production data.
            val options = FirebaseOptions.Builder(FirebaseApp.getInstance().options)
                .setProjectId("demo-vocablens").build()
            emulatorApp = FirebaseApp.initializeApp(context, options, "vocablens-emulator")
            auth().useEmulator(host, 9099)
            firestore().useEmulator(host, 8080)
            functions().useEmulator(host, 5001)
        }
    }

    fun auth(): FirebaseAuth = FirebaseAuth.getInstance(app())
    fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance(app())
    fun functions(): FirebaseFunctions = FirebaseFunctions.getInstance(app(), REGION)
}
