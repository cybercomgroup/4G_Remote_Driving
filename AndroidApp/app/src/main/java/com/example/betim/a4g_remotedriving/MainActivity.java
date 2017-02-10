package com.example.betim.a4g_remotedriving;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.view.MenuItem;
import android.view.View;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void connect(View view) {
        Intent intent = new Intent("com.example.betim.a4g_remotedriving.Client");

        EditText ipField = (EditText) findViewById(R.id.ip_addr);
        EditText portField = (EditText) findViewById(R.id.port_nr);

        String ip = ipField.getText().toString();
        String port = portField.getText().toString();

        intent.putExtra("ipaddr",ip);
        intent.putExtra("port",port);

        startActivity(intent);
    }
}
