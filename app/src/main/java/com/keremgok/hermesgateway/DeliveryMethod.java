package com.keremgok.hermesgateway;

/**
 * Enum for different delivery methods supported by the gateway
 */
public enum DeliveryMethod {
    HTTP_POST("http_post", "HTTP POST", "Send via HTTP POST webhook"),
    SMS("sms", "SMS", "Send via SMS message"),
    EMAIL("email", "Email", "Send via email");

    private final String key;
    private final String displayName;
    private final String description;

    DeliveryMethod(String key, String displayName, String description) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get DeliveryMethod from key string
     */
    public static DeliveryMethod fromKey(String key) {
        for (DeliveryMethod method : values()) {
            if (method.key.equals(key)) {
                return method;
            }
        }
        return HTTP_POST; // Default fallback
    }

    /**
     * Get all available delivery methods as display names
     */
    public static String[] getDisplayNames() {
        DeliveryMethod[] methods = values();
        String[] names = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            names[i] = methods[i].displayName;
        }
        return names;
    }

    /**
     * Check if this delivery method requires network connectivity
     */
    public boolean requiresNetwork() {
        return this == HTTP_POST || this == EMAIL;
    }

    /**
     * Check if this delivery method requires special permissions
     */
    public boolean requiresSpecialPermissions() {
        return this == SMS; // SMS requires SEND_SMS permission
    }

    /**
     * Get the permission required for this delivery method
     */
    public String getRequiredPermission() {
        switch (this) {
            case SMS:
                return "android.permission.SEND_SMS";
            default:
                return null;
        }
    }

    /**
     * Get character limit for this delivery method
     */
    public int getCharacterLimit() {
        switch (this) {
            case SMS:
                return 160; // Standard SMS limit
            case HTTP_POST:
            case EMAIL:
                return -1; // No practical limit
            default:
                return -1;
        }
    }

    /**
     * Check if this delivery method supports HTML formatting
     */
    public boolean supportsHtmlFormatting() {
        return this == EMAIL;
    }

    /**
     * Get icon resource for this delivery method
     */
    public int getIconResource() {
        switch (this) {
            case HTTP_POST:
                return R.drawable.ic_link;
            case SMS:
                return R.drawable.ic_sms;
            case EMAIL:
                return R.drawable.ic_email; // We'll need to add this icon
            default:
                return R.drawable.ic_link;
        }
    }
} 
