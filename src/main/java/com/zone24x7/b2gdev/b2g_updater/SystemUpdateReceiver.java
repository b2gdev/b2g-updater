package com.zone24x7.b2gdev.b2g_updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class SystemUpdateReceiver extends BroadcastReceiver {
    public SystemUpdateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.settings.SYSTEM_UPDATE_SETTINGS")){
            Toast.makeText(context,
                    "Yup! Received a system update broadcast",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
