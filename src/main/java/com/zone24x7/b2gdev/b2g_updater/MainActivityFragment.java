package com.zone24x7.b2gdev.b2g_updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements View.OnClickListener {

    private BroadcastReceiver receiver;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View  ret = inflater.inflate(R.layout.fragment_main, container, false);

        Button rebootBtn = (Button) ret.findViewById(R.id.button);
        rebootBtn.setOnClickListener(this);

        /*
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.settings.SYSTEM_UPDATE_SETTINGS")){
                    Toast.makeText(context,
                            "Yup! Received a system update broadcast",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.settings.SYSTEM_UPDATE_SETTINGS");
        this.getContext().registerReceiver(receiver, filter);
        */
        return ret;
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this.getContext(),
                "Button pressed!",
                Toast.LENGTH_SHORT).show();

        PowerManager pm = (PowerManager) this.getContext().getSystemService(this.getContext().POWER_SERVICE);
        pm.reboot("Just");
    }
}
