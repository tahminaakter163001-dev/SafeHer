
package com.example.safeher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

public class SystemEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {

            boolean airplaneModeOn =
                    Settings.Global.getInt(
                            context.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON,
                            0
                    ) == 1;

            if (airplaneModeOn) {
                Toast.makeText(
                        context,
                        "Airplane Mode is ON. Emergency connectivity may be unavailable.",
                        Toast.LENGTH_LONG
                ).show();
            } else {
                Toast.makeText(
                        context,
                        "Airplane Mode is OFF. Connectivity is available.",
                        Toast.LENGTH_LONG
                ).show();
            }

        } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {

            Toast.makeText(
                    context,
                    "Battery is low! Please charge your phone to keep SafeHer available.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
