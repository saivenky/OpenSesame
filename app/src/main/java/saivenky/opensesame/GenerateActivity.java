package saivenky.opensesame;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class GenerateActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "OpenSesamePrefs";
    private static final String PREF_TAG_HISTORY = "TagHistory";
    private static final long CLEAR_PASSWORD_TIME_IN_MS = 120000;
    private static final long CLEAR_PASSWORD_WARNING_TIME_IN_MS = 30000;
    private static final long CLEAR_PASSWORD_TIMER_TICK_IN_MS = 15000;
    private static final String CLIPBOARD_PASSWORD_KEY = "OpenSesame.generatedPassword";

    // UI references.
    private AutoCompleteTextView mTagView;
    private EditText mPassphraseView;
    private Switch mHasLowercase;
    private Switch mHasUppercase;
    private Switch mHasNumbers;
    private Switch mHasSymbols;

    private ClipboardManager clipboardManager;
    private SharedPreferences settings;
    private Set<String> tagHistory;
    private CountDownTimer clearPasswordTimer;
    private PasswordGenerator passwordGenerator;

    private void setTagHistoryAdapter() {
        String[] tags = tagHistory.toArray(new String[tagHistory.size()]);
        for (String tag : tags) {
            System.out.println(tag);
        }
        ArrayAdapter<String> tagHistoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tags);
        mTagView.setAdapter(tagHistoryAdapter);
    }

    private void savePrefs() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean succeeded = settings.edit()
            .clear()
            .putStringSet(PREF_TAG_HISTORY, tagHistory)
            .commit();
        if (!succeeded) {
            System.out.println("Failed to write preferences");
        }
    }

    private void addToTagHistory(String tag) {
        if (!tagHistory.contains(tag)) {
            tagHistory.add(tag);
            setTagHistoryAdapter();
            savePrefs();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);

        mTagView = (AutoCompleteTextView) findViewById(R.id.tag);
        settings = getSharedPreferences(PREFS_NAME, 0);
        tagHistory = settings.getStringSet(PREF_TAG_HISTORY, new HashSet<String>());
        setTagHistoryAdapter();

        mPassphraseView = (EditText) findViewById(R.id.passphrase);
        mPassphraseView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptGeneratePassword();
                    return true;
                }
                return false;
            }
        });

        mHasLowercase = (Switch) findViewById(R.id.hasLowercase);
        mHasUppercase = (Switch) findViewById(R.id.hasUppercase);
        mHasNumbers = (Switch) findViewById(R.id.hasNumbers);
        mHasSymbols = (Switch) findViewById(R.id.hasSymbols);
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clearPasswordTimer = new CountDownTimer(CLEAR_PASSWORD_TIME_IN_MS, CLEAR_PASSWORD_TIMER_TICK_IN_MS) {
            @Override
            public void onTick(long l) {
                if (l <= CLEAR_PASSWORD_WARNING_TIME_IN_MS && l > (CLEAR_PASSWORD_WARNING_TIME_IN_MS - CLEAR_PASSWORD_TIMER_TICK_IN_MS)) {
                    String warningText = String.format("Clearing clipboard in %d seconds", CLEAR_PASSWORD_WARNING_TIME_IN_MS/1000);
                    Toast warningOfClear = Toast.makeText(GenerateActivity.this, warningText, Toast.LENGTH_SHORT);
                    warningOfClear.show();
                }
            }

            @Override
            public void onFinish() {
                clearPasswordFromClipboard();
            }
        };

        passwordGenerator = new PasswordGenerator();

        Button mGenerateButton = (Button) findViewById(R.id.generate_button);
        mGenerateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptGeneratePassword();
            }
        });
    }

    private void resetErrors() {
        mTagView.setError(null);
        mPassphraseView.setError(null);
    }

    private void attemptGeneratePassword() {
        resetErrors();

        // Store values at the time of the login attempt.
        String tag = mTagView.getText().toString();
        String passphrase = mPassphraseView.getText().toString();
        boolean hasLowercase = mHasLowercase.isChecked();
        boolean hasUppercase = mHasUppercase.isChecked();
        boolean hasNumbers = mHasNumbers.isChecked();
        boolean hasSymbols = mHasSymbols.isChecked();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(passphrase)) {
            mPassphraseView.setError(getString(R.string.error_field_required));
            focusView = mPassphraseView;
            cancel = true;
        }

        if (TextUtils.isEmpty(tag)) {
            mTagView.setError(getString(R.string.error_field_required));
            focusView = mTagView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return;
        }

        addToTagHistory(tag);

        String generatedPassword = passwordGenerator.generate(
            hasLowercase, hasUppercase, hasNumbers, hasSymbols, tag, passphrase);
        copyPasswordToClipboard(generatedPassword);
    }

    private void copyPasswordToClipboard(String password) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_PASSWORD_KEY, password));
        Context context = getApplicationContext();
        CharSequence text = "Copied '" + password + "' to clipboard";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
        clearPasswordTimer.start();
    }

    private void clearPasswordFromClipboard() {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_PASSWORD_KEY, ""));
        Context context = getApplicationContext();
        CharSequence text = "Cleared password";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}

