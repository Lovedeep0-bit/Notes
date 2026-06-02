package com.lsj.notes.data.supabase

import android.content.Context
import com.lsj.notes.config.SupabaseConfig
import io.github.supabase.Supabase
import io.github.supabase.gotrue.GoTrue
import io.github.supabase.postgrest.Postgrest
import io.github.supabase.realtime.Realtime
import io.github.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseClient {
    private var isInitialized = false

    suspend fun initialize(context: Context) {
        if (!isInitialized) {
            Supabase.init(
                context = context,
                supabaseUrl = SupabaseConfig.SUPABASE_URL,
                supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY,
                block = {
                    install(GoTrue)
                    install(Postgrest)
                    install(Realtime)
                    install(Storage)
                }
            )
            isInitialized = true
        }
    }

    fun getClient() = Supabase.client
    
    fun isInitialized() = isInitialized
}
