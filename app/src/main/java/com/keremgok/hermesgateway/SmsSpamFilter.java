package com.keremgok.hermesgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SMS Spam Filter - kullanıcı tanımlı kelime listesi ile spam koruması
 */
public class SmsSpamFilter {

    private static final String TAG = "SmsSpamFilter";
    private static final String PREFS_NAME = "sms_spam_filter";
    private static final String KEY_ENABLED = "spam_filter_enabled";
    private static final String KEY_KEYWORDS = "spam_keywords";
    private static final String KEY_CASE_SENSITIVE = "case_sensitive";
    private static final String KEY_WHOLE_WORD_ONLY = "whole_word_only";
    private static final String KEYWORD_SEPARATOR = ";";

    private Context context;
    private SharedPreferences prefs;

    public SmsSpamFilter(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * SMS metninin spam olup olmadığını kontrol eder
     * 
     * @param messageText kontrol edilecek SMS metni
     * @return true ise spam, false ise spam değil
     */
    public boolean isSpam(String messageText) {
        if (!isEnabled() || TextUtils.isEmpty(messageText)) {
            return false;
        }

        Set<String> keywords = getKeywords();
        if (keywords.isEmpty()) {
            return false;
        }

        String textToCheck = isCaseSensitive() ? messageText : messageText.toLowerCase();

        for (String keyword : keywords) {
            if (TextUtils.isEmpty(keyword)) {
                continue;
            }

            String keywordToCheck = isCaseSensitive() ? keyword : keyword.toLowerCase();

            if (isWholeWordOnly()) {
                if (containsWholeWord(textToCheck, keywordToCheck)) {
                    Log.d(TAG, "Spam detected with keyword: " + keyword);
                    return true;
                }
            } else {
                if (textToCheck.contains(keywordToCheck)) {
                    Log.d(TAG, "Spam detected with keyword: " + keyword);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Tam kelime eşleşmesi kontrolü
     */
    private boolean containsWholeWord(String text, String keyword) {
        String pattern = "\\b" + keyword.replaceAll("([\\[\\]{}()*+?.\\\\^$|])", "\\\\$1") + "\\b";
        return text.matches(".*" + pattern + ".*");
    }

    /**
     * Spam filtresinin aktif olup olmadığını kontrol eder
     */
    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    /**
     * Spam filtresini aktif/pasif yapar
     */
    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        Log.d(TAG, "Spam filter " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Büyük/küçük harf duyarlılığı ayarını döndürür
     */
    public boolean isCaseSensitive() {
        return prefs.getBoolean(KEY_CASE_SENSITIVE, false);
    }

    /**
     * Büyük/küçük harf duyarlılığı ayarını değiştirir
     */
    public void setCaseSensitive(boolean caseSensitive) {
        prefs.edit().putBoolean(KEY_CASE_SENSITIVE, caseSensitive).apply();
    }

    /**
     * Tam kelime eşleşmesi ayarını döndürür
     */
    public boolean isWholeWordOnly() {
        return prefs.getBoolean(KEY_WHOLE_WORD_ONLY, true);
    }

    /**
     * Tam kelime eşleşmesi ayarını değiştirir
     */
    public void setWholeWordOnly(boolean wholeWordOnly) {
        prefs.edit().putBoolean(KEY_WHOLE_WORD_ONLY, wholeWordOnly).apply();
    }

    /**
     * Spam kelimelerini döndürür
     */
    public Set<String> getKeywords() {
        String keywordsString = prefs.getString(KEY_KEYWORDS, "");
        if (TextUtils.isEmpty(keywordsString)) {
            return new HashSet<>();
        }

        String[] keywordArray = keywordsString.split(KEYWORD_SEPARATOR);
        Set<String> keywords = new HashSet<>();
        for (String keyword : keywordArray) {
            String trimmed = keyword.trim();
            if (!TextUtils.isEmpty(trimmed)) {
                keywords.add(trimmed);
            }
        }
        return keywords;
    }

    /**
     * Spam kelimelerini ayarlar
     */
    public void setKeywords(Set<String> keywords) {
        if (keywords == null) {
            keywords = new HashSet<>();
        }

        List<String> keywordList = new ArrayList<>();
        for (String keyword : keywords) {
            String trimmed = keyword.trim();
            if (!TextUtils.isEmpty(trimmed)) {
                keywordList.add(trimmed);
            }
        }

        String keywordsString = TextUtils.join(KEYWORD_SEPARATOR, keywordList);
        prefs.edit().putString(KEY_KEYWORDS, keywordsString).apply();
        Log.d(TAG, "Spam keywords updated: " + keywordList.size() + " keywords");
    }

    /**
     * Yeni bir spam kelimesi ekler
     */
    public void addKeyword(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }

        Set<String> keywords = getKeywords();
        keywords.add(keyword.trim());
        setKeywords(keywords);
    }

    /**
     * Spam kelimesini siler
     */
    public void removeKeyword(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }

        Set<String> keywords = getKeywords();
        keywords.remove(keyword.trim());
        setKeywords(keywords);
    }

    /**
     * Tüm spam kelimelerini temizler
     */
    public void clearKeywords() {
        setKeywords(new HashSet<>());
    }

    /**
     * Spam kelimelerini string olarak döndürür (kullanıcı arayüzü için)
     */
    public String getKeywordsAsString() {
        Set<String> keywords = getKeywords();
        if (keywords.isEmpty()) {
            return "";
        }
        return TextUtils.join(", ", keywords);
    }

    /**
     * String'den spam kelimelerini ayarlar (kullanıcı arayüzü için)
     * Kelimeler virgül, noktalı virgül veya yeni satır ile ayrılabilir
     */
    public void setKeywordsFromString(String keywordsString) {
        if (TextUtils.isEmpty(keywordsString)) {
            clearKeywords();
            return;
        }

        // Virgül, noktalı virgül veya yeni satır ile ayrılmış kelimeleri parse et
        String[] parts = keywordsString.split("[,;\\n\\r]+");
        Set<String> keywords = new HashSet<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (!TextUtils.isEmpty(trimmed)) {
                keywords.add(trimmed);
            }
        }

        setKeywords(keywords);
    }

    /**
     * Spam filtresi istatistiklerini döndürür
     */
    public String getFilterStats() {
        Set<String> keywords = getKeywords();
        return String.format("Aktif: %s, Kelime sayısı: %d, Büyük/küçük harf: %s, Tam kelime: %s",
                isEnabled() ? "Evet" : "Hayır",
                keywords.size(),
                isCaseSensitive() ? "Duyarlı" : "Duyarsız",
                isWholeWordOnly() ? "Evet" : "Hayır");
    }

    /**
     * Varsayılan spam kelimelerini yükler (Türkçe)
     */
    public void loadDefaultKeywords() {
        Set<String> defaultKeywords = new HashSet<>(Arrays.asList(
                "kredi", "borç", "faiz", "taksit", "ödeme", "promosyon", "kampanya",
                "hediye", "kazandınız", "tebrikler", "ücretsiz", "bedava", "indirim",
                "bonus", "çekiliş", "şans", "kazanç", "para", "dolar", "euro",
                "yatırım", "bitcoin", "forex", "bahis", "kumar", "şans oyunu",
                "reklam", "tanıtım", "satış", "ürün", "hizmet", "abonelik",
                "iptal", "durdur", "STOP", "RED", "hayır", "istemiyorum"));

        setKeywords(defaultKeywords);
        Log.d(TAG, "Default spam keywords loaded: " + defaultKeywords.size() + " keywords");
    }

    /**
     * Test amaçlı spam kontrolü
     */
    public static void testSpamFilter(Context context) {
        SmsSpamFilter filter = new SmsSpamFilter(context);

        // Test mesajları
        String[] testMessages = {
                "Merhaba, nasılsın?",
                "Tebrikler! 1000 TL kazandınız. Hemen çekin!",
                "Kredi başvurunuz onaylandı. Faiz oranları çok uygun.",
                "Toplantı yarın saat 14:00'da",
                "Ücretsiz hediye kazandınız! Şimdi al!",
                "Bitcoin yatırımı ile zengin olun!"
        };

        Log.d(TAG, "Testing spam filter...");
        for (String message : testMessages) {
            boolean isSpam = filter.isSpam(message);
            Log.d(TAG, "Message: \"" + message + "\" -> " + (isSpam ? "SPAM" : "OK"));
        }
    }
}
