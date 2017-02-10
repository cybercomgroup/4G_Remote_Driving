package com.example.betim.a4g_remotedriving;

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
    private static final String TAG ="Client";
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_client);
        Intent intent = getIntent();
        String ip = intent.getStringExtra("ipaddr");
        String port = intent.getStringExtra("port");

        Thread comThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Connecting...");
                    clientSocket = new Socket("129.16.229.123", 3000);
                    //clientSocket.setSendBufferSize(1);
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

        Toast.makeText(this, "Ip: "+ ip + ", Port: " + port, Toast.LENGTH_SHORT).show();
        final WebView webView = (WebView)findViewById(R.id.stream);
        int default_zoom_level=100;
        webView.setInitialScale(default_zoom_level);
        webView.post(new Runnable()
        {
            @Override
            public void run() {
                int width = webView.getWidth();
                int height = webView.getHeight();
                Log.d(TAG, "Webview initialized, width: " + width + "height: " + height);
                webView.loadUrl("http://129.16.229.123:8000/stream" + "?width="+width+"&height="+height);
                Log.d(TAG, "Webview loaded successfully... proceeding...");
            }
        });
    }

    public void send(final View view){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String clientMsgL = "1";
                    String clientMsgR = "2";
                    switch (view.getId()) {
                        case R.id.left:
                            Log.d(TAG, "Sending data: " + clientMsgL + " to server...");
                            out.println(clientMsgL);
                            out.flush();
                            break;
                        case R.id.right:
                            Log.d(TAG, "Sending data: " + clientMsgR + " to server...");
                            out.println(clientMsgR);
                            out.flush();
                            break;
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}