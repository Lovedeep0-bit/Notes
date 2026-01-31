package com.lsj.notes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.lsj.notes.ui.*
import com.lsj.notes.ui.theme.NotesTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val repository = (application as NotesApplication).repository
            val userPrefs = (application as NotesApplication).userPreferencesRepository
            val viewModel: NotesViewModel = viewModel(
                factory = NotesViewModelFactory(repository, userPrefs)
            )
            val themePreference by viewModel.theme.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val biometricEnabled by viewModel.biometricEnabled.collectAsState()

            // Observe lifecycle to check lock state when returning to app
            DisposableEffect(Lifecycle.Event.ON_START) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        viewModel.checkLockState()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                }
            }

            // Trigger biometric prompt when locked
            LaunchedEffect(isLocked) {
                if (isLocked && biometricEnabled) {
                    showBiometricPrompt(viewModel)
                }
            }

            NotesTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLocked && biometricEnabled) {
                        // Simple overlay while locked - could be more fancy
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(16.dp))
                                Text("App Locked", style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { showBiometricPrompt(viewModel) }) {
                                    Text("Unlock with Biometrics")
                                }
                            }
                        }
                    } else {
                        NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        val notes by viewModel.allNotes.collectAsState()
                        val trashedNotes by viewModel.trashedNotes.collectAsState()
                        val notebooks by viewModel.allNotebooks.collectAsState()
                        val searchQuery by viewModel.searchQuery.collectAsState()
                        val selectedView by viewModel.selectedView.collectAsState()
                        val showOnlyTitles by viewModel.showOnlyTitles.collectAsState()

                        HomeScreen(
                            notes = notes,
                            trashedNotes = trashedNotes,
                            notebooks = notebooks,
                            searchQuery = searchQuery,
                            currentSelectedView = selectedView,
                            onSelectedViewChanged = viewModel::onSelectedViewChanged,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onNoteClick = { note ->
                                navController.navigate("editor/${note.id}?type=${note.type.name}")
                            },
                            onAddNoteClick = { type, notebookId ->
                                navController.navigate("editor/-1?type=${type.name}&notebookId=${notebookId ?: -1L}")
                            },
                            onDeleteNote = { note ->
                                viewModel.deleteNote(note)
                            },
                            onRestoreNote = { note ->
                                viewModel.restoreNote(note)
                            },
                            onDeleteForever = { note ->
                                viewModel.deleteNote(note) // Hard delete if isTrashed is true
                            },
                            onToggleTodo = { note ->
                                viewModel.toggleTodo(note)
                            },
                            onAddNotebook = { name ->
                                viewModel.addNotebook(com.lsj.notes.data.Notebook(name = name))
                            },
                            onDuplicateNote = { note ->
                                viewModel.duplicateNote(note)
                            },
                            onMoveNote = { note, notebookId ->
                                viewModel.moveNoteToNotebook(note, notebookId)
                            },
                            onRenameNotebook = { notebook, newName ->
                                viewModel.renameNotebook(notebook, newName)
                            },
                            onDeleteNotebook = { notebook ->
                                viewModel.deleteNotebook(notebook)
                            },
                            onTogglePin = { note ->
                                viewModel.togglePin(note)
                            },
                            onUpdateNote = viewModel::updateNote,
                            onEmptyTrash = viewModel::emptyTrash,
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            showOnlyTitles = showOnlyTitles
                        )
                    }
                    composable("settings") {
                        val currentTheme by viewModel.theme.collectAsState()
                        val showOnlyTitles by viewModel.showOnlyTitles.collectAsState()
                        val biometricEnabled by viewModel.biometricEnabled.collectAsState()
                        
                        SettingsScreen(
                            currentTheme = currentTheme,
                            onThemeChanged = viewModel::onThemeChanged,
                            showOnlyTitles = showOnlyTitles,
                            onShowOnlyTitlesChanged = viewModel::onShowOnlyTitlesChanged,
                            biometricEnabled = biometricEnabled,
                            onBiometricEnabledChanged = { enabled ->
                                if (enabled) {
                                    // Verify biometrics before enabling
                                    val executor = ContextCompat.getMainExecutor(this@MainActivity)
                                    val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                                        object : BiometricPrompt.AuthenticationCallback() {
                                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                viewModel.onBiometricEnabledChanged(true)
                                                viewModel.unlock() // Set lastUnlockTime
                                            }
                                        })
                                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Enable Biometric Lock")
                                        .setSubtitle("Confirm your identity to enable biometric locking")
                                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                        .build()
                                    biometricPrompt.authenticate(promptInfo)
                                } else {
                                    // Verify biometrics before disabling
                                    val executor = ContextCompat.getMainExecutor(this@MainActivity)
                                    val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                                        object : BiometricPrompt.AuthenticationCallback() {
                                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                viewModel.onBiometricEnabledChanged(false)
                                            }
                                        })
                                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Disable Biometric Lock")
                                        .setSubtitle("Confirm your identity to disable biometric locking")
                                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                        .build()
                                    biometricPrompt.authenticate(promptInfo)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "editor/{noteId}?type={type}&notebookId={notebookId}",
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.LongType },
                            navArgument("type") { type = NavType.StringType; defaultValue = "NOTE" },
                            navArgument("notebookId") { type = NavType.LongType; defaultValue = -1L }
                        )
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
                        val typeStr = backStackEntry.arguments?.getString("type") ?: "NOTE"
                        val initialNotebookId = backStackEntry.arguments?.getLong("notebookId")?.takeIf { it != -1L }

                        NoteEditorScreen(
                            noteId = noteId,
                            noteTypeStr = typeStr,
                            initialNotebookId = initialNotebookId,
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
}

    private fun showBiometricPrompt(viewModel: NotesViewModel) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlock()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Notes")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}