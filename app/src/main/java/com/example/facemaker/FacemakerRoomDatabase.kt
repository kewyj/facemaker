package com.example.facemaker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [FacemakerStruct::class], version = 2, exportSchema = false)
public abstract class FacemakerRoomDatabase: RoomDatabase() {
    public abstract fun facemakerDao(): FacemakerDao

    private class FacemakerDatabaseCallback(private val scope: CoroutineScope): RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.facemakerDao())
                }
            }
        }

        suspend fun populateDatabase(facemakerDao: FacemakerDao) {

        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FacemakerRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FacemakerRoomDatabase {
            return INSTANCE?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FacemakerRoomDatabase::class.java,
                    "value_database"
                ).addCallback(FacemakerDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}