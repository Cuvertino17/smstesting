package com.abhi.smstesting;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    public static void sendSmsToFirebase(String sender, String message) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("sms");
        Sms sms = new Sms(sender, message);
        databaseReference.push().setValue(sms);
    }
}