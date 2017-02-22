package com.example.betim.a4g_remotedriving;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import java.lang.Math;
import java.net.UnknownHostException;

public class Client extends AppCompatActivity {
    //--------Strings--------
    private static final String TAG = "Client";
    private  String PARENT_IP;
    private  String PARENT_PORT;
    //--------Network Variables--------
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    //--------Service Variables--------
    BoundService mBoundService;
    boolean mServiceBound = false;
    //-------Handler Variables---------
    Handler handler;
    Runnable runnable;

    /**
     * Initializes the Client
     * Sets borderless View
     * Initializes network connection and Stream
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_client);

        Intent intent = getIntent();
        String PARENT_IP = intent.getStringExtra("ipaddr");
        String PARENT_PORT = intent.getStringExtra("port");
        int PORT_INT = Integer.parseInt(PARENT_PORT);
        //int port = Integer.parseInt(intent.getStringExtra("port"));
        //initNetWork(ip, port);
        initNetwork(PARENT_IP, PORT_INT);
        initStream("http://" + PARENT_IP +"/stream", PORT_INT);
        handler = new Handler();

        //Initializes joystick
        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                String steerSignal = "("+angle+";"+strength+")";
                out.print(steerSignal);
                out.flush();

            }
        },50);
    }

    /**
     * Initializes the stream according to params
     * @param URI
     * @param zoom
     */
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

    /**
     * Initializes Network connection between client and server.
     * @param ip
     * @param port
     */
    private void initNetwork(final String ip, final int port){
        Thread comThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Connecting...");

                    clientSocket = new Socket(ip, port);
                    //clientSocket.setSendBufferSize(1);

                    Log.d(TAG, "Connected to server... proceeding...");

                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    Log.d(TAG, "Input/Outputstreams attached... proceeding...");
                }
                catch(IllegalArgumentException e) {
                    e.printStackTrace();

                    Log.w("hej", "IllegalARG didn't work: "+ e.getMessage());

                    runOnUiThread(new Runnable(){
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Invalid port number, should be between 1023 - 65000",Toast.LENGTH_LONG).show();
                        }
                    });
                }
                catch (UnknownHostException e){
                    runOnUiThread(new Runnable(){
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Error connecting to server",Toast.LENGTH_LONG).show();
                        }
                    });
                }
                catch(Exception e) {
                    e.printStackTrace();
                    //Toast.makeText(Client.this, "hejhej", Toast.LENGTH_SHORT).show();
                    Log.w("hej", "BETIMGENERAL didn't work: "+ e.getMessage());
                }
            }
        });
        comThread.start();

    }

    /**
     * Onclick functions for Clients View, case casted by ViewIDs
     * @param view
     */
    public void send(final View view){
        try {
            String clientMsgL = "(222;9)";
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
    
    /**
     * Contacts Bound Service if ping is requested and updates Clients View depending on termination
     * result.
     * @param view
     */
    public void sendPing(View view){
        Log.d(TAG, "Is service bound=" + mServiceBound);
        if(mServiceBound){
            double ping = mBoundService.getLatency("172.217.22.163", 1);
            TextView temp = (TextView) findViewById(R.id.pingbox);
            temp.setText(ping + " ms");
            Log.d(TAG, "Result of ping is: " + ping);
        }
    }

    /**
     * Recursive method call which threads ping requests in background
     * @param ip
     * @param timeout important to call in seconds
     */
    public void startRecursivePing(final String ip, final int timeout){
        //"172.217.22.163" google
        final int delay = timeout * 1000;
        if(mServiceBound) {
            Log.d(TAG, "Recursive starting...");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    double ping = mBoundService.getLatency(ip, timeout);
                    TextView temp = (TextView) findViewById(R.id.pingbox);
                    temp.setText(ping + " ms");
                    Log.d(TAG, "Result of ping is: " + ping);
                    handler.postDelayed(this, delay);
                }
            }, delay);
        }
        else
            Log.d(TAG,"Recursive failed");
            return;
    }

    /**
     * Overridden callback to handle lifecycle of BoundServices and Recursive Handler
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "In onStart");
        Intent intent = new Intent(this, BoundService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        try{
            Thread.sleep(5000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        startRecursivePing(PARENT_IP, 1);
    }

    /**
     * Overriden callback to handle lifecycle of BoundServices
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (mServiceBound) {
            Log.d(TAG, "In onStop");
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
        handler.removeCallbacksAndMessages(runnable);
    }

    @Override
    protected void onPause(){
        super.onPause();
        //handler.removeCallbacks(runnable);
    }

    /**
     * Private helper for service Connection of BoundServices, returns the Ibinder of BoundService.java
     */
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