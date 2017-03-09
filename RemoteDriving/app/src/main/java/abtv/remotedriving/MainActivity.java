package abtv.remotedriving;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/*
 * Written by Albin Hellqvist, Betim Raqi,
 * Tasdikul Huda and Victor Christoffersson
 */
public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        putLatestCon();
    }

    public void connect(View view) {
        Intent intent = new Intent("com.example.abtv.remote_driving.Client");

        EditText ipField = (EditText)findViewById(R.id.ip_addr);
        EditText portField = (EditText)findViewById(R.id.port_nr);
        String ip = ipField.getText().toString();
        String port = portField.getText().toString();

        storeConInfo(ip, port);

        intent.putExtra("ip_addr", ip);
        intent.putExtra("port", port);

        startActivity(intent);
    }

    public void storeConInfo(String ip, String port){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("ip", ip);
        editor.putString("port", port);
        editor.commit();
    }

    public void putLatestCon(){
        String ip = sharedPref.getString("ip", "");
        String port = sharedPref.getString("port", "");

        EditText ipField = (EditText) findViewById(R.id.ip_addr);
        EditText portField = (EditText) findViewById(R.id.port_nr);

        ipField.setText(ip);
        portField.setText(port);
    }
}
