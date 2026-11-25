package com.luudev.vocalens

import android.app.Application
import com.luudev.vocalens.data.remote.FirebaseServices

class VocabLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseServices.configureEmulators(this)
    }
}
