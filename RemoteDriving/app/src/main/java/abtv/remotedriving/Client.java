package abtv.remotedriving;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import io.github.controlwear.virtual.joystick.android.JoystickView;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/*
 * Written by Albin Hellqvist, Betim Raqi,
 * Tasdikul Huda and Victor Christoffersson
 */
public class Client extends AppCompatActivity {
    private static final String TAG = "Client";
    private String ipAdress;
    private int port;

    private Socket clientSocket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;

    private WebView mWebRTCWebView;
    private Thread pingThread;
    private TextView pingText;
    private long time;

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

        Log.d(TAG, "in onCreate");

        Intent intent = getIntent();
        ipAdress = intent.getStringExtra("ip_addr");
        port = Integer.parseInt(intent.getStringExtra("port"));
        initStream(100);

        pingText = (TextView)findViewById(R.id.pingbox);

        //Initializes joystick
        JoystickView joystick = (JoystickView)findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(final int angle, final int strength) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (socketWriter != null) {
                            socketWriter.print("("+angle+";"+strength+")");
                            socketWriter.flush();
                        }
                    }
                }).start();
            }
        }, 100);

        Button callButton = (Button)findViewById(R.id.callButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebRTCWebView.loadUrl("javascript:document.getElementById(\"start\").click();");
            }
        });

        Button hangUpButton = (Button)findViewById(R.id.hangUpButton);
        hangUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebRTCWebView.loadUrl("javascript:document.getElementById(\"stop\").click();");
            }
        });
    }

    private void initStream(int zoom) {
        final String streamURI = "http://" + ipAdress + ":" + 8080 + "/stream/webrtc";
        mWebRTCWebView = (WebView)findViewById(R.id.stream);
        WebSettings settings = mWebRTCWebView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        //settings.setMediaPlaybackRequiresUserGesture(false);

        // Hide the zoom controls for HONEYCOMB+
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            settings.setDisplayZoomControls(false);
        }

        // Enable remote debugging via chrome://inspect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
                        if (request.getOrigin().toString().equals(streamURI)) {
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
    private void initNetwork(final String ip, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Connecting...");
                    clientSocket = new Socket(ip, port);
                    Log.d(TAG, "Connected to server... proceeding...");
                    socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    socketWriter = new PrintWriter(clientSocket.getOutputStream());
                    Log.d(TAG, "Input/Outputstreams attached... proceeding...");
                }
                catch(IllegalArgumentException e) {
                    e.printStackTrace();
                    Log.d(TAG, "IllegalARG didn't work: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Invalid port number, should be between 1023 - 65000", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                catch (UnknownHostException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Error connecting to server", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                catch(Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Exception didn't work: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Recursive method call which threads ping requests in background
     * @param
     */
    public void startPing() {
        Log.d(TAG, "IP is: " + ipAdress);
        pingThread = new Thread(new Runnable() {
            @Override
            public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                SystemClock.sleep(1000);
                if (socketWriter != null) {
                    socketWriter.print("ping:" + System.currentTimeMillis());
                    socketWriter.flush();
                }
                try {
                    String message = socketReader.readLine();

                    if (message != null) {
                        time = System.currentTimeMillis() - Long.parseLong(message);
                        Log.d(TAG, "Result of ping is: " + time);
                        //appendLog(String.valueOf(time));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pingText.setText("Ping: " + String.valueOf(time) + " ms");
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


    /*public void appendLog(String text) {
        File logfile = new File(this.getFilesDir(), "latencylog");

        if (!logfile.exists()) {
            try {
                logfile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String currentDateandTime = sdf.format(Calendar.getInstance().getTime());
            BufferedWriter buf = new BufferedWriter(new FileWriter(logfile, true));
            buf.append(currentDateandTime + " : " + text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        try {
            socketReader.close();
            socketWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause");

        new Thread(new Runnable() {
            @Override
            public void run() {
                socketWriter.print("quit");
                socketWriter.flush();
            }
        }).start();

        if (pingThread.isAlive()) {
            pingThread.interrupt();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "in onResume");

        initNetwork(ipAdress, port);
        startPing();
    }
}
