package com.zone24x7.b2gdev.b2g_updater;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class UpdaterFragment extends Fragment implements View.OnClickListener {

    private DownloadFileFromURL mDownloadOTATask;
    private CheckForUpdates mUpdateListTask;
    private View mMainView;
    private String mNewUpdate = null;

    private enum ButtonState {UPDATE,CANCEL,DOWNLOAD,APPLY};
    ButtonState btnState = ButtonState.UPDATE;

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
    public void onDestroyView() {
        super.onDestroyView();
        cancelTasks();
        mMainView = null;
    }

    @Override
    public void onClick(View v) {

//        Toast.makeText(this.getContext(), "Button pressed!", Toast.LENGTH_SHORT).show();

//        PowerManager pm = (PowerManager) this.getContext().getSystemService(this.getContext().POWER_SERVICE);
//        pm.reboot("Just");
//        startDownload(getString(R.string.url_ota_info_location));

        switch (btnState){
            case UPDATE:
                getUpdates();
                break;
            case CANCEL:
                cancelTasks();
                break;
            case DOWNLOAD:
                startDownload(getString(R.string.url_test_ota_location));
                break;
            default:
                break;
        }
    }

    private void cancelTasks() {
        if(isTaskRunning(mDownloadOTATask)){
            mDownloadOTATask.cancel(true);
            showDownloadBtn();
            TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
            statusLabel.setText(getText(R.string.str_update_available)+" "+ mNewUpdate);
        }
        if(isTaskRunning(mUpdateListTask)) {
            mUpdateListTask.cancel(true);
            showUpdateBtn();
            TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
            statusLabel.setText("");
        }
        hideProgressBar();
    }

    private void getUpdates() {
        showProgressBar();
        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
        statusLabel.setText(getText(R.string.str_fetching_updates));
        mUpdateListTask = new CheckForUpdates(this);
        mUpdateListTask.execute(getString(R.string.url_ota_info_location));
        showCancelBtn();
    }

    public void doneUpdates(String update) {

        boolean isUpdateAvailable = false;
        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);

        hideProgressBar();

        if(update != null) {
            if(isUpdateValid(update)) {
                isUpdateAvailable = true;
                mNewUpdate = update;
            }
        }

        if(isUpdateAvailable){
            statusLabel.setText(getText(R.string.str_update_available) + " " + mNewUpdate);
            showDownloadBtn();
        }else{
            statusLabel.setText(getText(R.string.str_update_not_available));
            showUpdateBtn();
        }
    }

    public void doneDownloadingOTA(String error) {

        hideProgressBar();

        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
        if(error != null){
            statusLabel.setText(getText(R.string.str_dl_failed));
            showDownloadBtn();
        }else{
            statusLabel.setText(getText(R.string.str_dl_done));
            showAppyUpdateBtn();
        }
    }

    private void showDownloadBtn() {
        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setText(getText(R.string.str_download));
        btnState = ButtonState.DOWNLOAD;
    }

    private void showAppyUpdateBtn() {
        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setText(getText(R.string.str_install));
        btnState = ButtonState.APPLY;
    }

    private boolean isUpdateValid(String update) {
        boolean retVal = false;
        if(update.length() == Build.ID.length()){
            if(update.compareTo(Build.ID) != 0){
                retVal = true;
            }
        }else if(update.compareTo("") != 0){
            retVal = true;
        };

        return retVal;
    }

    private void startDownload(String urlS) {

        showProgressBar();
        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
        statusLabel.setText(getText(R.string.str_dl_updates));
        mDownloadOTATask = new DownloadFileFromURL(this);
        mDownloadOTATask.execute(urlS);
        showCancelBtn();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Sync UI state to current fragment and task state
        if(isTaskRunning(mDownloadOTATask) || isTaskRunning(mUpdateListTask)) {
            showProgressBar();

        }else {
            hideProgressBar();
        }
        //if(mNewUpdate!=null) {
        //    populateResult(mNewUpdate);
        //}
        super.onActivityCreated(savedInstanceState);
    }


    private void showProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
    }

    private void showCancelBtn() {
        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setText(getText(R.string.str_cancel));
        btnState = ButtonState.CANCEL;
    }

    private void showUpdateBtn() {
        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setText(getText(R.string.str_update));
        btnState = ButtonState.UPDATE;
    }

    private void hideProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.INVISIBLE);
    }

    public void setProgress(int progressVal) {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setIndeterminate(false);
        progress.setMax(100);
        progress.setProgress(progressVal);
    }

    protected boolean isTaskRunning(AsyncTask<String, Integer, String> task) {
        if(task==null ) {
            return false;
        } else if(task.getStatus() == AsyncTask.Status.FINISHED){
            return false;
        } else {
            return true;
        }
    }
}
