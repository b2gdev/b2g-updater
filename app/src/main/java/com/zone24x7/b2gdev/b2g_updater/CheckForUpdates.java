package com.zone24x7.b2gdev.b2g_updater;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class CheckForUpdates  extends AsyncTask<String, Integer, String> {

    private static String TAG = "b2g-updater";
    private UpdaterFragment container;
    private PowerManager.WakeLock mWakeLock;
    private String mResult = null;

    public CheckForUpdates(UpdaterFragment f) {
        this.container = f;
    }

    @Override
    protected String doInBackground(String... params) {
        OutputStream output = null;
        InputStream input = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(container.getString(R.string.url_ota_info_location));
            connection = (HttpURLConnection)url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            int lengthOfFile = connection.getContentLength();
            input = connection.getInputStream();
            final File dest = new File(container.getActivity().getString(R.string.path_updates));
            output = new FileOutputStream(dest);

            byte data[] = new byte[4096];
            int count;
            long total = 0;

            while ((count = input.read(data)) != -1) {
                // allow canceling
                if (isCancelled()) {
                    input.close();
                    output.close();
                    mWakeLock.release();
                    return null;
                }
                total += count;
                output.write(data, 0, count);
            }

        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }

        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new FileReader(container.getActivity().getString(R.string.path_updates)));
            mResult = buf.readLine();
        } catch (IOException e) {
            mResult = null;
            return e.toString();
        }finally {
            if(buf != null)
                try {
                    buf.close();
                } catch (IOException e) {
                    // ignore
                }
        }

        if(mResult != null) {
            if (!isUpdateValid(mResult)){
                mResult = null;
                return null;
            }
        }

        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) container.getActivity().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        // The activity can be null if it is thrown out by Android while task is running!
        mWakeLock.release();
        if(container!=null && container.getActivity()!=null) {

            if (result != null) {
                Toast.makeText(container.getActivity(), "Error while checking for updates: " + result, Toast.LENGTH_LONG).show();
                container.doneUpdates(null);
            }else {
                container.doneUpdates(mResult);
            }

            this.container = null;
        }
    }

    // Is available update valid?
    private boolean isUpdateValid(String update) {

        boolean retVal = false;

        // Check whether version number is valid
        if(update.length() == Build.ID.length()){
            if(update.compareTo(Build.ID) != 0){
                retVal = true;
            }
        }else if(update.compareTo("") != 0){
            retVal = true;
        };

        // Check if assumed URL is valid
        if(retVal){
            String tempURL = container.generateDownloadURL(update);
            if(!URLUtil.isValidUrl(tempURL)) {
                retVal = false;
            }else {
                try {
                    URL u = new URL(tempURL);
                    HttpURLConnection.setFollowRedirects(true);
                    HttpURLConnection huc = (HttpURLConnection) u.openConnection();
                    huc.setRequestMethod("HEAD");
                    int tempVal =  huc.getResponseCode();
                    if (tempVal != HttpURLConnection.HTTP_OK){
                        retVal = false;
                        Log.w(TAG, "Error while verifying update : http error=" + tempVal);
                    }
                } catch (IOException e) {
                    retVal = false;
                    Log.w(TAG, "Error while verifying update : " + e.toString());
                }
            }
        }
        return retVal;
    }
}
