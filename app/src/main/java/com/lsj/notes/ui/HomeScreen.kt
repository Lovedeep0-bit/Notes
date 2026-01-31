package com.lsj.notes.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lsj.notes.data.Note
import com.lsj.notes.data.NoteType
import com.lsj.notes.ui.theme.Black
import com.lsj.notes.data.Notebook
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    trashedNotes: List<Note>,
    notebooks: List<Notebook>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onAddNoteClick: (NoteType, Long?) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onRestoreNote: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit,
    onToggleTodo: (Note) -> Unit,
    onAddNotebook: (String) -> Unit,
    onDuplicateNote: (Note) -> Unit,
    onMoveNote: (Note, Long?) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRenameNotebook: (Notebook, String) -> Unit,
    onDeleteNotebook: (Notebook) -> Unit,
    onUpdateNote: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
    onSettingsClick: () -> Unit,
    currentSelectedView: String,
    onSelectedViewChanged: (String) -> Unit,
    showOnlyTitles: Boolean
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Local selectedView removed, using currentSelectedView
    var isSearchActive by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showNewNotebookDialog by remember { mutableStateOf(false) }
    var newNotebookName by remember { mutableStateOf("") }
    
    // Empty Trash Dialog
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    
    // Selection state
    var selectedNotes by remember { mutableStateOf(setOf<Long>()) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuNote by remember { mutableStateOf<Note?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    
    // Notebook context menu state
    var showNotebookContextMenu by remember { mutableStateOf(false) }
    var contextMenuNotebook by remember { mutableStateOf<Notebook?>(null) }
    var showRenameNotebookDialog by remember { mutableStateOf(false) }
    var renameNotebookName by remember { mutableStateOf("") }

    // Helper to get current notebook name

    // Filter notes based on selectedView
    val displayNotes = when {
        isSearchActive -> if (searchQuery.isBlank()) emptyList() else notes.filter { !it.isTrashed } // Search results, excluding trash
        currentSelectedView == "Notes" -> notes.filter { !it.isTrashed } // All notes (non-trashed)
        currentSelectedView == "Trash" -> trashedNotes
        currentSelectedView.startsWith("Notebook-") -> {
            val notebookId = currentSelectedView.removePrefix("Notebook-").toLongOrNull()
            notes.filter { !it.isTrashed && it.notebookId == notebookId }
        }
        else -> notes.filter { !it.isTrashed }
    }
    
    val currentNotebookName = if (currentSelectedView.startsWith("Notebook-")) {
        val id = currentSelectedView.removePrefix("Notebook-").toLongOrNull()
        notebooks.find { it.id == id }?.name
    } else null

    // New Notebook Dialog
    if (showNewNotebookDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNewNotebookDialog = false
                newNotebookName = ""
            },
            title = { Text("New Notebook") },
            text = {
                OutlinedTextField(
                    value = newNotebookName,
                    onValueChange = { newNotebookName = it },
                    label = { Text("Notebook name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newNotebookName.isNotBlank()) {
                            onAddNotebook(newNotebookName.trim())
                            showNewNotebookDialog = false
                            newNotebookName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showNewNotebookDialog = false
                        newNotebookName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Are you sure you want to permanently delete all notes in the trash? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEmptyTrash()
                        showEmptyTrashDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Context Menu Dialog
    if (showContextMenu && contextMenuNote != null) {
        val note = contextMenuNote!!
        AlertDialog(
            onDismissRequest = { 
                showContextMenu = false
                contextMenuNote = null
            },
            title = { Text(note.title.ifBlank { "Untitled" }) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            onTogglePin(note)
                            showContextMenu = false
                            contextMenuNote = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PushPin, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if (note.isPinned) "Unpin" else "Pin", Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            onDeleteNote(note)
                            showContextMenu = false
                            contextMenuNote = null
                            selectedNotes = selectedNotes - note.id
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Delete", Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            showContextMenu = false
                            showMoveDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DriveFileMove, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Move to notebook", Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            onDuplicateNote(note)
                            showContextMenu = false
                            contextMenuNote = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Duplicate", Modifier.weight(1f))
                    }
                    if (note.type == NoteType.TODO) {
                        TextButton(
                            onClick = {
                                onToggleTodo(note)
                                showContextMenu = false
                                contextMenuNote = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Done, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(if (note.isCompleted) "Mark as incomplete" else "Mark as completed", Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showContextMenu = false
                    contextMenuNote = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move to Notebook Dialog
    if (showMoveDialog && contextMenuNote != null) {
        val note = contextMenuNote!!
        AlertDialog(
            onDismissRequest = { 
                showMoveDialog = false
                contextMenuNote = null
            },
            title = { Text("Move to notebook") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            onMoveNote(note, null)
                            showMoveDialog = false
                            contextMenuNote = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("No notebook (All Notes)")
                    }
                    notebooks.forEach { notebook ->
                        TextButton(
                            onClick = {
                                onMoveNote(note, notebook.id)
                                showMoveDialog = false
                                contextMenuNote = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(notebook.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showMoveDialog = false
                    contextMenuNote = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Notebook Context Menu Dialog
    if (showNotebookContextMenu && contextMenuNotebook != null) {
        val notebook = contextMenuNotebook!!
        AlertDialog(
            onDismissRequest = { 
                showNotebookContextMenu = false
                contextMenuNotebook = null
            },
            title = { Text(notebook.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            renameNotebookName = notebook.name
                            showNotebookContextMenu = false
                            showRenameNotebookDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Edit", Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            onDeleteNotebook(notebook)
                            // If we're viewing this notebook, switch to All Notes
                            if (currentSelectedView == "Notebook-${notebook.id}") {
                                onSelectedViewChanged("Notes")
                            }
                            showNotebookContextMenu = false
                            contextMenuNotebook = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Delete", Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showNotebookContextMenu = false
                    contextMenuNotebook = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Notebook Dialog
    if (showRenameNotebookDialog && contextMenuNotebook != null) {
        val notebook = contextMenuNotebook!!
        AlertDialog(
            onDismissRequest = { 
                showRenameNotebookDialog = false
                contextMenuNotebook = null
                renameNotebookName = ""
            },
            title = { Text("Rename Notebook") },
            text = {
                OutlinedTextField(
                    value = renameNotebookName,
                    onValueChange = { renameNotebookName = it },
                    label = { Text("Notebook name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameNotebookName.isNotBlank()) {
                            onRenameNotebook(notebook, renameNotebookName.trim())
                            showRenameNotebookDialog = false
                            contextMenuNotebook = null
                            renameNotebookName = ""
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRenameNotebookDialog = false
                        contextMenuNotebook = null
                        renameNotebookName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (MaterialTheme.colorScheme.surface == Black) Black else DrawerDefaults.containerColor
            ) {
                Spacer(Modifier.height(12.dp))
                
                NavigationDrawerItem(
                    label = { Text("All Notes") },
                    icon = { Icon(Icons.Outlined.Description, null) },
                    selected = selectedItem(currentSelectedView, "Notes"),
                    onClick = {
                        onSelectedViewChanged("Notes")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Notebooks section header with add button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notebooks", style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        onClick = { showNewNotebookDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New notebook", modifier = Modifier.size(20.dp))
                    }
                }
                
                notebooks.forEach { notebook ->
                    NotebookDrawerItem(
                        notebook = notebook,
                        isSelected = currentSelectedView == "Notebook-${notebook.id}",
                        onClick = {
                            onSelectedViewChanged("Notebook-${notebook.id}")
                            scope.launch { drawerState.close() }
                        },
                        onLongClick = {
                            contextMenuNotebook = notebook
                            showNotebookContextMenu = true
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("Trash") },
                    icon = { Icon(Icons.Default.Delete, null) },
                    selected = currentSelectedView == "Trash",
                    onClick = {
                        onSelectedViewChanged("Trash")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, null) },
                    selected = false,
                    onClick = {
                        onSettingsClick()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Crossfade(
                    targetState = isSearchActive, 
                    label = "SearchAnimation",
                    animationSpec = tween(durationMillis = 300)
                ) { active ->
                    if (active) {
                        TopAppBar(
                            title = {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChanged,
                                    placeholder = { Text("Search...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = { 
                                    isSearchActive = false 
                                    onSearchQueryChanged("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            title = { 
                                Text(
                                    when (currentSelectedView) {
                                        "Notes" -> "All Notes"
                                        "Trash" -> "Trash"
                                        else -> currentNotebookName ?: "Notes"
                                    }
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                if (currentSelectedView != "Trash") {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                } else {
                                    // Trash Actions
                                    TextButton(onClick = { showEmptyTrashDialog = true }) {
                                        Text("Clear All")
                                    }
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentSelectedView != "Trash") {
                    FloatingActionButton(onClick = { 
                        val currentNotebookId = if (currentSelectedView.startsWith("Notebook-")) {
                            currentSelectedView.removePrefix("Notebook-").toLongOrNull()
                        } else {
                            null
                        }
                        onAddNoteClick(NoteType.NOTE, currentNotebookId) 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Note")
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (displayNotes.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No notes found", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    items(displayNotes, key = { it.id }) { note ->
                        NoteItem(
                            note = note,
                            onClick = { 
                                if (selectedNotes.isNotEmpty()) {
                                    // Toggle selection when in selection mode
                                    selectedNotes = if (note.id in selectedNotes) {
                                        selectedNotes - note.id
                                    } else {
                                        selectedNotes + note.id
                                    }
                                } else if (currentSelectedView != "Trash") {
                                    onNoteClick(note) 
                                }
                            },
                            onLongClick = {
                                if (currentSelectedView != "Trash") {
                                    contextMenuNote = note
                                    showContextMenu = true
                                }
                            },
                            isTrash = currentSelectedView == "Trash",
                            isSelected = note.id in selectedNotes,
                            onRestore = { onRestoreNote(note) },
                            onDeleteForever = { onDeleteForever(note) },
                            showOnlyTitles = showOnlyTitles,
                            onToggleChecklist = { lineIndex ->
                                val lines = note.content.lines().toMutableList()
                                if (lineIndex in lines.indices) {
                                    val line = lines[lineIndex]
                                    lines[lineIndex] = if (line.startsWith("[ ] ")) line.replaceFirst("[ ] ", "[x] ")
                                                       else line.replaceFirst("[x] ", "[ ] ")
                                    val newContent = lines.joinToString("\n")
                                    onUpdateNote(note.copy(content = newContent, updated = System.currentTimeMillis()))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun selectedItem(current: String, target: String): Boolean {
    return current == target
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isTrash: Boolean = false,
    isSelected: Boolean = false,
    onRestore: () -> Unit = {},
    onDeleteForever: () -> Unit = {},
    showOnlyTitles: Boolean = false,
    onToggleChecklist: (Int) -> Unit = {}
) {
    // Basic Markdown Parser for previews
    fun parseMarkdown(text: String): AnnotatedString {
        return buildAnnotatedString {
            val combinedPattern = Regex("(\\*\\*(.*?)\\*\\*)|(<u>(.*?)</u>)")
            var lastIndex = 0
            combinedPattern.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))
                
                val boldContent = match.groups[2]?.value
                val underlineContent = match.groups[4]?.value
                
                if (boldContent != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(parseMarkdown(boldContent))
                    }
                } else if (underlineContent != null) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(parseMarkdown(underlineContent))
                    }
                }
                lastIndex = match.range.last + 1
            }
            append(text.substring(lastIndex))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = true
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        border = if (MaterialTheme.colorScheme.surface == Black) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Title (only if not blank)
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Content Preview
                val allLines = note.content.lines().filter { it.isNotBlank() }
                if (allLines.isNotEmpty() && !showOnlyTitles) {
                    val maxPreviewLines = if (note.title.isBlank()) 5 else 3
                    var linesShown = 0
                    
                    note.content.lines().forEachIndexed { index, line ->
                        if (linesShown < maxPreviewLines && line.isNotBlank()) {
                            if (line.startsWith("[ ] ") || line.startsWith("[x] ")) {
                                val isChecked = line.startsWith("[x] ")
                                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { onToggleChecklist(index) },
                                            enabled = !isTrash,
                                            modifier = Modifier.padding(start = 0.dp).scale(0.7f).size(32.dp)
                                        )
                                        Text(
                                            text = parseMarkdown(line.substring(4)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                // Fix for bullets being inside formatting: replacement happens before markdown parsing
                                // Regex: Look for line start, optional whitespace, optional **, then - and space
                                val processedLine = line.replace(Regex("^(\\s*)(\\**)-\\s"), "$1$2• ")
                                
                                Text(
                                    text = parseMarkdown(processedLine),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            linesShown++
                        }
                    }
                }
            }
            
            // Pin indicator
            if (note.isPinned && !isTrash) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    modifier = Modifier.size(16.dp).rotate(45f),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Trash actions
            if (isTrash) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore")
                }
                IconButton(onClick = onDeleteForever) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete Forever")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotebookDrawerItem(
    notebook: Notebook,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = if (!isSelected && MaterialTheme.colorScheme.surface == Black) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = notebook.name,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
