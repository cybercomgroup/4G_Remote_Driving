package com.example.betim.a4g_remotedriving;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

public class Client extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        Intent intent = getIntent();
        String ip = intent.getStringExtra("ipaddr");
        String port = intent.getStringExtra("port");

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
                webView.loadUrl("http://95.80.12.203:8000/stream" + "?width="+width+"&height="+height);
            }
        });
    }
}
