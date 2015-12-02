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
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements View.OnClickListener {

    private DownloadFileFromURL mTask;
    private View mMainView;
    String mResult;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainView = inflater.inflate(R.layout.fragment_main, container, false);

        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setOnClickListener(this);
        setRetainInstance(true);
        return mMainView;
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this.getContext(),
                "Button pressed!",
                Toast.LENGTH_SHORT).show();

//        PowerManager pm = (PowerManager) this.getContext().getSystemService(this.getContext().POWER_SERVICE);
//        pm.reboot("Just");
        String urlS = "https://upload.wikimedia.org/wikipedia/commons/3/39/Lichtenstein_img_processing_test.png";
        startDownload(urlS);

    }

    private void startDownload(String urlS) {
        mTask = new DownloadFileFromURL(this);
        mTask.execute(urlS);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Sync UI state to current fragment and task state
        if(isTaskRunning(mTask)) {
            showProgressBar();
        }else {
            hideProgressBar();
        }
        //if(mResult!=null) {
        //    populateResult(mResult);
        //}
        super.onActivityCreated(savedInstanceState);
    }

    public void showProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
    }

    public void hideProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.GONE);

    }

    protected boolean isTaskRunning(DownloadFileFromURL task) {
        if(task==null ) {
            return false;
        } else if(task.getStatus() == DownloadFileFromURL.Status.FINISHED){
            return false;
        } else {
            return true;
        }
    }
}
