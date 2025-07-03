package com.keremgok.hermesgateway;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SMS Spam Filter ayarları aktivitesi
 */
public class SmsSpamFilterActivity extends AppCompatActivity {

    private SmsSpamFilter spamFilter;
    private Switch switchEnabled;
    private CheckBox checkBoxCaseSensitive;
    private CheckBox checkBoxWholeWord;
    private EditText editTextKeywords;
    private TextView textViewStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_spam_filter);

        // Initialize spam filter
        spamFilter = new SmsSpamFilter(this);

        // Initialize views
        initializeViews();

        // Load current settings
        loadSettings();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        switchEnabled = findViewById(R.id.switch_spam_filter_enabled);
        checkBoxCaseSensitive = findViewById(R.id.checkbox_case_sensitive);
        checkBoxWholeWord = findViewById(R.id.checkbox_whole_word);
        editTextKeywords = findViewById(R.id.edittext_keywords);
        textViewStats = findViewById(R.id.textview_stats);
    }

    private void loadSettings() {
        switchEnabled.setChecked(spamFilter.isEnabled());
        checkBoxCaseSensitive.setChecked(spamFilter.isCaseSensitive());
        checkBoxWholeWord.setChecked(spamFilter.isWholeWordOnly());
        editTextKeywords.setText(spamFilter.getKeywordsAsString());
        updateStatsDisplay();
    }

    private void setupListeners() {
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spamFilter.setEnabled(isChecked);
            updateStatsDisplay();
            showToast(isChecked ? "Spam filtresi açıldı" : "Spam filtresi kapatıldı");
        });

        checkBoxCaseSensitive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spamFilter.setCaseSensitive(isChecked);
            updateStatsDisplay();
        });

        checkBoxWholeWord.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spamFilter.setWholeWordOnly(isChecked);
            updateStatsDisplay();
        });

        // Keywords değişikliklerini otomatik kaydet
        editTextKeywords.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveKeywords();
            }
        });
    }

    private void saveKeywords() {
        String keywordsText = editTextKeywords.getText().toString();
        spamFilter.setKeywordsFromString(keywordsText);
        updateStatsDisplay();
    }

    private void updateStatsDisplay() {
        textViewStats.setText(spamFilter.getFilterStats());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.spam_filter_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save) {
            saveSettings();
            return true;
        } else if (id == R.id.action_load_defaults) {
            loadDefaultKeywords();
            return true;
        } else if (id == R.id.action_clear_keywords) {
            clearKeywords();
            return true;
        } else if (id == R.id.action_test_filter) {
            testSpamFilter();
            return true;
        } else if (id == R.id.action_add_keyword) {
            showAddKeywordDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveSettings() {
        saveKeywords();
        showToast("Ayarlar kaydedildi");
    }

    private void loadDefaultKeywords() {
        new AlertDialog.Builder(this)
                .setTitle("Varsayılan Kelimeler")
                .setMessage(
                        "Varsayılan spam kelimelerini yüklemek istediğinize emin misiniz? Bu işlem mevcut kelimelerinizin üzerine yazacaktır.")
                .setPositiveButton("Evet", (dialog, which) -> {
                    spamFilter.loadDefaultKeywords();
                    editTextKeywords.setText(spamFilter.getKeywordsAsString());
                    updateStatsDisplay();
                    showToast("Varsayılan kelimeler yüklendi");
                })
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void clearKeywords() {
        new AlertDialog.Builder(this)
                .setTitle("Kelimeleri Temizle")
                .setMessage("Tüm spam kelimelerini silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    spamFilter.clearKeywords();
                    editTextKeywords.setText("");
                    updateStatsDisplay();
                    showToast("Tüm kelimeler temizlendi");
                })
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void testSpamFilter() {
        // Test mesajı girme dialogu
        EditText editText = new EditText(this);
        editText.setHint("Test edilecek mesajı yazın...");
        editText.setMinLines(3);
        editText.setMaxLines(5);

        new AlertDialog.Builder(this)
                .setTitle("Spam Filtresi Test")
                .setView(editText)
                .setPositiveButton("Test Et", (dialog, which) -> {
                    String testMessage = editText.getText().toString();
                    if (!TextUtils.isEmpty(testMessage)) {
                        boolean isSpam = spamFilter.isSpam(testMessage);
                        String result = isSpam ? "SPAM" : "NORMAL";

                        new AlertDialog.Builder(this)
                                .setTitle("Test Sonucu")
                                .setMessage("Mesaj: \"" + testMessage + "\"\n\nSonuç: " + result)
                                .setPositiveButton("Tamam", null)
                                .show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showAddKeywordDialog() {
        EditText editText = new EditText(this);
        editText.setHint("Yeni kelime yazın...");

        new AlertDialog.Builder(this)
                .setTitle("Kelime Ekle")
                .setView(editText)
                .setPositiveButton("Ekle", (dialog, which) -> {
                    String newKeyword = editText.getText().toString().trim();
                    if (!TextUtils.isEmpty(newKeyword)) {
                        spamFilter.addKeyword(newKeyword);
                        editTextKeywords.setText(spamFilter.getKeywordsAsString());
                        updateStatsDisplay();
                        showToast("Kelime eklendi: " + newKeyword);
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveKeywords(); // Aktivite kapatılırken kelimeleri kaydet
    }
}
