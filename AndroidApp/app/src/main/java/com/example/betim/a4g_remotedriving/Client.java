package com.example.betim.a4g_remotedriving;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends AppCompatActivity {
    //Variables:
    private static final String TAG = Client.class.getName();
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean icmpRun = false;
    public static final int JOB_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_client);

        Intent intent = getIntent();
        String ip = intent.getStringExtra("ipaddr");
        int port = Integer.parseInt(intent.getStringExtra("port"));
        //initNetWork(ip, port);
        initNetwork("129.16.229.123", 3006);
        initStream("http://129.16.229.123:8000/stream", 100);

        if(!icmpRun) {
            scheduleJob();
            icmpRun = true;
            Log.d(TAG, "icmpService started..");
        }
    }

    private void initStream(final String URI, int zoom){
        final WebView webView = (WebView)findViewById(R.id.stream);
        int default_zoom_level=zoom;
        webView.setInitialScale(default_zoom_level);
        webView.post(new Runnable()
        {
            @Override
            public void run() {
                int width = webView.getWidth();
                int height = webView.getHeight();
                Log.d(TAG, "Webview initialized, width: " + width + "height: " + height);
                webView.loadUrl(URI + "?width="+width+"&height="+height);
                Log.d(TAG, "Webview loaded successfully... proceeding...");
            }
        });
    }

    private void initNetwork(final String ip, final int port){
        Thread comThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Connecting...");
                    clientSocket = new Socket(ip, port);
                    Log.d(TAG, "Connected to server... proceeding...");

                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    Log.d(TAG, "Input/Outputstreams attached... proceeding...");
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        comThread.start();
    }

    private void scheduleJob() {
        ComponentName serviceName = new ComponentName(this, icmpJobService.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, serviceName)
                .setPeriodic(2000)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build();
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result = scheduler.schedule(jobInfo);
        if(result == JobScheduler.RESULT_SUCCESS){
            Toast.makeText(this, R.string.job_scheduled_successfully, Toast.LENGTH_LONG).show();
        }
    }

    public void send(final View view){
        try {
            String clientMsgL = "1";
            String clientMsgR = "2";
            switch (view.getId()) {
                case R.id.left:
                    Log.d(TAG, "Sending data: " + clientMsgL + " to server...");
                    out.print(clientMsgL);
                    out.flush();
                    break;
                case R.id.right:
                    Log.d(TAG, "Sending data: " + clientMsgR + " to server...");
                    out.print(clientMsgR);
                    out.flush();
                    break;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(icmpRun){
            Intent icmpIntent = new Intent(this, icmpService.class);
            stopService(icmpIntent);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(icmpRun){
            Intent icmpIntent = new Intent(this, icmpService.class);
            stopService(icmpIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(icmpRun){
            Intent icmpIntent = new Intent(this, icmpService.class);
            stopService(icmpIntent);
        }
    }
}