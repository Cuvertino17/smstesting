package com.abhi.smstesting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "SMS_PREFS";
    private static final String KEY_LAST_SMS = "last_sms";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    String messageBody = smsMessage.getMessageBody();

                    // Check if this SMS has been sent before
                    if (!isDuplicateSMS(context, messageBody)) {
                        // Notify MainActivity of new message
//                        notifyMainActivity(context, messageBody);
                    }
                }
            }
        }
    }

    private boolean isDuplicateSMS(Context context, String message) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastSMS = prefs.getString(KEY_LAST_SMS, "");
        if (lastSMS.equals(message)) {
            // This is a duplicate SMS
            return true;
        } else {
            // Store the current SMS as the last one
            prefs.edit().putString(KEY_LAST_SMS, message).apply();
            return false;
        }
    }

    // Method to notify MainActivity of new message
//    private void notifyMainActivity(Context context, String message) {
//        MainActivity mainActivity = (MainActivity) context;
//        mainActivity.displayMessage(message);
//    }
}
