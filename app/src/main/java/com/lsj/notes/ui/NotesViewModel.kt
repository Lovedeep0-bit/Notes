package com.lsj.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lsj.notes.data.Note
import com.lsj.notes.data.NoteRepository
import com.lsj.notes.data.Notebook
import com.lsj.notes.data.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NoteRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLocked = MutableStateFlow(userPreferencesRepository.biometricEnabled.value)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastUnlockTime: Long = 0
    private val lockTimeout = 5 * 60 * 1000 // 5 minutes in milliseconds

    @OptIn(ExperimentalCoroutinesApi::class)
    val allNotes: StateFlow<List<Note>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allNotes
            } else {
                repository.searchNotes(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = repository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotebooks: StateFlow<List<Notebook>> = repository.allNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedView: StateFlow<String> = userPreferencesRepository.selectedView
    val theme: StateFlow<String> = userPreferencesRepository.theme
    val showOnlyTitles: StateFlow<Boolean> = userPreferencesRepository.showOnlyTitles
    val biometricEnabled: StateFlow<Boolean> = userPreferencesRepository.biometricEnabled

    fun onSelectedViewChanged(view: String) {
        userPreferencesRepository.saveSelectedView(view)
    }

    fun onThemeChanged(theme: String) {
        userPreferencesRepository.saveTheme(theme)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onShowOnlyTitlesChanged(showOnlyTitles: Boolean) {
        userPreferencesRepository.saveShowOnlyTitles(showOnlyTitles)
    }

    fun onBiometricEnabledChanged(enabled: Boolean) {
        userPreferencesRepository.saveBiometricEnabled(enabled)
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun unlock() {
        _isLocked.value = false
        lastUnlockTime = System.currentTimeMillis()
    }

    fun checkLockState() {
        if (userPreferencesRepository.biometricEnabled.value) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUnlockTime > lockTimeout) {
                _isLocked.value = true
            }
        } else {
            _isLocked.value = false
        }
    }

    suspend fun getNoteById(id: Long): Note? {
        return repository.getNoteById(id)
    }

    fun addNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            // Soft delete: move to trash if not already trashed
            if (!note.isTrashed) {
                repository.updateNote(note.copy(isTrashed = true))
            } else {
                // Hard delete if already in trash
                repository.deleteNote(note)
            }
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isTrashed = false))
        }
    }

    fun toggleTodo(note: Note) {
        viewModelScope.launch {
            val nextState = !note.isCompleted
            val updatedContent = if (nextState) {
                note.content.replace("[ ] ", "[x] ")
            } else {
                note.content.replace("[x] ", "[ ] ")
            }
            repository.updateNote(note.copy(
                isCompleted = nextState, 
                content = updatedContent,
                updated = System.currentTimeMillis()
            ))
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updated = System.currentTimeMillis()))
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            // Find the base title (without copy number)
            val baseTitle = note.title.replace(Regex(" \\(\\d+\\)$"), "").trim()
            
            // Find existing copies by checking titles
            val existingNotes = allNotes.value
            val copyNumbers = existingNotes
                .filter { it.title.startsWith(baseTitle) }
                .mapNotNull { n ->
                    if (n.title == baseTitle) 1
                    else Regex("\\((\\d+)\\)$").find(n.title)?.groupValues?.get(1)?.toIntOrNull()
                }
            
            val nextNumber = (copyNumbers.maxOrNull() ?: 1) + 1
            val newTitle = "$baseTitle ($nextNumber)"
            
            val duplicate = note.copy(
                id = 0,
                title = newTitle,
                created = System.currentTimeMillis(),
                updated = System.currentTimeMillis()
            )
            repository.insertNote(duplicate)
        }
    }

    fun moveNoteToNotebook(note: Note, notebookId: Long?) {
        viewModelScope.launch {
            repository.updateNote(note.copy(notebookId = notebookId, updated = System.currentTimeMillis()))
        }
    }

    fun addNotebook(notebook: Notebook) {
        viewModelScope.launch {
            repository.insertNotebook(notebook)
        }
    }

    fun renameNotebook(notebook: Notebook, newName: String) {
        viewModelScope.launch {
            repository.updateNotebook(notebook.copy(name = newName))
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch {
            // Move all notes in this notebook to "All Notes" first
            allNotes.value.filter { it.notebookId == notebook.id }.forEach { note ->
                repository.updateNote(note.copy(notebookId = null))
            }
            repository.deleteNotebook(notebook)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashedNotes.value.forEach { note ->
                repository.deleteNote(note)
            }
        }
    }
}

class NotesViewModelFactory(
    private val repository: NoteRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
