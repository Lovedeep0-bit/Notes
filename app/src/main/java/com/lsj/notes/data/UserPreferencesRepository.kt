package com.lsj.notes.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val _selectedViewKey = "selected_view"
    private val _themeKey = "theme"
    private val _showOnlyTitlesKey = "show_only_titles"
    private val _biometricEnabledKey = "biometric_enabled"
    
    private val _selectedView = MutableStateFlow(prefs.getString(_selectedViewKey, "Notes") ?: "Notes")
    val selectedView: StateFlow<String> = _selectedView.asStateFlow()

    private val _theme = MutableStateFlow(prefs.getString(_themeKey, "Default") ?: "Default")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _showOnlyTitles = MutableStateFlow(prefs.getBoolean(_showOnlyTitlesKey, false))
    val showOnlyTitles: StateFlow<Boolean> = _showOnlyTitles.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean(_biometricEnabledKey, false))
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()
    
    fun saveSelectedView(view: String) {
        prefs.edit().putString(_selectedViewKey, view).apply()
        _selectedView.value = view
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString(_themeKey, theme).apply()
        _theme.value = theme
    }

    fun saveShowOnlyTitles(showOnlyTitles: Boolean) {
        prefs.edit().putBoolean(_showOnlyTitlesKey, showOnlyTitles).apply()
        _showOnlyTitles.value = showOnlyTitles
    }

    fun saveBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(_biometricEnabledKey, enabled).apply()
        _biometricEnabled.value = enabled
    }
}
