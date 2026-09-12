package com.example.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log

/**
 * Helper to manage Android Telecom Default Dialer qualification and role requests.
 *
 * Supports RoleManager (Android 10+ / API 29+) and legacy TelecomManager
 * fallback (Android 9 and below).
 */
object DefaultDialerManager {

    private const val TAG = "DefaultDialerManager"

    /**
     * Checks if Elyzareth is currently set as the system default Phone/Dialer app.
     */
    fun isDefaultDialer(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            } else {
                @Suppress("DEPRECATION")
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                telecomManager?.defaultDialerPackage == context.packageName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default dialer status", e)
            false
        }
    }

    /**
     * Checks if the dialer role can be requested on this device.
     */
    fun isRoleAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true
        } else {
            true
        }
    }

    /**
     * Creates an Intent to prompt the user to set Elyzareth as the default dialer.
     *
     * - On API 29+: Uses RoleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
     * - On API 24-28: Uses TelecomManager.ACTION_CHANGE_DEFAULT_DIALER
     */
    fun createRequestRoleIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                } else {
                    createDefaultAppsSettingsIntent()
                }
            } else {
                @Suppress("DEPRECATION")
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create role request intent", e)
            createDefaultAppsSettingsIntent()
        }
    }

    /**
     * Fallback intent to navigate the user directly to Default Apps in Android Settings.
     */
    fun createDefaultAppsSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }
}
