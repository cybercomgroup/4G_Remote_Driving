package com.example.betim.a4g_remotedriving;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
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
    //Ping variables
    int PARENT_TIMEOUT;
    private WebView mWebRTCWebView;


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
        PARENT_IP = intent.getStringExtra("ip_addr");
        PARENT_PORT = intent.getStringExtra("port");
        int PORT_INT = Integer.parseInt(PARENT_PORT);
        //int port = Integer.parseInt(intent.getStringExtra("port"));
        //initNetWork(ip, port);
        initNetwork(PARENT_IP, PORT_INT);
        initStream("http://" + PARENT_IP +"/stream/webrtc", 150);
        handler = new Handler();
        PARENT_TIMEOUT = 1;

        //Initializes joystick
        /**JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                String steerSignal = "("+angle+";"+strength+")";
               if(out != null) {
                   out.print(steerSignal);
                   out.flush();
               }

            }
        },50);*/
    }

    private void initStream(final String URI, int zoom){

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

        mWebRTCWebView.loadUrl("http://95.80.12.203:3000/stream/webrtc");
        mWebRTCWebView.setInitialScale(zoom);

        mWebRTCWebView.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "onPermissionRequest");
                Client.this.runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        if(request.getOrigin().toString().equals("http://95.80.12.203:3000/stream/webrtc")) {
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
     * Initializes the stream according to params
     * @param URI
     * @param zoom

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
                //http://95.80.12.203:3000/stream/webrtc
                webView.loadUrl("http://95.80.12.203:3000/stream/webrtc");
                Log.d(TAG, "Webview loaded successfully... proceeding...");
            }
        }); */

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
     * Contacts Bound Service if ping is requested and updates Clients View depending on termination
     * result.
     * @param view
     */
    public void sendPing(View view){
        Log.d(TAG, "Is service bound=" + mServiceBound);
        if(mServiceBound){
            double ping = mBoundService.getLatency(PARENT_IP);
            TextView temp = (TextView) findViewById(R.id.pingbox);
            temp.setText(ping + " ms");
            Log.d(TAG, "Result of ping is: " + ping);
        }
    }

    /**
     * Recursive method call which threads ping requests in background
     * @param
     */
    public void startRecursivePing(View view){
        //"172.217.22.163" google
        Log.d(TAG, "IP is: " + PARENT_IP);
        final int delay = PARENT_TIMEOUT * 1000;
        if(mServiceBound) {
            Log.d(TAG, "Recursive starting...");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    double ping = mBoundService.getLatency(PARENT_IP);
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