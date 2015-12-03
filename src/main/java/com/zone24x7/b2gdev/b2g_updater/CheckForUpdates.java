package com.zone24x7.b2gdev.b2g_updater;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class CheckForUpdates  extends AsyncTask<String, Integer, String> {

    UpdaterFragment container;
    private PowerManager.WakeLock mWakeLock;

    public CheckForUpdates(UpdaterFragment f) {
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

            // this will be useful so that you can show a tipical 0-100%
            // progress bar
            int lengthOfFile = connection.getContentLength();

            // download the file
            input = connection.getInputStream();

            // Output stream
            //final File dest = new File(container.getActivity().getString(R.string.path_test_updates));
            final File dest = new File(container.getActivity().getString(R.string.path_updates));
            output = new FileOutputStream(dest);

            byte data[] = new byte[4096];
            int count;
            long total = 0;

            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    output.close();
                    PrintWriter writer = new PrintWriter(dest);
                    writer.print("");
                    writer.close();
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
                Toast.makeText(container.getActivity(), "Download error: " + result, Toast.LENGTH_LONG).show();
                container.doneUpdates(null);
            }else {
                //Toast.makeText(container.getActivity(), "Got list of updates", Toast.LENGTH_SHORT).show();
                BufferedReader buf = null;
                try {
                    buf = new BufferedReader(new FileReader(container.getActivity().getString(R.string.path_updates)));
                    result = buf.readLine();
                } catch (IOException e) {
                    result = null;
                }

                try {
                    if(buf != null)
                        buf.close();
                } catch (IOException e) {
                    // ignore
                }
                /*
                if(result != null) {
                    String tempURL = container.generateDownloadURL(result);
                    if (!URLUtil.isValidUrl(tempURL)) {
                        result = null;
                    } else {
                        try {
                            URL u = new URL(tempURL);
                            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
                            huc.setRequestMethod("HEAD");
                            int tempVal =  huc.getResponseCode();
                            if (tempVal != HttpURLConnection.HTTP_OK){
                                result = null;
                            }
                        } catch (IOException e) {
                            result = null;
                        }
                    }
                }
                */
                container.doneUpdates(result);
            }

            this.container = null;
        }
    }
}
