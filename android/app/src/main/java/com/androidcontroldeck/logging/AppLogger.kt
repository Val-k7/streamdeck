package com.androidcontroldeck.logging

import android.util.Log
import com.androidcontroldeck.BuildConfig

/**
 * Logger utilitaire qui conditionne les logs avec BuildConfig.DEBUG
 * En production (release), les logs de debug sont désactivés
 */
object AppLogger {
    private const val DEFAULT_TAG = "ControlDeck"

    /**
     * Log debug - uniquement en mode debug
     */
    @JvmStatic
    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }

    /**
     * Log error - toujours actif (même en production pour les erreurs critiques)
     */
    @JvmStatic
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Log warning - toujours actif
     */
    @JvmStatic
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    /**
     * Log info - uniquement en mode debug
     */
    @JvmStatic
    fun i(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }

    /**
     * Log verbose - uniquement en mode debug
     */
    @JvmStatic
    fun v(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }
}


