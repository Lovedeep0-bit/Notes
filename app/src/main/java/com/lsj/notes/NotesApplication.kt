package com.lsj.notes

import android.app.Application
import com.lsj.notes.data.AppDatabase
import com.lsj.notes.data.NoteRepository
import com.lsj.notes.data.UserPreferencesRepository

class NotesApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
}
