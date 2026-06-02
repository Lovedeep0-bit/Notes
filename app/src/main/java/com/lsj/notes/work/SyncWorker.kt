package com.lsj.notes.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lsj.notes.data.AppDatabase
import com.lsj.notes.data.AuthRepository
import com.lsj.notes.data.NoteRepository
import com.lsj.notes.data.supabase.SyncRepository
import com.lsj.notes.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            // Initialize Supabase if not already done
            if (!SupabaseClient.isInitialized()) {
                SupabaseClient.initialize(applicationContext)
            }

            val authRepository = AuthRepository()
            
            // Check if user is authenticated
            if (!authRepository.isAuthenticated()) {
                Log.d("SyncWorker", "User not authenticated, skipping sync")
                return@withContext Result.retry()
            }

            val database = AppDatabase.getDatabase(applicationContext)
            val noteRepository = NoteRepository(database.noteDao())
            val syncRepository = SyncRepository()

            // Get all local notes
            val localNotes = noteRepository.allNotes.first()

            // Sync notes
            var syncedCount = 0
            localNotes.forEach { note ->
                val result = syncRepository.uploadNote(note)
                if (result.isSuccess) {
                    syncedCount++
                }
            }
            
            Log.d("SyncWorker", "Successfully synced $syncedCount notes")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Worker error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val TAG = "note_sync"
    }
}