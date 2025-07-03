package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Router for delivering messages through different delivery methods
 */
public class DeliveryRouter {
    private static final String TAG = "DeliveryRouter";

    private final Context context;
    private final SmsDeliveryService smsService;
    private final EmailDeliveryService emailService;
    private final SmtpEmailDeliveryService smtpEmailService;

    public DeliveryRouter(Context context) {
        this.context = context;
        this.smsService = new SmsDeliveryService(context);
        this.emailService = new EmailDeliveryService(context);
        this.smtpEmailService = new SmtpEmailDeliveryService(context);
    }

    /**
     * Route message delivery based on ForwardingConfig
     */
    public boolean routeDelivery(ForwardingConfig config, String eventType, JSONObject messageData) {
        if (config == null) {
            Log.e(TAG, "ForwardingConfig is null");
            return false;
        }

        if (!config.isOn) {
            Log.d(TAG, "ForwardingConfig is disabled, skipping delivery");
            return true; // Not an error, just disabled
        }

        DeliveryMethod deliveryMethod = config.getDeliveryMethod();
        
        Log.d(TAG, "Routing delivery via " + deliveryMethod.getDisplayName() + " for event: " + eventType);

        switch (deliveryMethod) {
            case HTTP_POST:
                return routeHttpDelivery(config, messageData);
                
            case SMS:
                return routeSmsDelivery(config, eventType, messageData);
                
            case EMAIL:
                return routeEmailDelivery(config, eventType, messageData);
                
            default:
                Log.e(TAG, "Unknown delivery method: " + deliveryMethod);
                return false;
        }
    }

    /**
     * Route HTTP POST delivery (existing webhook system)
     */
    private boolean routeHttpDelivery(ForwardingConfig config, JSONObject messageData) {
        try {
            String message = config.prepareEnhancedMessage(
                messageData.optString("from", ""),
                messageData.optString("text", messageData.toString()),
                messageData.optString("sim", "unknown"),
                messageData.optLong("timestamp", System.currentTimeMillis())
            );

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            Data data = new Data.Builder()
                    .putString(RequestWorker.DATA_URL, config.getUrl())
                    .putString(RequestWorker.DATA_TEXT, message)
                    .putString(RequestWorker.DATA_HEADERS, config.getHeaders())
                    .putBoolean(RequestWorker.DATA_IGNORE_SSL, config.getIgnoreSsl())
                    .putBoolean(RequestWorker.DATA_CHUNKED_MODE, config.getChunkedMode())
                    .putInt(RequestWorker.DATA_MAX_RETRIES, config.getRetriesNumber())
                    .build();

            WorkRequest workRequest = new OneTimeWorkRequest.Builder(RequestWorker.class)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);
            
            Log.d(TAG, "HTTP POST delivery queued successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to route HTTP delivery", e);
            return false;
        }
    }

    /**
     * Route SMS delivery
     */
    private boolean routeSmsDelivery(ForwardingConfig config, String eventType, JSONObject messageData) {
        try {
            String phoneNumber = config.getSmsPhoneNumber();
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                Log.e(TAG, "SMS phone number not configured");
                return false;
            }

            // Create SMS-friendly message
            String smsMessage = createSmsMessage(eventType, messageData);
            
            // Use configured SIM slot or default
            int simSlot = config.getSimSlot();
            
            // Send SMS using the service
            boolean success = smsService.sendSms(phoneNumber, smsMessage, simSlot);
            
            if (success) {
                Log.d(TAG, "SMS delivery completed successfully");
            } else {
                Log.e(TAG, "SMS delivery failed");
            }
            
            return success;

        } catch (Exception e) {
            Log.e(TAG, "Failed to route SMS delivery", e);
            return false;
        }
    }

    /**
     * Route email delivery using SMTP
     */
    private boolean routeEmailDelivery(ForwardingConfig config, String eventType, JSONObject messageData) {
        try {
            // Check if SMTP is configured
            if (SmtpEmailDeliveryService.isSmtpConfigured(config)) {
                return routeSmtpEmailDelivery(config, eventType, messageData);
            } else {
                // Fallback to system email client
                return routeSystemEmailDelivery(config, eventType, messageData);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to route email delivery", e);
            return false;
        }
    }
    
    /**
     * Route SMTP email delivery (automatic sending)
     */
    private boolean routeSmtpEmailDelivery(ForwardingConfig config, String eventType, JSONObject messageData) {
        try {
            String subject = createEmailSubject(eventType, messageData);
            String htmlContent = smtpEmailService.createHtmlEmailFromActivityData(eventType, messageData);
            
            // Send email asynchronously using SMTP
            smtpEmailService.sendEmailAsync(config, subject, htmlContent, new SmtpEmailDeliveryService.EmailCallback() {
                @Override
                public void onSuccess(String messageId) {
                    Log.d(TAG, "SMTP email sent successfully. Message ID: " + messageId);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "SMTP email delivery failed: " + error);
                }
            });
            
            Log.d(TAG, "SMTP email delivery initiated successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to route SMTP email delivery", e);
            return false;
        }
    }
    
    /**
     * Route system email delivery (fallback - opens email client)
     */
    private boolean routeSystemEmailDelivery(ForwardingConfig config, String eventType, JSONObject messageData) {
        try {
            String emailAddress = config.getEmailAddress();
            if (emailAddress == null || emailAddress.trim().isEmpty()) {
                Log.e(TAG, "Email address not configured");
                return false;
            }

            // Create email subject and content
            String subject = createEmailSubject(eventType, messageData);
            String emailContent = emailService.createEmailFromActivityData(eventType, messageData);
            
            // Send email using system email client
            boolean success = emailService.sendEmail(emailAddress, subject, emailContent, true);
            
            if (success) {
                Log.d(TAG, "System email delivery initiated successfully");
            } else {
                Log.e(TAG, "System email delivery failed");
            }
            
            return success;

        } catch (Exception e) {
            Log.e(TAG, "Failed to route system email delivery", e);
            return false;
        }
    }

    /**
     * Create SMS-friendly message content
     */
    private String createSmsMessage(String eventType, JSONObject messageData) {
        StringBuilder smsContent = new StringBuilder();
        
        try {
            switch (eventType.toLowerCase()) {
                case "sms":
                case "sms_received":
                    smsContent.append("SMS from ").append(messageData.optString("from", "Unknown"));
                    String text = messageData.optString("text", "");
                    if (!text.isEmpty()) {
                        smsContent.append(": ").append(text);
                    }
                    break;
                    
                case "call":
                case "call_received":
                    smsContent.append("Call from ").append(messageData.optString("from", "Unknown"));
                    String contact = messageData.optString("contact", "");
                    if (!contact.isEmpty() && !contact.equals("Unknown")) {
                        smsContent.append(" (").append(contact).append(")");
                    }
                    break;
                    
                case "push":
                case "push_notification_received":
                    String appPackage = messageData.optString("package", "Unknown App");
                    String title = messageData.optString("title", "");
                    String content = messageData.optString("content", "");
                    
                    smsContent.append("Notification from ").append(appPackage);
                    if (!title.isEmpty()) {
                        smsContent.append(": ").append(title);
                    }
                    if (!content.isEmpty() && !content.equals(title)) {
                        smsContent.append(" - ").append(content);
                    }
                    break;
                    
                default:
                    smsContent.append("Activity: ").append(eventType);
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error creating SMS message, using fallback", e);
            smsContent.setLength(0);
            smsContent.append("Activity notification: ").append(eventType);
        }
        
        return smsContent.toString();
    }

    /**
     * Create email subject line
     */
    private String createEmailSubject(String eventType, JSONObject messageData) {
        try {
            switch (eventType.toLowerCase()) {
                case "sms":
                case "sms_received":
                    return "SMS from " + messageData.optString("from", "Unknown");
                    
                case "call":
                case "call_received":
                    String contact = messageData.optString("contact", "");
                    String from = messageData.optString("from", "Unknown");
                    if (!contact.isEmpty() && !contact.equals("Unknown")) {
                        return "Call from " + contact + " (" + from + ")";
                    } else {
                        return "Call from " + from;
                    }
                    
                case "push":
                case "push_notification_received":
                    String title = messageData.optString("title", "");
                    String appPackage = messageData.optString("package", "Unknown App");
                    if (!title.isEmpty()) {
                        return "Notification: " + title + " (" + appPackage + ")";
                    } else {
                        return "Notification from " + appPackage;
                    }
                    
                default:
                    return "Activity: " + eventType;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error creating email subject, using fallback", e);
            return "Activity Notification";
        }
    }

    /**
     * Check if delivery method is available and properly configured
     */
    public static boolean isDeliveryMethodAvailable(Context context, DeliveryMethod method, ForwardingConfig config) {
        switch (method) {
            case HTTP_POST:
                return config.getUrl() != null && !config.getUrl().trim().isEmpty();
                
            case SMS:
                return SmsDeliveryService.isAvailable(context) 
                    && config.getSmsPhoneNumber() != null 
                    && !config.getSmsPhoneNumber().trim().isEmpty()
                    && SmsDeliveryService.isValidPhoneNumber(config.getSmsPhoneNumber());
                
            case EMAIL:
                return EmailDeliveryService.isAvailable(context)
                    && config.getEmailAddress() != null
                    && !config.getEmailAddress().trim().isEmpty()
                    && EmailDeliveryService.isValidEmail(config.getEmailAddress());
                    
            default:
                return false;
        }
    }

    /**
     * Get delivery method requirements for UI display
     */
    public static String getDeliveryMethodRequirements(DeliveryMethod method) {
        switch (method) {
            case HTTP_POST:
                return "Requires: Webhook URL, Internet connection";
                
            case SMS:
                return "Requires: Phone number, SMS permission, SIM card";
                
            case EMAIL:
                return "Requires: Email address, Email app installed";
                
            default:
                return "Unknown requirements";
        }
    }
} 