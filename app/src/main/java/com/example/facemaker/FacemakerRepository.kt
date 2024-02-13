package com.example.facemaker

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class FacemakerRepository(private val facemakerDao: FacemakerDao) {
    val allScores: LiveData<List<FacemakerStruct>> = facemakerDao.getAllValLive()

    @WorkerThread
    suspend fun insert(value: FacemakerStruct) {
        facemakerDao.insert(value)
    }

    fun getAllValuesLive() : LiveData<List<FacemakerStruct>> {
        return facemakerDao.getAllValLive()
    }

    @WorkerThread
    suspend fun resetTable() {
        facemakerDao.resetTable()
    }
}