package com.example.betim.a4g_remotedriving;

/**
 * Created by Victor on 2017-02-20.
 */

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BoundService extends Service {
    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();
    public static String pingError = null;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "in onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "in onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "in onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "in onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "in onDestroy");
    }

    public int pingHost(final String host) throws IOException, InterruptedException {
        final int[] result = new int[1];
        final String pingCommand = "/system/bin/ping -c " + 1 + " " + host;
       new Thread(new Runnable() {
           @Override
           public void run() {
               Runtime runtime = Runtime.getRuntime();
               Process process = null;
               try {
                   process = runtime.exec(pingCommand);
               } catch (IOException e) {
                   e.printStackTrace();
               }
               try {
                   process.waitFor();
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               result[0] = process.exitValue();
           }
       }).start();
        return result[0];
    }

    public double getLatency(String host){
        String pingCommand = "/system/bin/ping -c " + 1 + " " + host;
        String inputLine = "";
        double avgRtt = 0;
        try {
            Process process = Runtime.getRuntime().exec(pingCommand);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            inputLine = bufferedReader.readLine();
            while ((inputLine != null)) {
                if (inputLine.length() > 0 && inputLine.contains("avg")) {
                    break;
                }
                inputLine = bufferedReader.readLine();
            }
        }
        catch (IOException e){
            Log.v(LOG_TAG, "getLatency: EXCEPTION");
            e.printStackTrace();
        }
        String afterEqual = inputLine.substring(inputLine.indexOf("="), inputLine.length()).trim();
        String afterFirstSlash = afterEqual.substring(afterEqual.indexOf('/') + 1, afterEqual.length()).trim();
        String strAvgRtt = afterFirstSlash.substring(0, afterFirstSlash.indexOf('/'));
        avgRtt = Double.valueOf(strAvgRtt);
        return avgRtt;
    }

    public class MyBinder extends Binder {
        BoundService getService() {
            return BoundService.this;
        }
    }
}