package com.example.facemaker

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FacemakerViewModel(private val repository: FacemakerRepository) : ViewModel(){
    val allScores: LiveData<List<FacemakerStruct>> = repository.allScores

    suspend fun insert(newValue: FacemakerStruct){
        repository.insert(newValue)
    }

    fun getAllValuesLive(): LiveData<List<FacemakerStruct>> {
        return repository.getAllValuesLive()
    }

    suspend fun resetTable(){
        repository.resetTable()
    }
}

class FacemakerViewModelFactory(private val repository: FacemakerRepository): ViewModelProvider.Factory {
    override fun <T: ViewModel> create(modelClass: Class<T>): T{
        if (modelClass.isAssignableFrom(ViewModel::class.java)){
            return FacemakerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}