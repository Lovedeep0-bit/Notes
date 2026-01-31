package com.lsj.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.widget.Toast
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity

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

    // Markdown Parser (Basic support for **bold**, <u>underline</u>, and URLs)
    fun parseMarkdown(text: String): AnnotatedString {
        return buildAnnotatedString {
            val combinedPattern = Regex("(\\*\\*(.*?)\\*\\*)|(<u>(.*?)</u>)|((https?://\\S+))")
            var lastIndex = 0
            combinedPattern.findAll(text).forEach { match ->
                append(text.substring(lastIndex, match.range.first))
                
                val boldContent = match.groups[2]?.value
                val underlineContent = match.groups[4]?.value
                val urlContent = match.groups[6]?.value
                
                if (boldContent != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(parseMarkdown(boldContent))
                    }
                } else if (underlineContent != null) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(parseMarkdown(underlineContent))
                    }
                } else if (urlContent != null) {
                    pushStringAnnotation(tag = "URL", annotation = urlContent)
                    withStyle(SpanStyle(color = Color(0xFF64B5F6), textDecoration = TextDecoration.Underline)) {
                        append(urlContent)
                    }
                    pop()
                }
                lastIndex = match.range.last + 1
            }
            append(text.substring(lastIndex))
        }
    }

    @Composable
    fun LinkText(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
        textDecoration: TextDecoration? = null
    ) {
        val uriHandler = LocalUriHandler.current
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

        Text(
            text = text,
            modifier = modifier.pointerInput(Unit) {
                detectTapGestures(
                    onTap = { pos ->
                        layoutResult?.let { layoutResult ->
                            val offset = layoutResult.getOffsetForPosition(pos)
                            text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    },
                    onLongPress = { pos ->
                        layoutResult?.let { layoutResult ->
                            val offset = layoutResult.getOffsetForPosition(pos)
                            text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    clipboardManager.setText(AnnotatedString(annotation.item))
                                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                )
            },
            style = style,
            textDecoration = textDecoration,
            onTextLayout = { layoutResult = it }
        )
    }

    @Composable
    fun CustomTextToolbar(content: @Composable () -> Unit) {
        var menuRect by remember { mutableStateOf<Rect?>(null) }
        var onCopy by remember { mutableStateOf<(() -> Unit)?>(null) }
        var onPaste by remember { mutableStateOf<(() -> Unit)?>(null) }
        var onCut by remember { mutableStateOf<(() -> Unit)?>(null) }
        var onSelectAll by remember { mutableStateOf<(() -> Unit)?>(null) }
        var isShown by remember { mutableStateOf(false) }

        val toolbar = remember {
            object : TextToolbar {
                override val status: TextToolbarStatus
                    get() = if (isShown) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

                override fun showMenu(
                    rect: Rect,
                    onCopyRequested: (() -> Unit)?,
                    onPasteRequested: (() -> Unit)?,
                    onCutRequested: (() -> Unit)?,
                    onSelectAllRequested: (() -> Unit)?
                ) {
                    menuRect = rect
                    onCopy = onCopyRequested
                    onPaste = onPasteRequested
                    onCut = onCutRequested
                    onSelectAll = onSelectAllRequested
                    isShown = true
                }

                override fun hide() {
                    isShown = false
                }
            }
        }

        CompositionLocalProvider(LocalTextToolbar provides toolbar) {
            Box {
                content()
                if (isShown && menuRect != null) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = with(LocalDensity.current) {
                            IntOffset(
                                x = menuRect!!.center.x.toInt(),
                                y = (menuRect!!.top - 50.dp.toPx()).toInt()
                            )
                        },
                        onDismissRequest = { isShown = false }
                    ) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            val items = listOfNotNull(
                                if (onCut != null) "Cut" to onCut else null,
                                if (onCopy != null) "Copy" to onCopy else null,
                                if (onPaste != null) "Paste" to onPaste else null,
                                if (onSelectAll != null) "Select All" to onSelectAll else null
                            )
                            
                            items.forEach { (label, action) ->
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clickable { 
                                            action?.invoke() 
                                            isShown = false 
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CustomTextToolbar {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.union(WindowInsets.ime),
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
                            
                            if (isBulletModeActive && (lastLine.trimStart().startsWith("- ") || lastLine.trimStart().startsWith("• "))) {
                                if (lastLine.trim() == "-" || lastLine.trim() == "•") {
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
                                
                                if (isBulletModeActive && !currentLine.trimStart().startsWith("-") && !newText.contains("- ")) {
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
                // Preview mode, hiding the toolbar handled by CustomTextToolbar
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()) // Enable scrolling for long notes
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
                                    LinkText(
                                        text = parseMarkdown(line.substring(4)),
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        } else {
                            // Fix for bullets being inside formatting: replacement happens before markdown parsing
                            // We replace "- " at start (or indented) with bullet, even if wrapped in **.
                            // Regex: Look for line start, optional whitespace, optional **, then - and space
                            val processedLine = line.replace(Regex("^(\\s*)(\\**)-\\s"), "$1$2• ")
                            
                            LinkText(
                                text = parseMarkdown(processedLine),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
