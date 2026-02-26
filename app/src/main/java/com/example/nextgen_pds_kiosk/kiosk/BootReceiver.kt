package com.example.nextgen_pds_kiosk.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BootReceiver
 *
 * Automatically relaunches the kiosk app when the device boots.
 * This ensures the kiosk is always running after a power cycle —
 * no manual intervention needed in the field.
 *
 * Requires RECEIVE_BOOT_COMPLETED permission in AndroidManifest.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("KioskBoot", "Boot complete — launching kiosk app")
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

            if (launchIntent != null) {
                context.startActivity(launchIntent)
                Log.i("KioskBoot", "Kiosk app started from boot")
            } else {
                Log.e("KioskBoot", "No launch intent found for ${context.packageName}")
            }
        }
    }
}
