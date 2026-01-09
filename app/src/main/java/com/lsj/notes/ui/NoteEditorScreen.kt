package com.lsj.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lsj.notes.data.Note
import com.lsj.notes.data.NoteType
import com.lsj.notes.ui.theme.Black

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    noteTypeStr: String = "NOTE",
    initialNotebookId: Long? = null,
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    // Use TextFieldValue for content to handle cursor/selection
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var currentNote by remember { mutableStateOf<Note?>(null) }
    var noteType by remember { mutableStateOf(NoteType.NOTE) }
    
    val isNewNote = noteId == null || noteId == -1L
    
    // Default to Edit mode for new notes, Preview mode for existing notes
    var isEditMode by remember { mutableStateOf(isNewNote) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    // Auto-mode flags (Session-based)
    var isBulletModeActive by remember { mutableStateOf(false) }
    var isChecklistModeActive by remember { mutableStateOf(false) }
    
    // Initialize state
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != -1L) {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                currentNote = note
                title = note.title
                content = TextFieldValue(note.content)
                noteType = note.type
                // Ensure correct mode is set if state was lost (though remember handles config changes usually)
                if (!isNewNote) isEditMode = false 
            }
        } else {
             // Initialize generic type for new note from string arg
             noteType = try { NoteType.valueOf(noteTypeStr) } catch (e: Exception) { NoteType.NOTE }
        }
    }

    // Save function
    fun saveNote() {
        if (title.isBlank() && content.text.isBlank()) {
            currentNote?.let { viewModel.deleteNote(it) }
            return
        }
        
        // Auto-detect if this should be a TODO for sorting purposes
        val detectedType = if (content.text.contains("[ ]") || content.text.contains("[x]")) NoteType.TODO else NoteType.NOTE
        
        // Create the note with whatever title/content is present
        val newNote = if (currentNote != null) {
            currentNote!!.copy(
                title = title, 
                content = content.text, 
                type = detectedType,
                updated = System.currentTimeMillis()
            )
        } else {
            Note(
                title = title, 
                content = content.text, 
                type = detectedType, 
                notebookId = initialNotebookId
            )
        }
        
        if (currentNote != null) {
            viewModel.updateNote(newNote)
        } else {
            viewModel.addNote(newNote)
        }
    }

    // Auto-save on system back press
    BackHandler {
        saveNote()
        onBack()
    }

    // Formatting helpers
    fun toggleBold() {
        val text = content.text
        val selection = content.selection
        
        if (selection.collapsed) {
             // Insert **** and move cursor to middle
             val newText = text.substring(0, selection.start) + "****" + text.substring(selection.end)
             content = content.copy(
                 text = newText,
                 selection = TextRange(selection.start + 2)
             )
        } else {
             // Wrap existing selection
             val newText = text.substring(0, selection.start) + "**" + 
                           text.substring(selection.start, selection.end) + "**" + 
                           text.substring(selection.end)
             content = content.copy(
                 text = newText,
                 selection = TextRange(selection.end + 4)
             )
        }
    }

    fun toggleUnderline() {
        val text = content.text
        val selection = content.selection
        
        if (selection.collapsed) {
             // Insert <u></u> and move cursor to middle
             val newText = text.substring(0, selection.start) + "<u></u>" + text.substring(selection.end)
             content = content.copy(
                 text = newText,
                 selection = TextRange(selection.start + 3)
             )
        } else {
             // Wrap existing selection
             val newText = text.substring(0, selection.start) + "<u>" + 
                           text.substring(selection.start, selection.end) + "</u>" + 
                           text.substring(selection.end)
             content = content.copy(
                 text = newText,
                 selection = TextRange(selection.end + 7)
             )
        }
    }

    fun addBulletPoint() {
        val text = content.text
        val selection = content.selection
        
        isBulletModeActive = true
        isChecklistModeActive = false
        
        // Simple append for now: Insert newline + bullet
        val prefix = if (text.isNotEmpty() && !text.endsWith("\n") && selection.start > 0 && text[selection.start-1] != '\n') "\n- " else "- "
        val newText = text.substring(0, selection.start) + prefix + text.substring(selection.end)
        content = content.copy(
            text = newText,
            selection = TextRange(selection.start + prefix.length)
        )
    }

    fun addChecklist() {
        val text = content.text
        val selection = content.selection
        
        isChecklistModeActive = true
        isBulletModeActive = false
        
        // Insert [ ] at start of line or cursor
        val prefix = if (text.isNotEmpty() && !text.endsWith("\n") && selection.start > 0 && text[selection.start-1] != '\n') "\n[ ] " else "[ ] "
        val newText = text.substring(0, selection.start) + prefix + text.substring(selection.end)
        content = content.copy(
            text = newText,
            selection = TextRange(selection.start + prefix.length)
        )
    }

    // Markdown Parser (Basic support for **bold** and <u>underline</u>)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Formatting Options (Visible only in Edit Mode)
                    if (isEditMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { addChecklist() }) {
                                Icon(
                                    imageVector = Icons.Default.CheckBox, 
                                    contentDescription = "To-do",
                                    tint = LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { toggleBold() }) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                            }
                            IconButton(onClick = { toggleUnderline() }) {
                                Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                            }
                            IconButton(onClick = { addBulletPoint() }) {
                                Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet Points")
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        saveNote()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Save/Options button first (Visible only in Edit Mode)

                    
                    Spacer(Modifier.width(8.dp))
                    
                    // Toggle buttons on the right (Always visible)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .then(
                                if (MaterialTheme.colorScheme.surface == Black) {
                                    Modifier.border(1.dp, Color.White, RoundedCornerShape(24.dp))
                                } else Modifier
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { isEditMode = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(
                            onClick = { isEditMode = false },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (!isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "Preview")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isEditMode) {
                // Edit mode
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                TextField(
                    value = content,
                    onValueChange = { newValue -> 
                        val oldText = content.text
                        val newText = newValue.text
                        
                        // Detect newline for auto-bullets/checklist
                        if (newText.length == oldText.length + 1 && 
                            newText.getOrNull(newValue.selection.start - 1) == '\n' &&
                            newValue.selection.collapsed
                        ) {
                            val cursorPosition = newValue.selection.start
                            val textBeforeNewline = newText.substring(0, cursorPosition - 1)
                            val lastLine = textBeforeNewline.substringAfterLast('\n', textBeforeNewline)
                            
                            if (isBulletModeActive && lastLine.startsWith("- ")) {
                                if (lastLine.trim() == "-") {
                                    // User pressed enter on an empty bullet -> stop bullet mode and remove empty bullet
                                    val updatedText = newText.substring(0, cursorPosition - lastLine.length - 1) + newText.substring(cursorPosition)
                                    content = newValue.copy(
                                        text = updatedText,
                                        selection = TextRange(cursorPosition - lastLine.length - 1)
                                    )
                                    isBulletModeActive = false
                                } else {
                                    // Auto-insert next bullet
                                    val updatedText = newText.substring(0, cursorPosition) + "- " + newText.substring(cursorPosition)
                                    content = newValue.copy(
                                        text = updatedText,
                                        selection = TextRange(cursorPosition + 2)
                                    )
                                }
                            } else if (isChecklistModeActive && lastLine.startsWith("[ ] ")) {
                                if (lastLine.trim() == "[ ]") {
                                    // User pressed enter on an empty checkbox -> stop checklist mode and remove empty checkbox
                                    val updatedText = newText.substring(0, cursorPosition - lastLine.length - 1) + newText.substring(cursorPosition)
                                    content = newValue.copy(
                                        text = updatedText,
                                        selection = TextRange(cursorPosition - lastLine.length - 1)
                                    )
                                    isChecklistModeActive = false
                                } else {
                                    // Auto-insert next checkbox
                                    val updatedText = newText.substring(0, cursorPosition) + "[ ] " + newText.substring(cursorPosition)
                                    content = newValue.copy(
                                        text = updatedText,
                                        selection = TextRange(cursorPosition + 4)
                                    )
                                }
                            } else {
                                content = newValue
                            }
                        } else {
                            // Detect if bullet/checklist prefix was deleted manually
                            if (newText.length < oldText.length) {
                                val cursorPosition = newValue.selection.start
                                val textBeforeCursor = newText.substring(0, cursorPosition)
                                val currentLine = textBeforeCursor.substringAfterLast('\n', textBeforeCursor)
                                
                                if (isBulletModeActive && !currentLine.startsWith("-") && !newText.contains("- ")) {
                                    isBulletModeActive = false
                                }
                                if (isChecklistModeActive && !currentLine.startsWith("[") && !newText.contains("[ ] ")) {
                                    isChecklistModeActive = false
                                }
                            }
                            content = newValue
                        }
                    },
                    placeholder = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    maxLines = Int.MAX_VALUE,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            } else {
                // Preview mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    content.text.lines().forEachIndexed { index, line ->
                        if (line.startsWith("[ ] ") || line.startsWith("[x] ")) {
                            val isChecked = line.startsWith("[x] ")
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val lines = content.text.lines().toMutableList()
                                            lines[index] = if (checked) line.replaceFirst("[ ] ", "[x] ") 
                                                            else line.replaceFirst("[x] ", "[ ] ")
                                            content = content.copy(text = lines.joinToString("\n"))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = parseMarkdown(line.substring(4)),
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = parseMarkdown(line),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
