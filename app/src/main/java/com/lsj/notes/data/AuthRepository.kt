package com.lsj.notes.data

import com.lsj.notes.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository {
    private val client = SupabaseClient.getClient()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        val currentUser = client.auth.currentUserOrNull()
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(
                userId = currentUser.id,
                email = currentUser.email ?: ""
            )
        }
    }

    suspend fun signUp(email: String, password: String): Result<String> = try {
        // Supabase signup would go here
        Result.success("signup-success")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signIn(email: String, password: String): Result<String> = try {
        // Supabase signin would go here
        Result.success("signin-success")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut(): Result<Unit> = try {
        _authState.value = AuthState.Unauthenticated
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    fun isAuthenticated(): Boolean {
        return client.auth.currentUserOrNull() != null
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val email: String) : AuthState()
}