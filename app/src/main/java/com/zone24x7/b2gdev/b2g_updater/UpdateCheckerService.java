package com.zone24x7.b2gdev.b2g_updater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.URLUtil;

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
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by root on 3/9/16.
 */
//debugger available in /home/b2g/src/b2g_dev/android-sdk-linux/tools
public class UpdateCheckerService extends Service {

    private static String TAG = "b2g-updater";

    private final long updatePeriod = 1000*60*60*12; //check every 12 hours
    //private final long updatePeriod = 60000;
    private Timer updateTimer;
    private CheckForUpdates mUpdateListTask;
    Intent intent;
    private String availableUpdateVersion;

    private class UpdateTask extends TimerTask {
        public void run() {
            ConnectivityManager connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mMobileData = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if(mWifi.isConnected() || mMobileData.isConnected()) {
                if (updateAvailable()) {
                    Log.d(TAG, "Update available!");
                    showNotification();

                }
            }
        }
    }

    private void showNotification(){
        Log.d(TAG,"Came inside ShowNotifications method");
        if(intent != null) {
            Log.d(TAG, "Intent inside showNotification method is not null");
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Intent notificationIntent = new Intent(this,MainActivity.class);
            Log.d(TAG,"Update version passed to UpdaterFragment is "+this.availableUpdateVersion);
            notificationIntent.putExtra("updateVersion",this.availableUpdateVersion);
            PendingIntent contentIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
            Notification.Builder builder = new Notification.Builder(this);
            builder.setAutoCancel(true);
            builder.setTicker("New Firmware Update Available");
            builder.setContentTitle("Update Available");
            builder.setContentIntent(contentIntent);
            builder.setOngoing(true);
            builder.setSmallIcon(R.drawable.icon);
            builder.build();

            Notification notification = builder.getNotification();
            notification.flags = Notification.FLAG_NO_CLEAR;
            notificationManager.notify(1,notification);
        }
    }

    private UpdateTask updateTask;

    private void launchMainActivity(){
        Intent dialogIntent = new Intent(this, MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "created");
        updateTimer = new Timer();
        updateTask = new UpdateTask();
    }

//    @Override
//    public void onStart(final Intent intent, final int startId) {
//        super.onStart(intent, startId);
//        Log.i(TAG, "started");
//        updateTimer.schedule(updateTask, 0, updatePeriod);
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "started");
        this.intent = intent;
        updateTimer.schedule(updateTask, 0, updatePeriod);
        return START_NOT_STICKY;
    }

    private boolean updateAvailable(){

        File fileToDelete = new File(getResources().getString(R.string.path_update_info_xml));
        if(fileToDelete.exists()) {
            fileToDelete.delete();
            Log.d(TAG,"File deleted");
        } else{
            Log.d(TAG,"Couldn't delete file");
        }

        boolean retVal = false;

        OutputStream output = null;
        InputStream input = null;
        HttpURLConnection connection = null;
        String currentVersionNumber = "";
        String maxUpdateVersion="";

        try {
            URL url = new URL(getString(R.string.url_update_info_file));
            connection = (HttpURLConnection)url.openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            input = connection.getInputStream();
            File dest = new File(getResources().getString(R.string.path_update_info_xml));
            output = new FileOutputStream(dest,false);

            byte data[] = new byte[4096];
            int count;
            long total = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
            }

            try {
                currentVersionNumber = Build.ID;
                maxUpdateVersion = getMaxCompatibleVersion(new File(getResources().getString(R.string.path_update_info_xml)), currentVersionNumber);
                Log.d(TAG,"Current Version : "+currentVersionNumber+" Max.Available Update Version : "+maxUpdateVersion);
            } catch (Exception e) {
                maxUpdateVersion = null;
                Log.w(TAG, e.toString());
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
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

        if(maxUpdateVersion != null) {
            if (!isUpdateValid(currentVersionNumber,maxUpdateVersion)){
                maxUpdateVersion = null;

                return false;
            }
            this.availableUpdateVersion = maxUpdateVersion;
            retVal = true;
        }

        Log.d(TAG,"Result from updateAvailable is : "+retVal);

        return retVal;
    }

    private boolean isUpdateValid(String currentVersionNumber,String updateVersionNumber) {
        Log.d(TAG,"came inside isUpdateValid method");
        boolean retVal = false;
        ComparableVersion currentVersion = new ComparableVersion(currentVersionNumber);
        ComparableVersion updateVersion = new ComparableVersion(updateVersionNumber);

        if(currentVersion.compareTo(updateVersion) < 0){
            Log.d(TAG,"update version is greater than current version");
            if(isValidURLAvailable(updateVersionNumber))
                retVal = true;
        }

        return retVal;
    }

    private boolean isValidURLAvailable(String versionNumber){
        Log.d(TAG,"Came inside isValidURLAvailable");
        boolean retVal = false;

        String tempURL = generateDownloadURL(versionNumber);
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

    public String generateDownloadURL(String version) {

        return String.format("%s%s/update-%s.zip", getResources().getString(R.string.url_github_release), version,version);
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

}
