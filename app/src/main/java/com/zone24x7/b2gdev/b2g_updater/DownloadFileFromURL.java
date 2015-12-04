package com.zone24x7.b2gdev.b2g_updater;


import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFileFromURL extends AsyncTask<String, Integer, String> {

    private static String TAG = "b2g-updater";
    private UpdaterFragment container;
    private PowerManager.WakeLock mWakeLock;

    public DownloadFileFromURL(UpdaterFragment f) {
        this.container = f;
    }


    @Override
    protected String doInBackground(String... params) {

        OutputStream output = null;
        InputStream input = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection)url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful so that you can show a typical 0-100%
            // progress bar
            int lengthOfFile = connection.getContentLength();
            input = connection.getInputStream();
            final File dest = new File(container.getActivity().getString(R.string.path_download_target));
            output = new FileOutputStream(dest);

            byte data[] = new byte[4096];
            int count;
            long total = 0;

            while ((count = input.read(data)) != -1) {
                // allow canceling
                if (isCancelled()) {
                    input.close();
                    output.close();
                    dest.delete();
                    mWakeLock.release();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (lengthOfFile > 0) // only if total length is known
                    publishProgress((int) (total * 100 / lengthOfFile));
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

        return null;

    }

    protected void onProgressUpdate(Integer... progress) {
        container.setProgress(progress[0]);
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

        mWakeLock.release();
        // The activity can be null if it is thrown out by Android while task is running!
        if(container!=null && container.getActivity()!=null) {


            if (result != null) {
                Toast.makeText(container.getActivity(), "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Download error: " + result);
            }else
                Toast.makeText(container.getActivity(),"Update downloaded", Toast.LENGTH_SHORT).show();

            this.container.doneDownloadingOTA(result);
            this.container = null;
        }
    }
}
