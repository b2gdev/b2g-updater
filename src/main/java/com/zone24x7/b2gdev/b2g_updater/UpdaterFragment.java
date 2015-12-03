package com.zone24x7.b2gdev.b2g_updater;

import android.os.AsyncTask;
import android.os.Build;
import android.os.RecoverySystem;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class UpdaterFragment extends Fragment implements View.OnClickListener {

    private DownloadFileFromURL mDownloadOTATask;
    private CheckForUpdates mUpdateListTask;
    private View mMainView;
    private String mNewUpdateVersion = null;
    private String mNewUpdateUrl = null;

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
        updateLastCheckedTime();
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
                //startDownload(mNewUpdateUrl);
                startDownload(getString(R.string.url_test_ota_location_2));
                break;
            case APPLY:
                installOTA();
                break;
            default:
                break;
        }
    }

    private void installOTA() {

        Button rebootBtn = (Button) mMainView.findViewById(R.id.button);
        rebootBtn.setEnabled(false);
        TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
        statusLabel.setText(getText(R.string.str_installing));
        try {
            File otaFile = new File(getString(R.string.path_test_target));
            // Verify the cryptographic signature before installing it.
            RecoverySystem.verifyPackage(otaFile, null, null);
            // Reboots the device into recovery mode to install the update package.
            RecoverySystem.installPackage(getActivity(), otaFile);
        } catch (IOException | GeneralSecurityException e) {
            //e.printStackTrace();
            Toast.makeText(getActivity(), "Install error: " + e.toString(), Toast.LENGTH_LONG).show();
            statusLabel.setText(getText(R.string.str_install_failed));
        }
        rebootBtn.setEnabled(true);
    }

    private void cancelTasks() {
        if(isTaskRunning(mDownloadOTATask)){
            mDownloadOTATask.cancel(true);
            showDownloadBtn();
            TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
            statusLabel.setText(getText(R.string.str_update_available)+" "+ mNewUpdateVersion);
        }
        if(isTaskRunning(mUpdateListTask)) {
            mUpdateListTask.cancel(true);
            showUpdateBtn();
            TextView statusLabel = (TextView) mMainView.findViewById(R.id.statusTextView);
            statusLabel.setText("");
        }
        hideProgressBar();
    }

    private void updateLastCheckedTime(){
        TextView lastEditedLabel = (TextView) mMainView.findViewById(R.id.lastCheckValue);
        File file = new File(getString(R.string.path_save_updates));
        if(file.exists()){
            Date lastModified = new Date(file.lastModified());
            lastEditedLabel.setText(lastModified.toString());
        }
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
            updateLastCheckedTime();
            if(isUpdateValid(update)) {
                isUpdateAvailable = true;
                mNewUpdateVersion = update;
            }
        }

        if(isUpdateAvailable){
            statusLabel.setText(getText(R.string.str_update_available) + " " + mNewUpdateVersion);
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

        if(retVal){
            String tempURL = generateDownloadURL(update);
            if(!URLUtil.isValidUrl(tempURL)) {
                retVal = false;
                mNewUpdateUrl = null;
            }else
                mNewUpdateUrl = tempURL;
        }
        return retVal;
    }

    public String generateDownloadURL(String version) {
        return getString(R.string.url_github_release)+version+"/update-"+version+".zip";
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
        //if(mNewUpdateVersion!=null) {
        //    populateResult(mNewUpdateVersion);
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
