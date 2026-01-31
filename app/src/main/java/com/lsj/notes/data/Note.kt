package com.lsj.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class NoteType {
    NOTE, TODO
}

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val notebookId: Long? = null,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    val type: NoteType = NoteType.NOTE,
    val isCompleted: Boolean = false, // For Todo items
    val isTrashed: Boolean = false,
    val isPinned: Boolean = false
)
