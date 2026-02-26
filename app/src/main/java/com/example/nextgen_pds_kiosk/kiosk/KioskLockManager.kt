package com.example.nextgen_pds_kiosk.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KioskLockManager — Phase K1
 *
 * Manages Android Lock Task Mode (screen pinning at the OS level).
 * When locked:
 *  - Back button → no-op
 *  - Home button → blocked
 *  - Recents button → blocked
 *  - Status bar → hidden/collapsed
 *  - Notifications → blocked
 *
 * Requires Device Owner status for seamless lock (no system confirmation dialog).
 * Falls back gracefully if not a Device Owner.
 */
@Singleton
class KioskLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, KioskDeviceAdminReceiver::class.java)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    /** Whether this app currently holds Device Owner status. */
    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    /**
     * Starts Lock Task Mode from an Activity context.
     * If Device Owner, sets lock task packages first for seamless lock.
     * If not Device Owner, Android will show a system "Screen Pinning" dialog instead.
     */
    fun startLockTask(activity: Activity) {
        try {
            if (isDeviceOwner) {
                // Whitelist our package so lock task starts silently
                dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
                Log.i("KioskLock", "Lock task packages whitelisted (Device Owner)")
            } else {
                Log.w("KioskLock", "Not Device Owner — lock task will show system dialog")
            }
            activity.startLockTask()
            _isLocked.value = true
            Log.i("KioskLock", "Lock Task Mode STARTED")
        } catch (e: Exception) {
            Log.e("KioskLock", "Failed to start lock task: ${e.message}")
        }
    }

    /**
     * Exits Lock Task Mode. Should only be called from AdminLoginScreen
     * after successful PIN verification.
     */
    fun stopLockTask(activity: Activity) {
        try {
            activity.stopLockTask()
            _isLocked.value = false
            Log.i("KioskLock", "Lock Task Mode STOPPED (Admin unlocked)")
        } catch (e: Exception) {
            Log.e("KioskLock", "Failed to stop lock task: ${e.message}")
        }
    }

    /**
     * Returns true if Lock Task Mode is currently active at the OS level.
     */
    fun isLockTaskActive(activity: Activity): Boolean {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    /**
     * Disables keyguard (lock screen) so the Kiosk app shows
     * immediately after boot without requiring a PIN/pattern.
     * Only works when Device Owner.
     */
    fun disableKeyguard() {
        if (isDeviceOwner) {
            try {
                dpm.setKeyguardDisabled(adminComponent, true)
                Log.i("KioskLock", "Keyguard disabled — no lock screen on boot")
            } catch (e: Exception) {
                Log.e("KioskLock", "Failed to disable keyguard: ${e.message}")
            }
        }
    }

    /**
     * Re-enables keyguard (for maintenance/admin use).
     */
    fun enableKeyguard() {
        if (isDeviceOwner) {
            try {
                dpm.setKeyguardDisabled(adminComponent, false)
                Log.i("KioskLock", "Keyguard re-enabled")
            } catch (e: Exception) {
                Log.e("KioskLock", "Failed to enable keyguard: ${e.message}")
            }
        }
    }
}
