package com.zone24x7.b2gdev.b2g_updater;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
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

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
        ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobileData = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        Button funcBtn = (Button) mMainView.findViewById(R.id.button);
        funcBtn.setEnabled(false);
        hideProgressBar();
        updateLastCheckedTime();

        if(!mWifi.isConnected() && !mMobileData.isConnected()) {
            setStatus(getString(R.string.status_not_connected));
            setButton(ButtonState.CHECK_FOR_UPDATES);
        } else {

            String globalURL = getGlobalUpdatePath(new File(getActivity().getString(R.string.path_update_info_xml)));
            if (globalURL.isEmpty()) {
                if (update != null) {
                    mNewUpdateUrl = generateDownloadURL(update);
                    mNewUpdateVersion = update;

                    setUpdateAvailableStatus(update);
                    setButton(ButtonState.DOWNLOAD_UPDATE);
                } else {
                    setStatus(getString((R.string.status_update_not_available)));
                    setButton(ButtonState.CHECK_FOR_UPDATES);
                }
            } else {

                mNewUpdateUrl = globalURL;
                mNewUpdateVersion = update;
                setUpdateAvailableStatus(update);
                setButton(ButtonState.DOWNLOAD_UPDATE);
            }

//        if(update != null) {
//            mNewUpdateVersion = update;
//            mNewUpdateUrl = generateDownloadURL(update);
//            setUpdateAvailableStatus(update);
//            setButton(ButtonState.DOWNLOAD_UPDATE);
//        }else{
//
//            ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
//            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//            NetworkInfo mMobileData = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//
//            if(!mWifi.isConnected() && !mMobileData.isConnected()) {
//                setStatus(getString(R.string.status_not_connected));
//                setButton(ButtonState.CHECK_FOR_UPDATES);
//            }
//            else {
//                setStatus(getString((R.string.status_update_not_available)));
//                setButton(ButtonState.CHECK_FOR_UPDATES);
//            }
//        }

            funcBtn.setEnabled(true);
        }

    }

    //Get the global update path for the current version
    private String getGlobalUpdatePath(File file) {

            String updateIDURL="";
            String versionNumber="";

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder documentBuilder = null;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            document.getDocumentElement().normalize();

            NodeList versionInfoNodes = document.getElementsByTagName("version");

            for (int versionIdCount = 0; versionIdCount < versionInfoNodes.getLength(); versionIdCount++) {
                if (versionInfoNodes.item(versionIdCount).getNodeType() == Node.ELEMENT_NODE) {
                    versionNumber = versionInfoNodes.item(versionIdCount).getAttributes().getNamedItem("id").getNodeValue();
                    if(versionNumber.equals(Build.ID)) {
                        updateIDURL = versionInfoNodes.item(versionIdCount).getAttributes().getNamedItem("updateIDURL").getNodeValue();
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            Log.e(TAG, "ParserConfiguration exception occurred while retrieving global update path", e);
        } catch (SAXException e) {
            Log.e(TAG, "SAXException occured while retrieving global update path",e);
        } catch (IOException e) {
            Log.e(TAG, "IOException occurred while retrieving global update path",e);
        }

            return updateIDURL;
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
            if(validateCheckSum(new File(getString(R.string.path_update_info_xml)))) {
                Toast.makeText(getActivity(),"Update downloaded", Toast.LENGTH_SHORT).show();
                setStatus(getString((R.string.status_dl_done)));
                setButton(ButtonState.APPLY_UPDATE);
            } else {
                Log.e(TAG,"Checksum validation failed");
                Toast.makeText(getActivity(),"Download error: checksum failed", Toast.LENGTH_SHORT).show();
                setStatus(getString((R.string.status_dl_failed)));
                setButton(ButtonState.DOWNLOAD_UPDATE);
            }

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

    //validate the checksum of the update zip file
    private boolean validateCheckSum(File fileName){
        boolean retVal = false;
        String checksumVal = "";
        String versionId="";

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(fileName);
            document.getDocumentElement().normalize();

            NodeList versionInfoNodes = document.getElementsByTagName("version");

            for (int versionIdCount = 0; versionIdCount < versionInfoNodes.getLength(); versionIdCount++) {
                if (versionInfoNodes.item(versionIdCount).getNodeType() == Node.ELEMENT_NODE) {
                    versionId = versionInfoNodes.item(versionIdCount).getAttributes().getNamedItem("id").getNodeValue();
                    checksumVal = versionInfoNodes.item(versionIdCount).getAttributes().getNamedItem("checksum")
                            .getNodeValue();
                    if(versionId.equals(Build.ID)) {
                        if (getMD5String(createChecksumValue(getString(R.string.path_download_target))).equals(checksumVal)) {
                            retVal = true;
                        } else {
                            retVal=false;
                        }
                    }

                }
            }
        } catch (ParserConfigurationException e) {
            Log.e(TAG,"ParserConfiguration exception occurred while validating checksum", e);
        } catch (SAXException e) {
            Log.e(TAG,"SAXException occured while validating checksum");
        } catch (IOException e) {
            Log.e(TAG, "IOException occurred while validating checksum");
        }

        return retVal;

    }

    //create the checksum value for the downloaded update.zip file
    static byte[] createChecksumValue(String filename){
        MessageDigest complete = null;
        try {
            InputStream fis = new FileInputStream(filename);

            byte[] buffer = new byte[1024];
            complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();

        } catch (FileNotFoundException e) {
            Log.e(TAG,"File for which the checksum value is to be calculated is not found" , e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG,"No such algorithm exception from message digest in createCheckSum method", e);
        } catch (IOException e) {
            Log.e(TAG,"No such algorithm exception from message digest in createCheckSum method" , e);
        }
        return complete.digest();
    }

    //convert the MD5 byte array to string
    static String getMD5String(byte[] hashArray) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hashArray.length; ++i) {
            sb.append(Integer.toHexString((hashArray[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
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
