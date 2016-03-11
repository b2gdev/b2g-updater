package com.zone24x7.b2gdev.b2g_updater;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private SystemUpdateReceiver receiver;
    public static String availableUpdateVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.availableUpdateVersion = getIntent().getStringExtra("updateVersion");
        setContentView(R.layout.activity_main);

        Log.d("Main Activity","availableUpdateVersion "+availableUpdateVersion);

        FragmentManager fm = getSupportFragmentManager();
        UpdaterFragment updaterFragment = (UpdaterFragment) fm.findFragmentById(R.id.fragment);

        updaterFragment.setAvailableUpdateVersion(availableUpdateVersion);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        receiver = new SystemUpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.settings.SYSTEM_UPDATE_SETTINGS");
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

}
