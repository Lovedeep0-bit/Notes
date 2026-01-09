package com.lsj.notes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lsj.notes.ui.theme.Black

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChanged: (String) -> Unit,
    showOnlyTitles: Boolean,
    onShowOnlyTitlesChanged: (Boolean) -> Unit,
    biometricEnabled: Boolean,
    onBiometricEnabledChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val themes = listOf("Default", "Light", "OLED")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = when(currentTheme) {
                        "OLED" -> "OLED (Pure Black)"
                        "Default" -> "Default"
                        else -> currentTheme
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Theme") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    themes.forEach { theme ->
                        DropdownMenuItem(
                            text = { 
                                Text(when(theme) {
                                    "OLED" -> "OLED (Pure Black)"
                                    "Default" -> "Default"
                                    else -> theme
                                }) 
                            },
                            onClick = {
                                onThemeChanged(theme)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Show Only Titles Setting in a consistent bordered box
            Surface(
                onClick = { onShowOnlyTitlesChanged(!showOnlyTitles) },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == Black) Color.White else MaterialTheme.colorScheme.outline),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Show Only Titles", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Hide note content in the main list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showOnlyTitles,
                        onCheckedChange = onShowOnlyTitlesChanged
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Biometric Auth Setting
            Surface(
                onClick = { onBiometricEnabledChanged(!biometricEnabled) },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == Black) Color.White else MaterialTheme.colorScheme.outline),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Biometric Lock", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Require fingerprint to access app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onBiometricEnabledChanged
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val uriHandler = LocalUriHandler.current
            OutlinedButton(
                onClick = { uriHandler.openUri("https://github.com/Lovedeep0-bit/Notes") },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, if (MaterialTheme.colorScheme.surface == Black) Color.White else MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Consistent height with TextField
            ) {
                Icon(Icons.Filled.Code, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View on GitHub")
            }

            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
