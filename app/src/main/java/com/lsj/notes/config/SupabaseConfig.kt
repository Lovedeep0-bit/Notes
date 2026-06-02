package com.lsj.notes.config

/**
 * Supabase configuration
 * Replace these with your actual Supabase project credentials
 * Get these from your Supabase project dashboard:
 * - Go to Settings > API Docs
 * - Copy the Project URL and anon (public) API key
 */
object SupabaseConfig {
    // Replace with your Supabase project URL
    const val SUPABASE_URL = "https://adbtooemgvqyprbnpvyn.supabase.co"
    
    // Replace with your Supabase anon key
    const val SUPABASE_ANON_KEY = "sb_publishable_EecaNX3F6OAIPQOKzrHJXg_MVhbLQIU"
    
    // Table names
    const val NOTES_TABLE = "notes"
    const val NOTEBOOKS_TABLE = "notebooks"
    const val PROFILES_TABLE = "profiles"
}
