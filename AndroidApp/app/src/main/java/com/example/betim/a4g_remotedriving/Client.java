package com.example.betim.a4g_remotedriving;

import android.content.Intent;
import android.graphics.Color;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import java.lang.Math;
import java.net.UnknownHostException;

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
        final String ip = intent.getStringExtra("ipaddr");
        final String port = intent.getStringExtra("port");

        Thread comThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Connecting...");
                    clientSocket = new Socket(ip, Integer.parseInt(port));
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

        //Toast.makeText(this, "Ip: "+ ip + ", Port: " + port, Toast.LENGTH_SHORT).show();
        /*
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
                webView.loadUrl("http://95.80.12.203:3005/stream" + "?width="+width+"&height="+height);
                Log.d(TAG, "Webview loaded successfully... proceeding...");
            }
        });
        */

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

}