package com.abhi.smstesting;

//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QuerySnapshot;

public class Sms {

    private static final String TAG = "Sms";

    private String sender;
    private String message;

    public Sms() {
        fetchDataFromFirestore();
        // Default constructor required for Firebase
    }

    public Sms(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Method to fetch data from Firestore
    public static void fetchDataFromFirestore() {
        System.out.println("im inside firestore  ");

//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        db.collection("sms")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            System.out.println("true");
//                            System.out.println("im inside firestore and its fetching data");
//                            for (DocumentSnapshot document : task.getResult()) {
//                                // Print raw data from document
//                                System.out.println("Raw Data: " + document.getData());
//                            }
//                        } else {
//                            System.out.println("im inside firestore and its not fetching data");
//
//                            System.out.println("Error getting documents: " + task.getException());
//                        }
//                    }
//                });
    }
}
