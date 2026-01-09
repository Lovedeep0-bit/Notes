package com.lsj.notes.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val trashedNotes: Flow<List<Note>> = noteDao.getTrashedNotes()
    val allNotebooks: Flow<List<Notebook>> = noteDao.getAllNotebooks()

    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    fun getNotesByNotebook(notebookId: Long): Flow<List<Note>> {
        return noteDao.getNotesByNotebook(notebookId)
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query)
    }

    suspend fun insertNotebook(notebook: Notebook) {
        noteDao.insertNotebook(notebook)
    }

    suspend fun updateNotebook(notebook: Notebook) {
        noteDao.updateNotebook(notebook)
    }

    suspend fun deleteNotebook(notebook: Notebook) {
        noteDao.deleteNotebook(notebook)
    }
}
