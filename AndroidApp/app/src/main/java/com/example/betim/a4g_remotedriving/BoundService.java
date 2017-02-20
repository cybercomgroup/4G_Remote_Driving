package com.example.betim.a4g_remotedriving;

/**
 * Created by Victor on 2017-02-20.
 */

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

import static android.R.attr.host;

public class BoundService extends Service {
    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();


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

    public int getPingstamp() {
        final int[] result = new int[1];
       new Thread(new Runnable() {
           @Override
           public void run() {
               Runtime runtime = Runtime.getRuntime();
               Process process = null;
               try {
                   process = runtime.exec("ping -c 1" + host);
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

    public class MyBinder extends Binder {
        BoundService getService() {
            return BoundService.this;
        }
    }
}