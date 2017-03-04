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
import java.io.PrintWriter;

public class BoundService extends Service {
    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();
    public static String pingError = null;
    public static double ping=0;
    public static int exitValue;


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

    public long sendMessage(PrintWriter out, BufferedReader in) {
        long timeDirreference = 0;
        Log.d(LOG_TAG, "Entering threaded service, sending time to server...");
        if (out != null) {
            out.print("ping:" + System.currentTimeMillis());
            out.flush();
        }
        try {
            String message = in.readLine();
            Log.d(LOG_TAG, "Time from server: " + in);
            timeDirreference = System.currentTimeMillis() - Long.parseLong(message);
            Log.d(LOG_TAG, "Finished sending data, exiting service...");
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error error error");
            e.printStackTrace();
        }
        return timeDirreference;
    }

        /**
    public double getLatency(String host, int timeout){
        final String innerHost = host;
        final int innerTimeout = timeout;
        Log.d(LOG_TAG, "Host being pinged: " + host);
        Log.d(LOG_TAG, "Innerhost = " + innerHost + " timeout: " + timeout + " innertimeout= " + innerTimeout);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String pingCommand = "/system/bin/ping -c " + 1 + " " + innerHost;
                String inputLine = "";
                double avgRtt;
                try {
                    Process process = Runtime.getRuntime().exec(pingCommand);
                    process.waitFor();
                    int exit = process.exitValue();
                    Log.d(LOG_TAG, "exitValue: " + exit);
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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (inputLine != null) {
                    String afterEqual = inputLine.substring(inputLine.indexOf("="), inputLine.length()).trim();
                    String afterFirstSlash = afterEqual.substring(afterEqual.indexOf('/') + 1, afterEqual.length()).trim();
                    String strAvgRtt = afterFirstSlash.substring(0, afterFirstSlash.indexOf('/'));
                    avgRtt = Double.valueOf(strAvgRtt);
                    Log.d(LOG_TAG, "value of ping: " + avgRtt);
                    postResult(avgRtt);
                }
                else {
                    avgRtt = 1;
                    Log.d(LOG_TAG, "value of ping: " + avgRtt);
                    postResult(avgRtt);
                }
            }
        }).start();
        return ping;
    }*/

    public class MyBinder extends Binder {
        BoundService getService() {
            return BoundService.this;
        }
    }
}