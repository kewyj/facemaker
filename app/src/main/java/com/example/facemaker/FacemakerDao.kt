package com.example.facemaker

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
@Dao
interface FacemakerDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: FacemakerStruct)

    @Query("SElECT * FROM value_table")
    fun getAllValLive(): LiveData<List<FacemakerStruct>>

    @Query("SElECT * FROM value_table")
    fun getAllVal(): List<FacemakerStruct>

    @Query("SELECT MAX(uid) FROM value_table")
    fun getLastVal(): Int

    @Query("DELETE FROM value_table")
    suspend fun deleteAll()

    @Query("UPDATE sqlite_sequence SET seq = 0 WHERE name = 'value_table'")
    fun resetKey()

    suspend fun resetTable(){
        deleteAll()
        resetKey()
    }

    @Query("SELECT COUNT(uid) FROM value_table")
    fun getSize(): LiveData<Int>

    //@Query("DELETE FROM value_table WHERE fourDigitVal IS value")
    //suspend fun deleteValue(value: Int)
}