package com.keremgok.hermesgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

/**
 * Manager for tracking delivery statistics across different delivery methods
 */
public class DeliveryStatistics {
    private static final String TAG = "DeliveryStatistics";
    private static final String PREFS_NAME = "delivery_statistics";

    // Webhook statistics keys
    private static final String KEY_WEBHOOK_SUCCESS = "webhook_success_count";
    private static final String KEY_WEBHOOK_FAILURE = "webhook_failure_count";
    private static final String KEY_WEBHOOK_LAST_SUCCESS = "webhook_last_success";
    private static final String KEY_WEBHOOK_LAST_FAILURE = "webhook_last_failure";

    // SMS statistics keys
    private static final String KEY_SMS_SUCCESS = "sms_success_count";
    private static final String KEY_SMS_FAILURE = "sms_failure_count";
    private static final String KEY_SMS_LAST_SUCCESS = "sms_last_success";
    private static final String KEY_SMS_LAST_FAILURE = "sms_last_failure";

    // Email statistics keys
    private static final String KEY_EMAIL_SUCCESS = "email_success_count";
    private static final String KEY_EMAIL_FAILURE = "email_failure_count";
    private static final String KEY_EMAIL_LAST_SUCCESS = "email_last_success";
    private static final String KEY_EMAIL_LAST_FAILURE = "email_last_failure";

    // General statistics
    private static final String KEY_TOTAL_DELIVERIES = "total_deliveries";
    private static final String KEY_FIRST_DELIVERY = "first_delivery_time";
    private static final String KEY_LAST_ACTIVITY = "last_activity_time";

    private final SharedPreferences prefs;
    private final Context context;

    public DeliveryStatistics(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Record successful webhook delivery
     */
    public void recordWebhookSuccess() {
        recordSuccess(KEY_WEBHOOK_SUCCESS, KEY_WEBHOOK_LAST_SUCCESS);
        Log.d(TAG, "Webhook success recorded");
    }

    /**
     * Record failed webhook delivery
     */
    public void recordWebhookFailure() {
        recordFailure(KEY_WEBHOOK_FAILURE, KEY_WEBHOOK_LAST_FAILURE);
        Log.d(TAG, "Webhook failure recorded");
    }

    /**
     * Record successful SMS delivery
     */
    public void recordSmsSuccess() {
        recordSuccess(KEY_SMS_SUCCESS, KEY_SMS_LAST_SUCCESS);
        Log.d(TAG, "SMS success recorded");
    }

    /**
     * Record failed SMS delivery
     */
    public void recordSmsFailure() {
        recordFailure(KEY_SMS_FAILURE, KEY_SMS_LAST_FAILURE);
        Log.d(TAG, "SMS failure recorded");
    }

    /**
     * Record successful email delivery
     */
    public void recordEmailSuccess() {
        recordSuccess(KEY_EMAIL_SUCCESS, KEY_EMAIL_LAST_SUCCESS);
        Log.d(TAG, "Email success recorded");
    }

    /**
     * Record failed email delivery
     */
    public void recordEmailFailure() {
        recordFailure(KEY_EMAIL_FAILURE, KEY_EMAIL_LAST_FAILURE);
        Log.d(TAG, "Email failure recorded");
    }

    /**
     * Record success for any delivery method
     */
    private void recordSuccess(String countKey, String lastTimeKey) {
        long currentTime = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(countKey, prefs.getInt(countKey, 0) + 1);
        editor.putLong(lastTimeKey, currentTime);
        editor.putInt(KEY_TOTAL_DELIVERIES, prefs.getInt(KEY_TOTAL_DELIVERIES, 0) + 1);
        editor.putLong(KEY_LAST_ACTIVITY, currentTime);

        // Set first delivery time if not already set
        if (prefs.getLong(KEY_FIRST_DELIVERY, 0) == 0) {
            editor.putLong(KEY_FIRST_DELIVERY, currentTime);
        }

        editor.apply();

        // Trigger widget update if any widgets are active
        triggerWidgetUpdate();
    }

    /**
     * Record failure for any delivery method
     */
    private void recordFailure(String countKey, String lastTimeKey) {
        long currentTime = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(countKey, prefs.getInt(countKey, 0) + 1);
        editor.putLong(lastTimeKey, currentTime);
        editor.putLong(KEY_LAST_ACTIVITY, currentTime);

        editor.apply();
    }

    /**
     * Get webhook success count
     */
    public int getWebhookSuccessCount() {
        return prefs.getInt(KEY_WEBHOOK_SUCCESS, 0);
    }

    /**
     * Get webhook failure count
     */
    public int getWebhookFailureCount() {
        return prefs.getInt(KEY_WEBHOOK_FAILURE, 0);
    }

    /**
     * Get SMS success count
     */
    public int getSmsSuccessCount() {
        return prefs.getInt(KEY_SMS_SUCCESS, 0);
    }

    /**
     * Get SMS failure count
     */
    public int getSmsFailureCount() {
        return prefs.getInt(KEY_SMS_FAILURE, 0);
    }

    /**
     * Get email success count
     */
    public int getEmailSuccessCount() {
        return prefs.getInt(KEY_EMAIL_SUCCESS, 0);
    }

    /**
     * Get email failure count
     */
    public int getEmailFailureCount() {
        return prefs.getInt(KEY_EMAIL_FAILURE, 0);
    }

    /**
     * Get total successful deliveries across all methods
     */
    public int getTotalSuccessCount() {
        return getWebhookSuccessCount() + getSmsSuccessCount() + getEmailSuccessCount();
    }

    /**
     * Get total failed deliveries across all methods
     */
    public int getTotalFailureCount() {
        return getWebhookFailureCount() + getSmsFailureCount() + getEmailFailureCount();
    }

    /**
     * Get total deliveries attempted
     */
    public int getTotalDeliveries() {
        return prefs.getInt(KEY_TOTAL_DELIVERIES, 0);
    }

    /**
     * Get overall success rate as percentage
     */
    public float getOverallSuccessRate() {
        int total = getTotalSuccessCount() + getTotalFailureCount();
        if (total == 0)
            return 0f;
        return (float) getTotalSuccessCount() / total * 100f;
    }

    /**
     * Get webhook success rate as percentage
     */
    public float getWebhookSuccessRate() {
        int total = getWebhookSuccessCount() + getWebhookFailureCount();
        if (total == 0)
            return 0f;
        return (float) getWebhookSuccessCount() / total * 100f;
    }

    /**
     * Get SMS success rate as percentage
     */
    public float getSmsSuccessRate() {
        int total = getSmsSuccessCount() + getSmsFailureCount();
        if (total == 0)
            return 0f;
        return (float) getSmsSuccessCount() / total * 100f;
    }

    /**
     * Get email success rate as percentage
     */
    public float getEmailSuccessRate() {
        int total = getEmailSuccessCount() + getEmailFailureCount();
        if (total == 0)
            return 0f;
        return (float) getEmailSuccessCount() / total * 100f;
    }

    /**
     * Get last successful webhook delivery time
     */
    public long getLastWebhookSuccess() {
        return prefs.getLong(KEY_WEBHOOK_LAST_SUCCESS, 0);
    }

    /**
     * Get last failed webhook delivery time
     */
    public long getLastWebhookFailure() {
        return prefs.getLong(KEY_WEBHOOK_LAST_FAILURE, 0);
    }

    /**
     * Get last successful SMS delivery time
     */
    public long getLastSmsSuccess() {
        return prefs.getLong(KEY_SMS_LAST_SUCCESS, 0);
    }

    /**
     * Get last failed SMS delivery time
     */
    public long getLastSmsFailure() {
        return prefs.getLong(KEY_SMS_LAST_FAILURE, 0);
    }

    /**
     * Get last successful email delivery time
     */
    public long getLastEmailSuccess() {
        return prefs.getLong(KEY_EMAIL_LAST_SUCCESS, 0);
    }

    /**
     * Get last failed email delivery time
     */
    public long getLastEmailFailure() {
        return prefs.getLong(KEY_EMAIL_LAST_FAILURE, 0);
    }

    /**
     * Get first delivery time
     */
    public long getFirstDeliveryTime() {
        return prefs.getLong(KEY_FIRST_DELIVERY, 0);
    }

    /**
     * Get last activity time
     */
    public long getLastActivityTime() {
        return prefs.getLong(KEY_LAST_ACTIVITY, 0);
    }

    /**
     * Get most active delivery method
     */
    public DeliveryMethod getMostActiveDeliveryMethod() {
        int webhookTotal = getWebhookSuccessCount() + getWebhookFailureCount();
        int smsTotal = getSmsSuccessCount() + getSmsFailureCount();
        int emailTotal = getEmailSuccessCount() + getEmailFailureCount();

        if (webhookTotal >= smsTotal && webhookTotal >= emailTotal) {
            return DeliveryMethod.HTTP_POST;
        } else if (smsTotal >= emailTotal) {
            return DeliveryMethod.SMS;
        } else {
            return DeliveryMethod.EMAIL;
        }
    }

    /**
     * Get statistics summary as JSON
     */
    public JSONObject getStatisticsSummary() {
        try {
            JSONObject summary = new JSONObject();

            // Overall statistics
            summary.put("total_success", getTotalSuccessCount());
            summary.put("total_failure", getTotalFailureCount());
            summary.put("total_deliveries", getTotalDeliveries());
            summary.put("overall_success_rate", getOverallSuccessRate());

            // Webhook statistics
            JSONObject webhook = new JSONObject();
            webhook.put("success_count", getWebhookSuccessCount());
            webhook.put("failure_count", getWebhookFailureCount());
            webhook.put("success_rate", getWebhookSuccessRate());
            webhook.put("last_success", getLastWebhookSuccess());
            webhook.put("last_failure", getLastWebhookFailure());
            summary.put("webhook", webhook);

            // SMS statistics
            JSONObject sms = new JSONObject();
            sms.put("success_count", getSmsSuccessCount());
            sms.put("failure_count", getSmsFailureCount());
            sms.put("success_rate", getSmsSuccessRate());
            sms.put("last_success", getLastSmsSuccess());
            sms.put("last_failure", getLastSmsFailure());
            summary.put("sms", sms);

            // Email statistics
            JSONObject email = new JSONObject();
            email.put("success_count", getEmailSuccessCount());
            email.put("failure_count", getEmailFailureCount());
            email.put("success_rate", getEmailSuccessRate());
            email.put("last_success", getLastEmailSuccess());
            email.put("last_failure", getLastEmailFailure());
            summary.put("email", email);

            // Timing information
            summary.put("first_delivery", getFirstDeliveryTime());
            summary.put("last_activity", getLastActivityTime());
            summary.put("most_active_method", getMostActiveDeliveryMethod().getKey());

            return summary;

        } catch (Exception e) {
            Log.e(TAG, "Error creating statistics summary", e);
            return new JSONObject();
        }
    }

    /**
     * Reset all statistics
     */
    public void resetStatistics() {
        prefs.edit().clear().apply();
        Log.d(TAG, "All delivery statistics reset");
    }

    /**
     * Reset statistics for specific delivery method
     */
    public void resetDeliveryMethodStatistics(DeliveryMethod method) {
        SharedPreferences.Editor editor = prefs.edit();

        switch (method) {
            case HTTP_POST:
                editor.remove(KEY_WEBHOOK_SUCCESS)
                        .remove(KEY_WEBHOOK_FAILURE)
                        .remove(KEY_WEBHOOK_LAST_SUCCESS)
                        .remove(KEY_WEBHOOK_LAST_FAILURE);
                break;

            case SMS:
                editor.remove(KEY_SMS_SUCCESS)
                        .remove(KEY_SMS_FAILURE)
                        .remove(KEY_SMS_LAST_SUCCESS)
                        .remove(KEY_SMS_LAST_FAILURE);
                break;

            case EMAIL:
                editor.remove(KEY_EMAIL_SUCCESS)
                        .remove(KEY_EMAIL_FAILURE)
                        .remove(KEY_EMAIL_LAST_SUCCESS)
                        .remove(KEY_EMAIL_LAST_FAILURE);
                break;
        }

        editor.apply();
        Log.d(TAG, "Statistics reset for delivery method: " + method.getDisplayName());
    }

    /**
     * Get formatted success rate string
     */
    public static String formatSuccessRate(float rate) {
        if (rate == 0f)
            return "No data";
        return String.format("%.1f%%", rate);
    }

    /**
     * Get formatted time string for last activity
     */
    public static String formatLastActivity(long timestamp) {
        if (timestamp == 0)
            return "Never";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) { // Less than 1 minute
            return "Just now";
        } else if (diff < 3600000) { // Less than 1 hour
            return (diff / 60000) + " minutes ago";
        } else if (diff < 86400000) { // Less than 1 day
            return (diff / 3600000) + " hours ago";
        } else {
            return (diff / 86400000) + " days ago";
        }
    }

    /**
     * Trigger widget update if widgets are active
     */
    private void triggerWidgetUpdate() {
        try {
            if (StatisticsWidgetProvider.hasActiveWidgets(context)) {
                StatisticsWidgetProvider.requestWidgetUpdate(context);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error triggering widget update", e);
        }
    }
}
