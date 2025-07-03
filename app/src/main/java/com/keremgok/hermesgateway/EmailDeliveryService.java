package com.keremgok.hermesgateway;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Service for sending email notifications
 */
public class EmailDeliveryService {
    private static final String TAG = "EmailDeliveryService";
    private static final String EMAIL_SUBJECT_PREFIX = "[Activity Gateway] ";

    private final Context context;

    public EmailDeliveryService(Context context) {
        this.context = context;
    }

    /**
     * Send email notification using system email client
     */
    public boolean sendEmail(String toEmail, String subject, String message, boolean isHtml) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            Log.e(TAG, "Email address is empty");
            return false;
        }

        if (!isValidEmail(toEmail)) {
            Log.e(TAG, "Invalid email address: " + toEmail);
            return false;
        }

        try {
            String fullSubject = EMAIL_SUBJECT_PREFIX + (subject != null ? subject : "Notification");
            String emailBody = formatMessageForEmail(message, isHtml);

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + toEmail));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, fullSubject);
            
            if (isHtml) {
                emailIntent.putExtra(Intent.EXTRA_TEXT, android.text.Html.fromHtml(emailBody));
            } else {
                emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
            }

            // Add FLAG_ACTIVITY_NEW_TASK to start from service context
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Check if there's an email app available
            if (emailIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(emailIntent);
                Log.d(TAG, "Email intent launched successfully");
                return true;
            } else {
                Log.e(TAG, "No email app found to handle the intent");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to send email: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send email with default settings (plain text, auto-generated subject)
     */
    public boolean sendEmail(String toEmail, String message) {
        return sendEmail(toEmail, "Activity Notification", message, false);
    }

    /**
     * Create email content from activity data
     */
    public String createEmailFromActivityData(String eventType, JSONObject activityData) {
        StringBuilder emailBody = new StringBuilder();
        
        try {
            // Email header
            emailBody.append("<h2>Activity Gateway Notification</h2>\n");
            emailBody.append("<p><strong>Event Type:</strong> ").append(eventType).append("</p>\n");
            emailBody.append("<p><strong>Timestamp:</strong> ").append(getCurrentTimestamp()).append("</p>\n");
            emailBody.append("<hr>\n");

            // Activity specific content
            switch (eventType.toLowerCase()) {
                case "sms":
                case "sms_received":
                    createSmsEmailContent(emailBody, activityData);
                    break;
                case "call":
                case "call_received":
                    createCallEmailContent(emailBody, activityData);
                    break;
                case "push":
                case "push_notification_received":
                    createPushEmailContent(emailBody, activityData);
                    break;
                default:
                    createGenericEmailContent(emailBody, activityData);
                    break;
            }

            // Footer
            emailBody.append("<hr>\n");
            emailBody.append("<p><small>Sent by Android Nomad Gateway</small></p>\n");

        } catch (Exception e) {
            Log.e(TAG, "Error creating email content", e);
            return "Activity notification: " + activityData.toString();
        }

        return emailBody.toString();
    }

    /**
     * Create SMS-specific email content
     */
    private void createSmsEmailContent(StringBuilder emailBody, JSONObject data) throws JSONException {
        emailBody.append("<h3>📱 SMS Message Received</h3>\n");
        
        if (data.has("from")) {
            emailBody.append("<p><strong>From:</strong> ").append(data.getString("from")).append("</p>\n");
        }
        
        if (data.has("text")) {
            emailBody.append("<p><strong>Message:</strong></p>\n");
            emailBody.append("<blockquote>").append(escapeHtml(data.getString("text"))).append("</blockquote>\n");
        }
        
        if (data.has("sim")) {
            emailBody.append("<p><strong>SIM:</strong> ").append(data.getString("sim")).append("</p>\n");
        }
        
        if (data.has("sentStamp")) {
            emailBody.append("<p><strong>Sent:</strong> ").append(formatTimestamp(data.getString("sentStamp"))).append("</p>\n");
        }
    }

    /**
     * Create call-specific email content
     */
    private void createCallEmailContent(StringBuilder emailBody, JSONObject data) throws JSONException {
        emailBody.append("<h3>📞 Incoming Call</h3>\n");
        
        if (data.has("from")) {
            emailBody.append("<p><strong>From:</strong> ").append(data.getString("from")).append("</p>\n");
        }
        
        if (data.has("contact")) {
            String contact = data.getString("contact");
            if (!contact.isEmpty() && !contact.equals("Unknown")) {
                emailBody.append("<p><strong>Contact:</strong> ").append(contact).append("</p>\n");
            }
        }
        
        if (data.has("duration")) {
            emailBody.append("<p><strong>Duration:</strong> ").append(data.getString("duration")).append(" seconds</p>\n");
        }
        
        if (data.has("timestamp")) {
            emailBody.append("<p><strong>Time:</strong> ").append(formatTimestamp(data.getString("timestamp"))).append("</p>\n");
        }
    }

    /**
     * Create push notification-specific email content
     */
    private void createPushEmailContent(StringBuilder emailBody, JSONObject data) throws JSONException {
        emailBody.append("<h3>🔔 Push Notification</h3>\n");
        
        if (data.has("package")) {
            emailBody.append("<p><strong>App:</strong> ").append(data.getString("package")).append("</p>\n");
        }
        
        if (data.has("title")) {
            emailBody.append("<p><strong>Title:</strong> ").append(escapeHtml(data.getString("title"))).append("</p>\n");
        }
        
        if (data.has("content")) {
            emailBody.append("<p><strong>Content:</strong></p>\n");
            emailBody.append("<blockquote>").append(escapeHtml(data.getString("content"))).append("</blockquote>\n");
        }
        
        if (data.has("sentStamp")) {
            emailBody.append("<p><strong>Time:</strong> ").append(formatTimestamp(data.getString("sentStamp"))).append("</p>\n");
        }
    }

    /**
     * Create generic email content for unknown event types
     */
    private void createGenericEmailContent(StringBuilder emailBody, JSONObject data) {
        emailBody.append("<h3>📋 Activity Data</h3>\n");
        try {
            emailBody.append("<pre>").append(escapeHtml(data.toString(2))).append("</pre>\n");
        } catch (Exception e) {
            emailBody.append("<pre>").append(escapeHtml(data.toString())).append("</pre>\n");
        }
    }

    /**
     * Format message for email delivery
     */
    private String formatMessageForEmail(String message, boolean isHtml) {
        if (message == null) return "";
        
        if (isHtml) {
            return message; // Assume it's already properly formatted HTML
        } else {
            // Convert plain text to HTML for better formatting
            return escapeHtml(message).replace("\n", "<br>\n");
        }
    }

    /**
     * Escape HTML characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(ts));
        } catch (NumberFormatException e) {
            return timestamp; // Return as-is if parsing fails
        }
    }

    /**
     * Get current timestamp as formatted string
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Validate email address format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /**
     * Create mailto URL for external use
     */
    public static String createMailtoUrl(String email, String subject, String body) {
        try {
            StringBuilder mailtoUrl = new StringBuilder("mailto:");
            mailtoUrl.append(URLEncoder.encode(email, "UTF-8"));
            mailtoUrl.append("?subject=").append(URLEncoder.encode(subject, "UTF-8"));
            mailtoUrl.append("&body=").append(URLEncoder.encode(body, "UTF-8"));
            return mailtoUrl.toString();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error creating mailto URL", e);
            return "mailto:" + email;
        }
    }

    /**
     * Check if email functionality is available (always true on Android)
     */
    public static boolean isAvailable(Context context) {
        // Email functionality via Intent is always available on Android
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:test@example.com"));
        return emailIntent.resolveActivity(context.getPackageManager()) != null;
    }

    /**
     * Get suggested email apps
     */
    public static String[] getSuggestedEmailApps() {
        return new String[]{
            "Gmail",
            "Outlook", 
            "Yahoo Mail",
            "ProtonMail",
            "System Email Client"
        };
    }
} 
