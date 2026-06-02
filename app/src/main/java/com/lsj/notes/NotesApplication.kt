package com.lsj.notes

import android.app.Application
import android.util.Log
import com.lsj.notes.data.AppDatabase
import com.lsj.notes.data.AuthRepository
import com.lsj.notes.data.NoteRepository
import com.lsj.notes.data.UserPreferencesRepository
import com.lsj.notes.data.supabase.SupabaseClient
import com.lsj.notes.work.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val authRepository by lazy { AuthRepository() }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Supabase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseClient.initialize(this@NotesApplication)
                Log.d("NotesApplication", "Supabase initialized successfully")
                
                // Schedule periodic sync
                SyncManager.scheduleSyncWorker(this@NotesApplication)
                Log.d("NotesApplication", "Sync worker scheduled")
            } catch (e: Exception) {
                Log.e("NotesApplication", "Failed to initialize Supabase: ${e.message}")
            }
        }
    }
}
