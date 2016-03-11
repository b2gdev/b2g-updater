package com.zone24x7.b2gdev.b2g_updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by root on 3/9/16.
 */
public class UpdateCheckerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent mServiceIntent = new Intent();
        mServiceIntent.setAction("com.zone24x7.b2gdev.b2g_updater.UpdateCheckerService");
        context.startService(mServiceIntent);
    }
}
