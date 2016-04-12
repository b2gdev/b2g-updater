package com.zone24x7.b2gdev.b2g_updater;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


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
        Log.d(TAG,"came inside CheckForUpdates doInBackground");

        File fileToDelete = new File(container.getActivity().getString(R.string.path_update_info_xml));
        if(fileToDelete.exists()) {
            fileToDelete.delete();
            Log.d(TAG,"File deleted");
        } else{
            Log.d(TAG,"Couldn't delete file");
        }

        OutputStream output = null;
        InputStream input = null;
        HttpURLConnection connection = null;
        String currentVersionNumber = "";
        mResult = null;

        try {
            URL url = new URL(container.getString(R.string.url_update_info_file));
            connection = (HttpURLConnection)url.openConnection();
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            input = connection.getInputStream();
            File dest = new File(container.getActivity().getString(R.string.path_update_info_xml));
            output = new FileOutputStream(dest,false);

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
            Log.e(TAG,"Exception occurred",e);
            return null;
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

        try {
            currentVersionNumber = Build.ID;
            mResult = getMaxCompatibleVersion(new File(container.getActivity().getString(R.string.path_update_info_xml)), currentVersionNumber);
            Log.d(TAG,"Current Version : "+currentVersionNumber+" Max.Available Update Version : "+mResult);
        } catch (Exception e) {
            mResult = null;
            return e.toString();
        }

        if(mResult != null) {
            if (!isUpdateValid(currentVersionNumber,mResult)){
                mResult = null;
                return null;
            }

        }

        return mResult;
    }

//    @Override
//    protected String doInBackground(String... params) {
//        OutputStream output = null;
//        InputStream input = null;
//        HttpURLConnection connection = null;
//
//        try {
//            URL url = new URL(container.getString(R.string.url_ota_info_location));
//            connection = (HttpURLConnection)url.openConnection();
//            connection.connect();
//
//            // expect HTTP 200 OK, so we don't mistakenly save error report
//            // instead of the file
//            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
//                return "Server returned HTTP " + connection.getResponseCode()
//                        + " " + connection.getResponseMessage();
//            }
//
//            input = connection.getInputStream();
//            final File dest = new File(container.getActivity().getString(R.string.path_updates));
//            output = new FileOutputStream(dest);
//
//            byte data[] = new byte[4096];
//            int count;
//            long total = 0;
//
//            while ((count = input.read(data)) != -1) {
//                // allow canceling
//                if (isCancelled()) {
//                    input.close();
//                    output.close();
//                    mWakeLock.release();
//                    return null;
//                }
//                total += count;
//                output.write(data, 0, count);
//            }
//
//        } catch (Exception e) {
//            return e.toString();
//        } finally {
//            try {
//                if (output != null)
//                    output.close();
//                if (input != null)
//                    input.close();
//            } catch (IOException ignored) {
//            }
//
//            if (connection != null)
//                connection.disconnect();
//        }
//
//        BufferedReader buf = null;
//        try {
//            buf = new BufferedReader(new FileReader(container.getActivity().getString(R.string.path_updates)));
//            mResult = buf.readLine();
//        } catch (IOException e) {
//            mResult = null;
//            return e.toString();
//        }finally {
//            if(buf != null)
//                try {
//                    buf.close();
//                } catch (IOException e) {
//                    // ignore
//                }
//        }
//
//        if(mResult != null) {
//            if (!isUpdateValid(mResult)){
//                mResult = null;
//                return null;
//            }
//        }
//
//        return null;
//    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG,"came inside CheckForUpdates onPreExecute");
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
                container.doneUpdates(result);
            }else {
                //Toast.makeText(container.getActivity(), "Error while checking for updates: " + result, Toast.LENGTH_LONG).show();
                Log.e(TAG,"Error while checking for updates: ");
                container.doneUpdates(null);
            }

            this.container = null;
        }
    }

    // Is available update valid?
//    private boolean isUpdateValid(String update) {
//
//        if(isVersionNumberNewer(update))
//            if(isValidURLAvailable(update))
//                return true;
//
//        return false;
//    }

    private boolean isUpdateValid(String currentVersionNumber,String updateVersionNumber) {
        boolean retVal = false;
        ComparableVersion currentVersion = new ComparableVersion(currentVersionNumber);
        ComparableVersion updateVersion = new ComparableVersion(updateVersionNumber);

        if(currentVersion.compareTo(updateVersion) < 0){
            if(isValidURLAvailable(updateVersionNumber))
                retVal = true;
        }

        return retVal;
    }

    private String getMaxCompatibleVersion(File file, String versionNumber) {
        String versionId="";
        String maxEligibleUpdateId="";

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
                    versionId = versionInfoNodes.item(versionIdCount).getAttributes().getNamedItem("id").getNodeValue();

                    if(versionId.equals(versionNumber)){
                        maxEligibleUpdateId = versionInfoNodes.item(versionIdCount).getAttributes().getNamedItem("maxUpdateId").getNodeValue();
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            Log.w(TAG,e);
        }   catch (SAXException e) {
            Log.w(TAG,e);
        } catch (IOException e) {
            Log.w(TAG,e);
        }

        return maxEligibleUpdateId;
    }

    private boolean isVersionNumberNewer(String versionNumber){

        boolean retVal = false;

        StringTokenizer versionNumST = new StringTokenizer(versionNumber,"v.-");
        StringTokenizer currentVersionST = new StringTokenizer(Build.ID,"v.-");

        while(versionNumST.hasMoreElements() && currentVersionST.hasMoreElements()){
            String newV = versionNumST.nextToken();
            String currentV = currentVersionST.nextToken();
            if(newV.compareTo(currentV) == 0){
                continue;
            }else if (newV.compareTo(currentV) < 0)
                break;
            else {
                retVal = true;
                break;
            }
        }

        return retVal;
    }

    private boolean isValidURLAvailable(String versionNumber){

        boolean retVal = false;

        String tempURL = container.generateDownloadURL(versionNumber);
        if (URLUtil.isValidUrl(tempURL)) {
            try {
                URL u = new URL(tempURL);
                HttpURLConnection.setFollowRedirects(true);
                HttpURLConnection huc = (HttpURLConnection) u.openConnection();
                huc.setRequestMethod("HEAD");
                int tempVal = huc.getResponseCode();
                if (tempVal == HttpURLConnection.HTTP_OK) {
                    retVal = true;
                }else
                    Log.w(TAG, "Error while verifying update : http error=" + tempVal);
            } catch (IOException e) {
                Log.w(TAG, "Error while verifying update : " + e.toString());
            }
        }

        return retVal;
    }
}
