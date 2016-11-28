package saivenky.opensesame;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;


public class GenerateActivity extends AppCompatActivity {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%&*()?=";
    private static final int PASS_LENGTH = 10;

    // UI references.
    private EditText mTagView;
    private EditText mPassphraseView;
    private Switch mHasLowercase;
    private Switch mHasUppercase;
    private Switch mHasNumbers;
    private Switch mHasSymbols;
    private TextView mGenerated;

    private ClipboardManager clipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);
        // Set up the login form.
        mTagView = (EditText) findViewById(R.id.tag);

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
        mGenerated = (TextView) findViewById(R.id.generated);
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

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
        mHasLowercase.setError(null);
        mGenerated.setText("GENERATED");
    }

    private int[] generateSeed(
            boolean hasLowercase, boolean hasUppercase, boolean hasNumbers, boolean hasSymbols,
            String tag, String passphrase) {
        String usage = "";
        usage += hasLowercase ? "1" : "0";
        usage += hasUppercase ? "1" : "0";
        usage += hasNumbers ? "1" : "0";
        usage += hasSymbols ? "1" : "0";

        String output = encode(usage) + "+" + encode(tag) + "+" + encode(passphrase);
        return unsigned(toSHA1(output));
    }

    private StringBuilder buildPassword(
            int[] seed, boolean hasLowercase, boolean hasUppercase, boolean hasNumbers, boolean hasSymbols) {
        String fullSet = "";
        fullSet += hasLowercase ? LOWERCASE : "";
        fullSet += hasUppercase ? UPPERCASE : "";
        fullSet += hasNumbers ? NUMBERS : "";
        fullSet += hasSymbols ? SYMBOLS : "";

        int passLength = 10;
        StringBuilder passwordBuilder = new StringBuilder();
        for (int i = 0; i < PASS_LENGTH; i++) {
            passwordBuilder.append(fullSet.charAt(seed[i] % fullSet.length()));
        }
        passwordBuilder.append(" ");
        return passwordBuilder;
    }

    private String correctGeneratedPassword(
            StringBuilder passwordBuilder, int[] seed,
            boolean hasLowercase, boolean hasUppercase, boolean hasNumbers, boolean hasSymbols) {
        int setNumber = 0;
        if (hasLowercase) {
            if (!Pattern.matches(".*?[" + LOWERCASE + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, LOWERCASE.charAt(seed[PASS_LENGTH + setNumber] % LOWERCASE.length()));
            }
            setNumber += 1;
        }
        if (hasUppercase) {
            if (!Pattern.matches(".*?[" + UPPERCASE + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, UPPERCASE.charAt(seed[PASS_LENGTH + setNumber] % UPPERCASE.length()));
            }
            setNumber += 1;
        }
        if (hasNumbers) {
            if (!Pattern.matches(".*?[" + NUMBERS + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, NUMBERS.charAt(seed[PASS_LENGTH + setNumber] % NUMBERS.length()));
            }
            setNumber += 1;
        }
        if (hasSymbols) {
            if (!Pattern.matches(".*?[" + SYMBOLS + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, SYMBOLS.charAt(seed[PASS_LENGTH + setNumber] % SYMBOLS.length()));
            }
        }

        return passwordBuilder.substring(0, PASS_LENGTH);
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


        if (!TextUtils.isEmpty(passphrase)) {
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

        int[] seed = generateSeed(hasLowercase, hasUppercase, hasNumbers, hasSymbols, tag, passphrase);
        StringBuilder passwordBuilder = buildPassword(seed, hasLowercase, hasUppercase, hasNumbers, hasSymbols);
        String generatedPassword = correctGeneratedPassword(passwordBuilder, seed, hasLowercase, hasUppercase, hasNumbers, hasSymbols);

        copyPasswordToClipboard(generatedPassword);
        mGenerated.setText(generatedPassword);
    }

    private void copyPasswordToClipboard(String password) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("OpenSesame.generatedPassword", password));
    }

    public static byte[] toSHA1(String toHash) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return md.digest(toHash.getBytes());
    }

    private String encode(String toEncode) {
        byte[] digest = toSHA1(toEncode);
        String digestAsString = Charset.forName("ISO-8859-1").decode(ByteBuffer.wrap(digest)).toString();
        return digestAsString;
    }

    public static int[] unsigned(byte[] byteArray) {
        int[] result = new int[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            result[i] = byteArray[i] & 0xff;
        }
        return result;
    }
}
