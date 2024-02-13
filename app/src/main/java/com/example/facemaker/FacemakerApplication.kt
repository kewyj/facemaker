package com.example.facemaker

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class FacemakerApplication: Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    lateinit var repository: FacemakerRepository

    override fun onCreate() {
        super.onCreate()
        repository = FacemakerRepository(FacemakerRoomDatabase.getDatabase(this, applicationScope).facemakerDao())
    }
}