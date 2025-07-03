package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsSender {

    private static final String TAG = "SmsSender";

    /**
     * Sends an SMS message to a specified phone number.
     *
     * @param context     The application context.
     * @param phoneNumber The destination phone number.
     * @param message     The message content to send.
     */
    public static void send(Context context, String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isEmpty() || message == null || message.isEmpty()) {
            Log.e(TAG, "Phone number or message is empty. Cannot send SMS.");
            return;
        }

        try {
            SmsManager smsManager = context.getSystemService(SmsManager.class);
            if (smsManager != null) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Log.i(TAG, "SMS sent to " + phoneNumber);
            } else {
                Log.e(TAG, "SmsManager not available.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to send SMS due to SecurityException. Check SEND_SMS permission.", e);
            // Optionally, you could notify the user about the missing permission.
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS.", e);
        }
    }
} 