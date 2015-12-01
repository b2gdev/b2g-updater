package com.zone24x7.b2gdev.b2g_updater;

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
public class MainActivityFragment extends Fragment implements View.OnClickListener{

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View  ret = inflater.inflate(R.layout.fragment_main, container, false);

        Button rebootBtn = (Button) ret.findViewById(R.id.button);
        rebootBtn.setOnClickListener(this);

        return ret;
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this.getContext(),
                "Button pressed!",
                Toast.LENGTH_SHORT).show();
    }
}
