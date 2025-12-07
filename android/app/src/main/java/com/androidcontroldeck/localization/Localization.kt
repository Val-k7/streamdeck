package com.androidcontroldeck.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

const val SYSTEM_LANGUAGE_TAG = "system"
private val supportedLanguageTags = setOf("en", "fr")
val allLanguageTags: Set<String> = supportedLanguageTags + SYSTEM_LANGUAGE_TAG

fun defaultLanguageTag(): String =
    Locale.getDefault().language.takeIf { it in supportedLanguageTags } ?: SYSTEM_LANGUAGE_TAG

fun applyAppLocale(languageTag: String) {
    val localeList = if (languageTag == SYSTEM_LANGUAGE_TAG) {
        LocaleListCompat.getAdjustedDefault()
    } else {
        LocaleListCompat.forLanguageTags(languageTag)
    }
    AppCompatDelegate.setApplicationLocales(localeList)
}

fun getAvailableLanguages(): Map<String, String> {
    return mapOf(
        "system" to "Follow system",
        "en" to "English",
        "fr" to "French"
    )
}