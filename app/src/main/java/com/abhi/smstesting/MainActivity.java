package com.abhi.smstesting;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private TextView errorTextView;
    private ArrayAdapter<String> adapter;
    private static final int READ_SMS_PERMISSION_CODE = 1;
    private static final int SEND_SMS_PERMISSION_CODE = 2;
    private static final int REQUEST_BATTERY_OPTIMIZATIONS = 3;
    private static final String DESTINATION_NUMBER = "+918761237654"; // Change to your desired number
    private Set<String> fetchedMessageIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseApp.initializeApp(this);
//        listView = findViewById(R.id.listView);
//        errorTextView = findViewById(R.id.errorTextView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);
        checkAndRequestPermissions();

    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS},
                    READ_SMS_PERMISSION_CODE);
        } else {
            checkBatteryOptimizations();

            startSMSService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.SEND_SMS},
                            SEND_SMS_PERMISSION_CODE);
                } else {
                    startSMSService();
                }
            } else {
                Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SEND_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSMSService();
            } else {
                Toast.makeText(this, "Permission denied to send SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSMSService() {
        Intent intent = new Intent(this, SMSService.class);
        startService(intent);
    }
    private void checkBatteryOptimizations() {
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATIONS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BATTERY_OPTIMIZATIONS) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Battery optimization disabled for your app", Toast.LENGTH_SHORT).show();
            } else {
                String reason = "";
                if (data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        reason = uri.toString();
                    }
                }
                Toast.makeText(this, "Battery optimization was not disabled: " + reason, Toast.LENGTH_SHORT).show();
            }
        }
    }




    // Remaining methods for processing and sending SMS messages...
}
