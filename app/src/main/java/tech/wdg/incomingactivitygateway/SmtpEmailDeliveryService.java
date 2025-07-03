package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Advanced SMTP Email Delivery Service for automatic email sending
 */
public class SmtpEmailDeliveryService {
    private static final String TAG = "SmtpEmailDelivery";
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    private final Context context;

    public interface EmailCallback {
        void onSuccess(String messageId);
        void onError(String error);
    }

    public SmtpEmailDeliveryService(Context context) {
        this.context = context;
    }

    /**
     * Send email using SMTP configuration from ForwardingConfig
     */
    public void sendEmailAsync(ForwardingConfig config, String subject, String htmlContent, EmailCallback callback) {
        executor.execute(() -> {
            try {
                String messageId = sendEmailSync(config, subject, htmlContent);
                if (callback != null) {
                    callback.onSuccess(messageId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send email", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Send email synchronously using SMTP
     */
    public String sendEmailSync(ForwardingConfig config, String subject, String htmlContent) throws MessagingException {
        // Validate SMTP configuration
        if (!isSmtpConfigured(config)) {
            throw new MessagingException("SMTP configuration is incomplete");
        }

        // Create SMTP properties
        Properties props = createSmtpProperties(config);
        
        // Create session with authentication
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    config.getSmtpUsername(), 
                    config.getSmtpPassword()
                );
            }
        });

        // Enable debug if needed
        if (BuildConfig.DEBUG) {
            session.setDebug(true);
        }

        // Create message
        MimeMessage message = new MimeMessage(session);
        
        // Set sender
        String fromEmail = config.getSmtpFromEmail();
        String fromName = config.getSmtpFromName();
        if (fromName != null && !fromName.trim().isEmpty()) {
            message.setFrom(new InternetAddress(fromEmail, fromName));
        } else {
            message.setFrom(new InternetAddress(fromEmail));
        }
        
        // Set recipient
        message.setRecipients(Message.RecipientType.TO, 
            InternetAddress.parse(config.getEmailAddress()));
        
        // Set subject with prefix
        message.setSubject("[Activity Gateway] " + subject);
        
        // Set content
        message.setContent(htmlContent, "text/html; charset=utf-8");
        
        // Set headers
        message.setHeader("X-Mailer", "Android Nomad Gateway");
        message.setHeader("X-Priority", "3"); // Normal priority
        
        // Send message
        Transport.send(message);
        
        String messageId = message.getMessageID();
        Log.d(TAG, "Email sent successfully. Message ID: " + messageId);
        
        return messageId;
    }

    /**
     * Create SMTP properties based on configuration
     */
    private Properties createSmtpProperties(ForwardingConfig config) {
        Properties props = new Properties();
        
        String host = config.getSmtpHost();
        int port = config.getSmtpPort();
        boolean useTls = config.getSmtpUseTls();
        boolean useSsl = config.getSmtpUseSsl();
        
        // Basic SMTP properties
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        
        // SSL/TLS configuration
        if (useSsl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        } else if (useTls) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        
        // Connection settings
        props.put("mail.smtp.connectiontimeout", "30000"); // 30 seconds
        props.put("mail.smtp.timeout", "30000"); // 30 seconds
        props.put("mail.smtp.writetimeout", "30000"); // 30 seconds
        
        // Trust settings for self-signed certificates (if needed)
        if (useSsl || useTls) {
            props.put("mail.smtp.ssl.trust", host);
        }
        
        Log.d(TAG, "SMTP Properties: Host=" + host + ", Port=" + port + 
              ", TLS=" + useTls + ", SSL=" + useSsl);
        
        return props;
    }

    /**
     * Create HTML email content from activity data
     */
    public String createHtmlEmailFromActivityData(String eventType, JSONObject activityData) {
        StringBuilder htmlBody = new StringBuilder();
        
        try {
            // Start HTML
            htmlBody.append("<!DOCTYPE html>\n");
            htmlBody.append("<html><head>");
            htmlBody.append("<meta charset=\"UTF-8\">");
            htmlBody.append("<style>");
            htmlBody.append("body { font-family: Arial, sans-serif; margin: 20px; }");
            htmlBody.append(".header { background-color: #2196F3; color: white; padding: 15px; border-radius: 5px; }");
            htmlBody.append(".content { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 10px 0; }");
            htmlBody.append(".footer { color: #666; font-size: 12px; margin-top: 20px; }");
            htmlBody.append(".highlight { background-color: #fff3cd; padding: 10px; border-left: 4px solid #ffc107; }");
            htmlBody.append("</style>");
            htmlBody.append("</head><body>");
            
            // Header
            htmlBody.append("<div class=\"header\">");
            htmlBody.append("<h2>🚨 Activity Gateway Notification</h2>");
            htmlBody.append("<p><strong>Event Type:</strong> ").append(eventType).append("</p>");
            htmlBody.append("<p><strong>Timestamp:</strong> ").append(getCurrentTimestamp()).append("</p>");
            htmlBody.append("</div>");
            
            // Content based on event type
            htmlBody.append("<div class=\"content\">");
            switch (eventType.toLowerCase()) {
                case "sms":
                case "sms_received":
                    createSmsHtmlContent(htmlBody, activityData);
                    break;
                case "call":
                case "call_received":
                    createCallHtmlContent(htmlBody, activityData);
                    break;
                case "push":
                case "push_notification_received":
                    createPushHtmlContent(htmlBody, activityData);
                    break;
                default:
                    createGenericHtmlContent(htmlBody, activityData);
                    break;
            }
            htmlBody.append("</div>");
            
            // Footer
            htmlBody.append("<div class=\"footer\">");
            htmlBody.append("<hr>");
            htmlBody.append("<p>Sent by <strong>Android Nomad Gateway</strong></p>");
            htmlBody.append("<p>Device: ").append(android.os.Build.MODEL).append("</p>");
            htmlBody.append("</div>");
            
            htmlBody.append("</body></html>");
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating HTML email content", e);
            return "<html><body><h3>Activity Notification</h3><pre>" + 
                   escapeHtml(activityData.toString()) + "</pre></body></html>";
        }
        
        return htmlBody.toString();
    }

    /**
     * Create SMS-specific HTML content
     */
    private void createSmsHtmlContent(StringBuilder htmlBody, JSONObject data) {
        try {
            htmlBody.append("<h3>📱 SMS Message Received</h3>");
            
            if (data.has("from")) {
                htmlBody.append("<p><strong>From:</strong> ").append(escapeHtml(data.getString("from"))).append("</p>");
            }
            
            if (data.has("text")) {
                htmlBody.append("<div class=\"highlight\">");
                htmlBody.append("<h4>Message Content:</h4>");
                htmlBody.append("<p>").append(escapeHtml(data.getString("text"))).append("</p>");
                htmlBody.append("</div>");
            }
            
            if (data.has("sim")) {
                htmlBody.append("<p><strong>SIM:</strong> ").append(escapeHtml(data.getString("sim"))).append("</p>");
            }
            
            if (data.has("sentStamp")) {
                htmlBody.append("<p><strong>Sent:</strong> ").append(formatTimestamp(data.getString("sentStamp"))).append("</p>");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating SMS HTML content", e);
        }
    }

    /**
     * Create call-specific HTML content
     */
    private void createCallHtmlContent(StringBuilder htmlBody, JSONObject data) {
        try {
            htmlBody.append("<h3>📞 Incoming Call</h3>");
            
            if (data.has("from")) {
                htmlBody.append("<p><strong>From:</strong> ").append(escapeHtml(data.getString("from"))).append("</p>");
            }
            
            if (data.has("contact")) {
                String contact = data.getString("contact");
                if (!contact.isEmpty() && !contact.equals("Unknown")) {
                    htmlBody.append("<p><strong>Contact:</strong> ").append(escapeHtml(contact)).append("</p>");
                }
            }
            
            if (data.has("timestamp")) {
                htmlBody.append("<p><strong>Time:</strong> ").append(formatTimestamp(data.getString("timestamp"))).append("</p>");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating call HTML content", e);
        }
    }

    /**
     * Create push notification-specific HTML content
     */
    private void createPushHtmlContent(StringBuilder htmlBody, JSONObject data) {
        try {
            htmlBody.append("<h3>🔔 Push Notification</h3>");
            
            if (data.has("package")) {
                htmlBody.append("<p><strong>App:</strong> ").append(escapeHtml(data.getString("package"))).append("</p>");
            }
            
            if (data.has("title")) {
                htmlBody.append("<p><strong>Title:</strong> ").append(escapeHtml(data.getString("title"))).append("</p>");
            }
            
            if (data.has("content")) {
                htmlBody.append("<div class=\"highlight\">");
                htmlBody.append("<h4>Notification Content:</h4>");
                htmlBody.append("<p>").append(escapeHtml(data.getString("content"))).append("</p>");
                htmlBody.append("</div>");
            }
            
            if (data.has("sentStamp")) {
                htmlBody.append("<p><strong>Time:</strong> ").append(formatTimestamp(data.getString("sentStamp"))).append("</p>");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating push HTML content", e);
        }
    }

    /**
     * Create generic HTML content
     */
    private void createGenericHtmlContent(StringBuilder htmlBody, JSONObject data) {
        htmlBody.append("<h3>📋 Activity Data</h3>");
        htmlBody.append("<pre style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto;\">");
        htmlBody.append(escapeHtml(data.toString(2)));
        htmlBody.append("</pre>");
    }

    /**
     * Check if SMTP is properly configured
     */
    public static boolean isSmtpConfigured(ForwardingConfig config) {
        return config.getSmtpHost() != null && !config.getSmtpHost().trim().isEmpty() &&
               config.getSmtpUsername() != null && !config.getSmtpUsername().trim().isEmpty() &&
               config.getSmtpPassword() != null && !config.getSmtpPassword().trim().isEmpty() &&
               config.getSmtpFromEmail() != null && !config.getSmtpFromEmail().trim().isEmpty() &&
               config.getEmailAddress() != null && !config.getEmailAddress().trim().isEmpty();
    }

    /**
     * Get common SMTP presets for popular email providers
     */
    public static SmtpPreset[] getSmtpPresets() {
        return new SmtpPreset[] {
            new SmtpPreset("Gmail", "smtp.gmail.com", 587, true, false,
                "Use App Password instead of regular password"),
            new SmtpPreset("Outlook/Hotmail", "smtp-mail.outlook.com", 587, true, false,
                "Use account password or app password"),
            new SmtpPreset("Yahoo Mail", "smtp.mail.yahoo.com", 587, true, false,
                "Use App Password instead of regular password"),
            new SmtpPreset("ProtonMail", "smtp.protonmail.com", 587, true, false,
                "Requires ProtonMail Bridge for SMTP"),
            new SmtpPreset("Apple iCloud", "smtp.mail.me.com", 587, true, false,
                "Use App-Specific Password"),
            new SmtpPreset("Custom SMTP", "", 587, true, false,
                "Enter your custom SMTP settings")
        };
    }

    /**
     * SMTP Preset class for popular email providers
     */
    public static class SmtpPreset {
        public final String name;
        public final String host;
        public final int port;
        public final boolean useTls;
        public final boolean useSsl;
        public final String note;

        public SmtpPreset(String name, String host, int port, boolean useTls, boolean useSsl, String note) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.useTls = useTls;
            this.useSsl = useSsl;
            this.note = note;
        }
    }

    /**
     * Utility methods
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private String formatTimestamp(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                java.util.Locale.getDefault()).format(new java.util.Date(ts));
        } catch (NumberFormatException e) {
            return timestamp;
        }
    }

    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
            java.util.Locale.getDefault()).format(new java.util.Date());
    }

    /**
     * Test SMTP connection
     */
    public void testSmtpConnection(ForwardingConfig config, EmailCallback callback) {
        executor.execute(() -> {
            try {
                // Create properties and session
                Properties props = createSmtpProperties(config);
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                            config.getSmtpUsername(), 
                            config.getSmtpPassword()
                        );
                    }
                });
                
                // Test connection
                Transport transport = session.getTransport("smtp");
                transport.connect();
                transport.close();
                
                Log.d(TAG, "SMTP connection test successful");
                if (callback != null) {
                    callback.onSuccess("Connection successful");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "SMTP connection test failed", e);
                if (callback != null) {
                    callback.onError("Connection failed: " + e.getMessage());
                }
            }
        });
    }
}
