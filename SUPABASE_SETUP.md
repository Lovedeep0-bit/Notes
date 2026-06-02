# Supabase Integration Guide

This document describes how to set up and use Supabase with the Notes app.

## Prerequisites

- A Supabase account (https://supabase.com)
- A Supabase project created

## Step 1: Create Supabase Project

1. Go to https://supabase.com and sign in
2. Create a new project
3. Note your **Project URL** and **Anon Key** from the API settings

## Step 2: Set Up Database Tables

In your Supabase project, go to the SQL Editor and run these commands:

```sql
-- Create profiles table
create table profiles (
    id uuid references auth.users on delete cascade,
    username text,
    email text,
    created_at timestamp with time zone default timezone('utc'::text, now()),
    primary key (id)
);

-- Enable RLS for profiles
alter table profiles enable row level security;

create policy "Users can only access their own profile"
    on profiles for all
    using (auth.uid() = id);

-- Create notebooks table
create table notebooks (
    id text primary key,
    user_id uuid not null references auth.users on delete cascade,
    name text not null,
    created_at timestamp with time zone default timezone('utc'::text, now()),
    updated_at timestamp with time zone default timezone('utc'::text, now())
);

-- Enable RLS for notebooks
alter table notebooks enable row level security;

create policy "Users can only access their own notebooks"
    on notebooks for all
    using (auth.uid() = user_id);

-- Create notes table
create table notes (
    id text primary key,
    user_id uuid not null references auth.users on delete cascade,
    title text not null,
    content text not null,
    notebook_id text references notebooks(id) on delete set null,
    type text default 'NOTE', -- 'NOTE' or 'TODO'
    is_pinned boolean default false,
    is_completed boolean default false,
    is_deleted boolean default false,
    created_at timestamp with time zone default timezone('utc'::text, now()),
    updated_at timestamp with time zone default timezone('utc'::text, now()),
    synced boolean default true
);

-- Create indexes for better performance
create index notes_user_id_idx on notes(user_id);
create index notes_user_id_deleted_idx on notes(user_id, is_deleted);
create index notes_notebook_id_idx on notes(notebook_id);

-- Enable RLS for notes
alter table notes enable row level security;

create policy "Users can only access their own notes"
    on notes for all
    using (auth.uid() = user_id);
```

## Step 3: Configure the App

### 1. Update Supabase Credentials

Edit `app/src/main/java/com/lsj/notes/config/SupabaseConfig.kt`:

```kotlin
object SupabaseConfig {
    const val SUPABASE_URL = "https://your-project.supabase.co"
    const val SUPABASE_ANON_KEY = "your-anon-key"
    
    // ... rest of config
}
```

Replace:
- `your-project` with your Supabase project name
- `your-anon-key` with your Anon Key from Supabase

### 2. Build the App

```bash
./gradlew build
```

## Step 4: Features

### Authentication
- Users can sign up with email and password
- Users can sign in with existing credentials
- Auth state is automatically synced to Supabase

### Automatic Sync
- Notes are automatically synced every 15 minutes (when device is connected to network)
- You can manually trigger sync in the UI
- Sync is conflict-free: local changes take precedence if more recent

### Offline Support
- All notes are stored locally in Room database
- Notes can be created, edited, and deleted offline
- Changes will be synced when connection is restored

## Step 5: Testing

### Test Sign Up
1. Open the app
2. Tap "Don't have an account? Sign Up"
3. Enter email and password
4. Tap "Sign Up"

### Test Sign In
1. Enter your credentials
2. Tap "Sign In"

### Test Sync
1. Create a note
2. Wait for automatic sync or trigger sync manually
3. Check Supabase console to verify note was uploaded

### Test Offline Support
1. Create a note
2. Turn off internet
3. Modify the note
4. Turn internet back on
5. Wait for sync or trigger manually

## Troubleshooting

### Notes not syncing
- Check internet connection
- Verify Supabase credentials in SupabaseConfig.kt
- Check app logs for errors

### Sync worker not running
- Check if WorkManager is properly initialized
- Ensure device has network connection
- Check battery saver mode is not blocking background work

### Authentication issues
- Verify email/password is correct
- Check Supabase Auth settings in dashboard
- Ensure user exists in Supabase Auth users table

## Advanced Configuration

### Change Sync Frequency
Edit `app/src/main/java/com/lsj/notes/work/SyncManager.kt`:

```kotlin
// Change 15 to desired minutes
15, TimeUnit.MINUTES // Sync every X minutes
```

### Customize Conflict Resolution
Edit `app/src/main/java/com/lsj/notes/data/supabase/SyncRepository.kt`:

In the `syncAllNotes` function, modify the `onConflict` logic to customize how conflicts are resolved.

## Security Notes

- **Never commit credentials** to version control
- Consider using environment variables or secure config files
- Supabase RLS policies are enabled to protect user data
- API key shown in app is the "anon" key with limited permissions

## API Reference

### SyncRepository Methods

- `uploadNote(note: Note)` - Upload single note to Supabase
- `downloadNotes()` - Download all notes for current user
- `deleteNote(noteId: String)` - Soft delete a note
- `permanentlyDeleteNote(noteId: String)` - Permanently delete a note
- `uploadNotebook(notebook: Notebook)` - Upload notebook
- `downloadNotebooks()` - Download all notebooks
- `syncAllNotes(...)` - Full sync with conflict resolution

### AuthRepository Methods

- `signUp(email: String, password: String)` - Create new account
- `signIn(email: String, password: String)` - Sign in to existing account
- `signOut()` - Sign out current user
- `getCurrentUserId()` - Get current authenticated user ID
- `isAuthenticated()` - Check if user is logged in

## Support

For issues with Supabase, visit: https://supabase.com/docs
