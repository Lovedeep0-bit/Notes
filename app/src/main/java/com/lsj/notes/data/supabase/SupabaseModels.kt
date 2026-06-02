package com.lsj.notes.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SupabaseNote(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("content")
    val content: String,
    
    @SerialName("notebook_id")
    val notebookId: String? = null,
    
    @SerialName("type")
    val type: String = "NOTE", // "NOTE" or "TODO"
    
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    
    @SerialName("created_at")
    val createdAt: String,
    
    @SerialName("updated_at")
    val updatedAt: String,
    
    @SerialName("synced")
    val synced: Boolean = true
)

@Serializable
data class SupabaseNotebook(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("created_at")
    val createdAt: String,
    
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class SupabaseProfile(
    @SerialName("id")
    val id: String,
    
    @SerialName("username")
    val username: String? = null,
    
    @SerialName("email")
    val email: String? = null,
    
    @SerialName("created_at")
    val createdAt: String? = null
)