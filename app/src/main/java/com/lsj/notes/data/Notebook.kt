package com.lsj.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val created: Long = System.currentTimeMillis()
)
