package com.zone24x7.b2gdev.b2g_updater;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class UpdaterFragment extends Fragment implements View.OnClickListener {

    private DownloadFileFromURL mDownloadOTATask;
    private CheckForUpdates mUpdateListTask;
    private View mMainView;
    String mResult;

    public UpdaterFragment() {
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

//        Toast.makeText(this.getContext(), "Button pressed!", Toast.LENGTH_SHORT).show();

//        PowerManager pm = (PowerManager) this.getContext().getSystemService(this.getContext().POWER_SERVICE);
//        pm.reboot("Just");
//        startDownload(getString(R.string.url_ota_info_location));
        getUpdates(getString(R.string.url_ota_info_location));
    }

    private void getUpdates(String urlS) {
        showProgressBar();
        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
        statusLabel.setText(getText(R.string.str_fetching_updates));
        mUpdateListTask = new CheckForUpdates(this);
        mUpdateListTask.execute(urlS);
    }

    public void doneUpdates(String update) {
        hideProgressBar();
        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
        if(update != null) {
            statusLabel.setText(getText(R.string.str_update_available)+" "+update);
        }else
            statusLabel.setText(getText(R.string.str_update_not_available));
    }

    private void startDownload(String urlS) {
        mDownloadOTATask = new DownloadFileFromURL(this);
        mDownloadOTATask.execute(urlS);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Sync UI state to current fragment and task state
        if(isTaskRunning(mDownloadOTATask)) {
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
        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setEnabled(false);
    }

    public void hideProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.INVISIBLE);
        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setEnabled(true);
    }

    public void setProgress(int progressVal) {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setIndeterminate(false);
        progress.setMax(100);
        progress.setProgress(progressVal);
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
