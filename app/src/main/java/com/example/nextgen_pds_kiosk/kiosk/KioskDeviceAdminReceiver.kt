package com.example.nextgen_pds_kiosk.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * KioskDeviceAdminReceiver
 *
 * This receiver is the anchor for Device Owner (DO) mode.
 * Once the app is set as Device Owner via ADB, it gains authority
 * to call startLockTask() / stopLockTask() programmatically without
 * requiring per-session user confirmation.
 *
 * ADB Provisioning (one-time, on factory-reset device):
 *   adb shell dpm set-device-owner \
 *     com.example.nextgen_pds_kiosk/.kiosk.KioskDeviceAdminReceiver
 */
class KioskDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("KioskAdmin", "Device Admin ENABLED — kiosk authority granted")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("KioskAdmin", "Device Admin DISABLED — lock task authority lost")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.i("KioskAdmin", "Lock Task Mode ENTERING: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.i("KioskAdmin", "Lock Task Mode EXITING")
    }
}
