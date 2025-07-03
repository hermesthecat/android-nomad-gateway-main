package tech.wdg.incomingactivitygateway;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for sending SMS notifications
 */
public class SmsDeliveryService {
    private static final String TAG = "SmsDeliveryService";
    private static final int SMS_CHARACTER_LIMIT = 160;
    private static final int LONG_SMS_PART_LIMIT = 153; // For multi-part SMS

    private final Context context;

    public SmsDeliveryService(Context context) {
        this.context = context;
    }

    /**
     * Send SMS notification
     */
    public boolean sendSms(String phoneNumber, String message, int simSlot) {
        if (!hasPermission()) {
            Log.e(TAG, "SMS permission not granted");
            return false;
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.e(TAG, "Phone number is empty");
            return false;
        }

        if (message == null || message.trim().isEmpty()) {
            Log.e(TAG, "Message is empty");
            return false;
        }

        try {
            // Clean phone number
            String cleanPhoneNumber = cleanPhoneNumber(phoneNumber);
            
            // Truncate message if too long for SMS
            String smsMessage = formatMessageForSms(message);
            
            Log.d(TAG, "Sending SMS to " + cleanPhoneNumber + " via SIM " + simSlot);
            Log.d(TAG, "Message: " + smsMessage);

            SmsManager smsManager = getSmsManager(simSlot);
            
            // Check if message needs to be split
            if (smsMessage.length() <= SMS_CHARACTER_LIMIT) {
                // Single SMS
                smsManager.sendTextMessage(cleanPhoneNumber, null, smsMessage, null, null);
                Log.d(TAG, "Single SMS sent successfully");
            } else {
                // Multi-part SMS
                ArrayList<String> parts = smsManager.divideMessage(smsMessage);
                smsManager.sendMultipartTextMessage(cleanPhoneNumber, null, parts, null, null);
                Log.d(TAG, "Multi-part SMS sent successfully (" + parts.size() + " parts)");
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send SMS using default SIM
     */
    public boolean sendSms(String phoneNumber, String message) {
        return sendSms(phoneNumber, message, 0); // Use default SIM
    }

    /**
     * Get SMS manager for specific SIM slot
     */
    private SmsManager getSmsManager(int simSlot) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                    
                    if (subscriptionInfos != null && simSlot > 0 && simSlot <= subscriptionInfos.size()) {
                        SubscriptionInfo subscriptionInfo = subscriptionInfos.get(simSlot - 1);
                        return SmsManager.getSmsManagerForSubscriptionId(subscriptionInfo.getSubscriptionId());
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to get SMS manager for SIM slot " + simSlot + ", using default", e);
            }
        }
        
        // Fallback to default SMS manager
        return SmsManager.getDefault();
    }

    /**
     * Clean and format phone number for SMS
     */
    private String cleanPhoneNumber(String phoneNumber) {
        // Remove any non-digit characters except + at the beginning
        String cleaned = phoneNumber.trim().replaceAll("[^+0-9]", "");
        
        // Ensure it starts with + for international format if it's a long number
        if (!cleaned.startsWith("+") && cleaned.length() > 10) {
            cleaned = "+" + cleaned;
        }
        
        return cleaned;
    }

    /**
     * Format message for SMS delivery
     */
    private String formatMessageForSms(String message) {
        if (message == null) return "";
        
        // Remove HTML tags if any
        String cleanMessage = message.replaceAll("<[^>]*>", "");
        
        // Replace common HTML entities
        cleanMessage = cleanMessage
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        
        // Add prefix to identify the source
        String prefix = "[AG] "; // Activity Gateway prefix
        
        // Calculate available space for content
        int maxContentLength = (SMS_CHARACTER_LIMIT * 3) - prefix.length(); // Allow up to 3 SMS parts
        
        if (cleanMessage.length() > maxContentLength) {
            cleanMessage = cleanMessage.substring(0, maxContentLength - 3) + "...";
        }
        
        return prefix + cleanMessage;
    }

    /**
     * Check if SMS permission is granted
     */
    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if SMS functionality is available
     */
    public static boolean isAvailable(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    /**
     * Get available SIM slots for SMS
     */
    public static List<String> getAvailableSimSlots(Context context) {
        List<String> simSlots = new ArrayList<>();
        simSlots.add("Default SIM");
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                    
                    if (subscriptionInfos != null) {
                        for (int i = 0; i < subscriptionInfos.size(); i++) {
                            SubscriptionInfo info = subscriptionInfos.get(i);
                            String displayName = info.getDisplayName() != null ? 
                                info.getDisplayName().toString() : "SIM " + (i + 1);
                            simSlots.add(displayName);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to get SIM slot information", e);
            }
        }
        
        return simSlots;
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String cleaned = phoneNumber.trim().replaceAll("[^+0-9]", "");
        
        // Must have at least 10 digits (local numbers) or start with + for international
        return cleaned.length() >= 10 && (cleaned.startsWith("+") || cleaned.length() <= 15);
    }

    /**
     * Estimate SMS parts needed for message
     */
    public static int estimateSmsPartsNeeded(String message) {
        if (message == null || message.isEmpty()) {
            return 0;
        }
        
        if (message.length() <= SMS_CHARACTER_LIMIT) {
            return 1;
        }
        
        return (int) Math.ceil((double) message.length() / LONG_SMS_PART_LIMIT);
    }
} 