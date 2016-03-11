package com.zone24x7.b2gdev.b2g_updater;

import android.os.AsyncTask;
import android.os.RecoverySystem;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

public class UpdaterFragment extends Fragment implements View.OnClickListener {

    private static String TAG = "b2g-updater";
    private DownloadFileFromURL mDownloadOTATask;
    private CheckForUpdates mUpdateListTask;
    private View mMainView;
    private String mNewUpdateVersion = null;
    private String mNewUpdateUrl = null;
    private enum ButtonState {CHECK_FOR_UPDATES,CANCEL, DOWNLOAD_UPDATE, APPLY_UPDATE, UPDATE_AVAILABLE};
    ButtonState mButtonState;
    private String availableUpdateVersion;

    public UpdaterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMainView = inflater.inflate(R.layout.fragment_main, container, false);

        Button funcBtn = (Button) mMainView.findViewById(R.id.button);
        funcBtn.setOnClickListener(this);
        setRetainInstance(true);
        updateLastCheckedTime();
        this.availableUpdateVersion = MainActivity.availableUpdateVersion;
        if(this.availableUpdateVersion !=null && this.availableUpdateVersion !="" && this.availableUpdateVersion!="null"){
            Log.d(TAG,"Came inside update version available : "+this.availableUpdateVersion);
            setButton(ButtonState.UPDATE_AVAILABLE);
        } else {
            setButton(ButtonState.CHECK_FOR_UPDATES);
        }
        setStatus(getString(R.string.status_empty));
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

        v.setEnabled(false);
        switch (mButtonState){

            case CHECK_FOR_UPDATES:
                getUpdates();
                break;

            case CANCEL:
                cancelTasks();
                break;

            case DOWNLOAD_UPDATE:
                startDownload();
                break;

            case APPLY_UPDATE:
                installOTA();
                break;

            case UPDATE_AVAILABLE:
                doneUpdates(this.availableUpdateVersion);
                break;
            default:
                break;
        }
        v.setEnabled(true);
    }

    // Generate URL to git releases
    // Expected format https://github.com/b2gdev/Android-JB-4.1.2/releases/download/v4.2.5/update-v4.2.5.zip
    public String generateDownloadURL(String version) {

        return String.format("%s%s/update-%s.zip", getString(R.string.url_github_release), version,version);
    }

    // Getting updates
    private void getUpdates() {
        Log.d(TAG,"GetUpdates called");
        showProgressBar();
        setStatus(getString(R.string.status_fetching_updates));
        mUpdateListTask = new CheckForUpdates(this);
        mUpdateListTask.execute("");
        setButton(ButtonState.CANCEL);
    }

    // Done getting updates
    public void doneUpdates(String update) {
        Log.d(TAG,"Done updates called");
        Button funcBtn = (Button) mMainView.findViewById(R.id.button);
        funcBtn.setEnabled(false);
        hideProgressBar();
        updateLastCheckedTime();

        if(update != null) {
            mNewUpdateVersion = update;
            mNewUpdateUrl = generateDownloadURL(update);
            setUpdateAvailableStatus(update);
            setButton(ButtonState.DOWNLOAD_UPDATE);
        }else{
            setStatus(getString((R.string.status_update_not_available)));
            setButton(ButtonState.CHECK_FOR_UPDATES);
        }

        funcBtn.setEnabled(true);
    }

    // Getting OTA
    private void startDownload() {

        showProgressBar();
        setStatus(getString((R.string.status_dl_updates)));
        mDownloadOTATask = new DownloadFileFromURL(this);
        mDownloadOTATask.execute(mNewUpdateUrl);
//        mDownloadOTATask.execute(getString(R.string.url_test_ota)); // Alt test function
        setButton(ButtonState.CANCEL);
    }

    // Done getting OTA
    public void doneDownloadingOTA(String error) {

        Button funcBtn = (Button) mMainView.findViewById(R.id.button);
        funcBtn.setEnabled(false);
        hideProgressBar();

        if(error != null){
            setStatus(getString((R.string.status_dl_failed)));
            setButton(ButtonState.DOWNLOAD_UPDATE);
        }else{
            setStatus(getString((R.string.status_dl_done)));
            setButton(ButtonState.APPLY_UPDATE);
        }

        funcBtn.setEnabled(true);
    }

    // Install OTA
    private void installOTA() {

        setStatus(getString((R.string.status_installing)));

        try {
            File otaFile = new File(getString(R.string.path_download_target));

            // Verify the cryptographic signature before installing it.
            RecoverySystem.verifyPackage(otaFile, null, null);

            // Reboots the device into recovery mode to install the update package.
            RecoverySystem.installPackage(getActivity(), otaFile);
        } catch (IOException | GeneralSecurityException e) {
            Toast.makeText(getActivity(), "Install error: " + e.toString(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Install error: " + e.toString());
            setStatus(getString((R.string.status_install_failed)));
        }
    }

    // Cancel current task
    private void cancelTasks() {
        if(isTaskRunning(mDownloadOTATask)){
            mDownloadOTATask.cancel(true);
            setButton(ButtonState.DOWNLOAD_UPDATE);
            setUpdateAvailableStatus(mNewUpdateVersion);
        }
        if(isTaskRunning(mUpdateListTask)) {
            mUpdateListTask.cancel(true);
            setButton(ButtonState.CHECK_FOR_UPDATES);;
            setStatus(getString(R.string.status_empty));
        }
        hideProgressBar();
    }

    // Button toggle functions
    private void setButton(ButtonState state){
        Log.d(TAG,"came inside setButton method and state is : "+state);
        Button functionBtn = (Button) mMainView.findViewById(R.id.button);

        switch(state){
            case CHECK_FOR_UPDATES:
                functionBtn.setText(getText(R.string.btn_update));
                mButtonState = state;
                break;
            case DOWNLOAD_UPDATE:
                functionBtn.setText(getText(R.string.btn_download));
                mButtonState = state;
                break;
            case APPLY_UPDATE:
                functionBtn.setText(getText(R.string.btn_install));
                mButtonState = state;
                break;
            case CANCEL:
                functionBtn.setText(getText(R.string.btn_cancel));
                mButtonState = state;
                break;
            case UPDATE_AVAILABLE:
                mButtonState = state;
                Log.d(TAG,"came inside setButton method 2 and state is : "+state);
                functionBtn.setText("Update Available");
                break;
            default:
                break;
        }
    }

    // Status text label
    private void setStatus(String status){
        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
        statusLabel.setText(status);
    }

    private void setUpdateAvailableStatus(String newRelease){
        setStatus((String.format("%s : %s", getText(R.string.status_update_available), newRelease)));
    }

    // Progress bar control functions
    private void showProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
    }

    private void hideProgressBar() {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setVisibility(View.INVISIBLE);
    }

    public void setProgress(int progressPercentage) {
        ProgressBar progress = (ProgressBar)getActivity().findViewById(R.id.progressBarFetch);
        progress.setIndeterminate(false);
        progress.setMax(100);
        progress.setProgress(progressPercentage);
    }

    // Util functions
    private void updateLastCheckedTime(){
        TextView lastEditedLabel = (TextView) mMainView.findViewById(R.id.lastCheckValue);
        File file = new File(getString(R.string.path_update_info_xml));
        if(file.exists()){
            Date lastModified = new Date(file.lastModified());
            lastEditedLabel.setText(lastModified.toString());
        }
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Sync UI state to current fragment and task state
        Log.d(TAG,"came inside onActivityCreated method");
        updateLastCheckedTime();
        setButton(mButtonState);
        if(isTaskRunning(mDownloadOTATask)){
            showProgressBar();
            setStatus(getString(R.string.status_dl_updates));
        }else if (isTaskRunning(mUpdateListTask)) {
            showProgressBar();
            setStatus(getString(R.string.status_fetching_updates));
        }else {
            hideProgressBar();
            setStatus(getString(R.string.status_empty));
        }
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG,"Exited successfuly from onActivityCreated method");
    }

    public void setAvailableUpdateVersion(String availableUpdateVersion) {
        Log.d(TAG,"setAvailableUpdateVersion called with "+availableUpdateVersion);
        this.availableUpdateVersion = availableUpdateVersion;
    }
}
