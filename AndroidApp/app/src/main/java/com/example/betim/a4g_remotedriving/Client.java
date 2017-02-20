package com.example.betim.a4g_remotedriving;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends AppCompatActivity {
    //Variables:
    private static final String TAG = Client.class.getName();
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    BoundService mBoundService;
    boolean mServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_client);

        Intent intent = getIntent();
        String ip = intent.getStringExtra("ipaddr");
        //int port = Integer.parseInt(intent.getStringExtra("port"));
        //initNetWork(ip, port);
        initNetwork("129.16.229.123", 3006);
        initStream("http://129.16.229.123:8000/stream", 100);
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

    public void sendPing(View view){
        Log.d(TAG, "Is service bound=" + mServiceBound);
        if(mServiceBound){
            int num = mBoundService.getPingstamp();
            Log.d(TAG, "Result of ping is: " + num);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "In onStart");
        Intent intent = new Intent(this, BoundService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mServiceBound) {
            Log.d(TAG, "In onStop");
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "in onServiceDisconnected");
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "In onServiceConnected");
            BoundService.MyBinder myBinder = (BoundService.MyBinder) service;
            mBoundService = myBinder.getService();
            mServiceBound = true;
        }
    };

}