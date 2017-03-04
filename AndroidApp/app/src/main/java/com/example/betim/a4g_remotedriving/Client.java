package com.example.betim.a4g_remotedriving;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.util.Log;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import io.github.controlwear.virtual.joystick.android.JoystickView;
import java.net.UnknownHostException;

public class Client extends AppCompatActivity {
    //--------Strings--------
    private static final String TAG = "Client";
    private String PARENT_IP;
    private String PARENT_PORT;
    private int PORT;
    //--------Network Variables--------
    private Socket clientSocket;
    private PrintWriter out;
    private PrintWriter out2;
    private BufferedReader in;
    //--------Service Variables--------
    boolean initialized = false;
    //Ping variables
    private WebView mWebRTCWebView;
    private Thread pingThread;
    private boolean threadLife = false;
    private TextView pingText;
    long time;

    /**
     * Initializes the Client
     * Sets borderless View
     * Initializes network connection and Stream
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "in onCreate");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_client);

        Intent intent = getIntent();
        PARENT_IP = intent.getStringExtra("ip_addr");
        PARENT_PORT = intent.getStringExtra("port");
        PORT = Integer.parseInt(PARENT_PORT);
        initStream(100);

        pingText = (TextView)findViewById(R.id.pingbox);

        //Initializes joystick
        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                String steerSignal = "("+angle+";"+strength+")";
               if(out2 != null) {
                   out2.print(steerSignal);
                   out2.flush();
               }

            }
        },100);
        initialized = true;
    }

    private void initStream(int zoom){
        final String streamURI = "http://" + PARENT_IP + ":" + 8080 + "/stream/webrtc";
        mWebRTCWebView = (WebView) findViewById(R.id.stream);

        WebSettings settings = mWebRTCWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            settings.setDisplayZoomControls(false);
        }
        // Enable remote debugging via chrome://inspect
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebRTCWebView.setWebViewClient(new WebViewClient());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebRTCWebView, true);


        mWebRTCWebView.loadUrl(streamURI);
        mWebRTCWebView.setInitialScale(zoom);

        mWebRTCWebView.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "onPermissionRequest");
                Client.this.runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        if(request.getOrigin().toString().equals(streamURI)) {
                            request.grant(request.getResources());
                        } else {
                            request.deny();
                        }
                    }
                });
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
                    Log.d(TAG, "Connected to server... proceeding...");
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out2 = new PrintWriter(clientSocket.getOutputStream(),true);
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    Log.d(TAG, "Input/Outputstreams attached... proceeding...");
                }
                catch(IllegalArgumentException e) {
                    e.printStackTrace();
                    Log.d(TAG, "IllegalARG didn't work: "+ e.getMessage());
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
                    Log.d(TAG, "BETIMGENERAL didn't work: "+ e.getMessage());
                }
            }
        });
        comThread.start();

    }


    /**
     * Recursive method call which threads ping requests in background
     * @param
     */
    public void startPing(){
        Log.d(TAG, "IP is: " + PARENT_IP);
        pingThread = new Thread(new Runnable() {
        @Override
            public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                SystemClock.sleep(1000);
                if (out != null) {
                    out.print("ping:" + System.currentTimeMillis());
                    out.flush();
                }
                try {
                    String message = in.readLine();
                    Log.d(TAG, "Time from server: " + in);

                    if (message != null) {
                        time = System.currentTimeMillis() - Long.parseLong(message);
                        Log.d(TAG, "Result of ping is: " + time);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pingText.setText(String.valueOf(time));
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Error error error");
                    e.printStackTrace();
                }
            }
        }
        });
        pingThread.start();
    }

    /**
     * Overridden callback to handle lifecycle of BoundServices and Recursive Handler
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    /**
     * Overriden callback to handle lifecycle of BoundServices
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "in onPause");
        out.print("quit");
        out.flush();
        if(pingThread.isAlive()) {
            pingThread.interrupt();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "in onResume");
        initNetwork(PARENT_IP, PORT);
        startPing();
    }

}