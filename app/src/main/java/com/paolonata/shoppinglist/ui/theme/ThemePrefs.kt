package com.paolonata.shoppinglist.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { AUTO, LIGHT, DARK }

/**
 * Sceglie la modalità del tema dell'app (auto/chiaro/scuro), persistita in
 * SharedPreferences. Espone un [StateFlow] per permettere alla UI Compose di
 * reagire al cambiamento senza dover riavviare l'Activity.
 */
object ThemePrefs {
    private const val PREFS = "theme_prefs"
    private const val KEY = "theme_mode"

    private val _mode = MutableStateFlow(ThemeMode.AUTO)
    val mode: StateFlow<ThemeMode> = _mode

    fun init(context: Context) {
        _mode.value = read(context)
    }

    fun set(context: Context, mode: ThemeMode) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, mode.name)
            .apply()
        _mode.value = mode
    }

    private fun read(context: Context): ThemeMode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, ThemeMode.AUTO.name)
            ?: ThemeMode.AUTO.name
        return runCatching { ThemeMode.valueOf(raw) }.getOrElse { ThemeMode.AUTO }
    }
}
