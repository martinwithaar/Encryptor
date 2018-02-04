package org.encryptor4j.android.encryptor;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Fragment implementation for adding keys to the keystore.
 *
 * There are three ways of adding a key:
 * <ol>
 * <li>Generate random key</li>
 * <li>Enter Base64 key</li>
 * <li>Password-based key</li>
 * </ol>
 * Created by Martin on 1-6-2016.
 */
public class AddKeyFragment extends DialogFragment implements View.OnClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = AddKeyFragment.class.getSimpleName();
    private static final String ALGORITHM = "AES";
    private static final int HASH_ITERATIONS = 16384;
    private static final byte[] SALT = new byte[] {-111, 103, -15, -17, 107, -117, -107, -96, -58, 33, 97, 41, 35, -106, -48, 11};

    private Spinner keyTypeSpinner;
    private TextView aliasView;
    private TextView keySourceView;
    private Spinner keySizeSpinner;
    private View addView;
    private CheckBox shareViaNFCCheckBox;
    private CheckBox userAuthenticationRequiredCheckBox;
    private TextView userAuthenticationValidityDurationSecondsView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_key, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Activity activity = getActivity();

        keyTypeSpinner = view.findViewById(R.id.key_type);
        ArrayAdapter<CharSequence> keyTypeAdapter = ArrayAdapter.createFromResource(activity, R.array.key_types, android.R.layout.simple_spinner_item);
        keyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyTypeSpinner.setAdapter(keyTypeAdapter);
        keyTypeSpinner.setOnItemSelectedListener(this);

        aliasView = view.findViewById(R.id.alias);
        keySourceView = view.findViewById(R.id.key_source);

        keySizeSpinner = view.findViewById(R.id.key_size);
        ArrayAdapter<CharSequence> keySizeAdapter = ArrayAdapter.createFromResource(activity, R.array.aes_key_sizes, android.R.layout.simple_spinner_item);
        keySizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keySizeSpinner.setAdapter(keySizeAdapter);
        keySizeSpinner.setOnItemSelectedListener(this);

        addView = view.findViewById(R.id.add);
        addView.setOnClickListener(this);

        shareViaNFCCheckBox = view.findViewById(R.id.share_via_nfc);
        NfcManager nfcManager = (NfcManager) activity.getSystemService(Context.NFC_SERVICE);
        NfcAdapter nfcAdapter = nfcManager.getDefaultAdapter();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            shareViaNFCCheckBox.setEnabled(true);
        }

        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        boolean deviceSecure = keyguardManager.isDeviceSecure();

        userAuthenticationRequiredCheckBox = view.findViewById(R.id.user_authentication_required);
        userAuthenticationRequiredCheckBox.setOnCheckedChangeListener(this);

        userAuthenticationValidityDurationSecondsView = view.findViewById(R.id.user_authentication_validity_duration_seconds);

        if (deviceSecure) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.USE_FINGERPRINT}, 0);
            } else {
                //FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
                //userAuthenticationValidityDurationSecondsNumberPicker.setEnabled(fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints());
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.add_key);
        return dialog;
    }

    @Override
    public void onClick(View v) {
        if (v == addView) {
            try {
                String alias = aliasView.getText().toString();
                if (!alias.isEmpty()) {
                    String keyType = (String) keyTypeSpinner.getSelectedItem();
                    String keySource = keySourceView.getText().toString();

                    Resources resources = getResources();
                    int keySize = Integer.parseInt((String) keySizeSpinner.getSelectedItem());
                    boolean exportable = shareViaNFCCheckBox.isChecked();
                    KeyProtection keyProtection = getKeyProtection();

                    SecretKey secretKey = null;
                    KeyStoreFragment parentFragment = (KeyStoreFragment) getParentFragment();
                    if (keyType.equals(resources.getString(R.string.random))) {
                        if (exportable) {
                            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                            keyGenerator.init(keySize);
                            secretKey = keyGenerator.generateKey();
                            parentFragment.importKey(alias, secretKey, keyProtection);
                        } else {
                            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore");
                            boolean userAuthenticationRequired = userAuthenticationRequiredCheckBox.isChecked();
                            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                                    alias,
                                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                    .setKeySize(keySize)
                                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC, KeyProperties.BLOCK_MODE_CTR, KeyProperties.BLOCK_MODE_GCM)
                                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                    .setUserAuthenticationRequired(userAuthenticationRequired);
                            if (userAuthenticationRequired) {
                                int userAuthenticationValidityDurationSeconds = Integer.valueOf(userAuthenticationValidityDurationSecondsView.getText().toString());
                                builder.setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds);
                            }
                            KeyGenParameterSpec keygenParameterSpec = builder.build();
                            keyGenerator.init(keygenParameterSpec);
                            secretKey = keyGenerator.generateKey();
                            parentFragment.updateKeyStoreList();
                        }
                    } else if (keyType.equals(resources.getString(R.string.password))) {
                        if (!keySource.isEmpty()) {
                            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                            KeySpec spec = new PBEKeySpec(keySource.toCharArray(), SALT, HASH_ITERATIONS, keySize);
                            secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITHM);
                            parentFragment.importKey(alias, secretKey, keyProtection);
                        } else {
                            keySourceView.setError(getResources().getString(R.string.key_source_required));
                        }
                    } else if (keyType.equals(resources.getString(R.string.base64))) {
                        if (!keySource.isEmpty()) {
                            byte[] keyBytes = Base64.decode(keySource, Base64.NO_WRAP);
                            secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
                            parentFragment.importKey(alias, secretKey, keyProtection);
                        } else {
                            keySourceView.setError(getResources().getString(R.string.key_source_required));
                        }
                    }

                    if (secretKey != null) {
                        if (shareViaNFCCheckBox.isChecked()) {
                            byte[] encoded = secretKey.getEncoded();
                            if (encoded != null) {
                                Uri.Builder builder = new Uri.Builder();
                                builder.appendQueryParameter(MainActivity.ALIAS, alias);
                                builder.appendQueryParameter(MainActivity.KEY, Base64.encodeToString(encoded, Base64.NO_WRAP));
                                builder.appendQueryParameter(MainActivity.ALGORITHM, secretKey.getAlgorithm());
                                builder.appendQueryParameter(MainActivity.USER_AUTHENTICATION_REQUIRED, String.valueOf(userAuthenticationRequiredCheckBox.isChecked()));
                                builder.appendQueryParameter(MainActivity.USER_AUTHENTICATION_VALIDITY_DURATION_SECONDS, userAuthenticationValidityDurationSecondsView.getText().toString());

                                //Intent intent = new Intent(getContext(), NFCKeyBeamActivity.class);
                                Intent intent = new Intent(getContext(), NFCReaderActivity.class);
                                intent.setData(builder.build());
                                startActivity(intent);
                            } else {
                                Toast.makeText(getActivity(), R.string.key_is_not_exportable, Toast.LENGTH_LONG).show();
                            }
                        }
                        dismiss();
                    }
                } else {
                    aliasView.setError(getResources().getString(R.string.alias_required));
                }
            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == keyTypeSpinner) {
            String keyType = (String) keyTypeSpinner.getSelectedItem();
            Resources resources = getResources();
            if (keyType.equals(resources.getString(R.string.password))) {
                keySourceView.setEnabled(true);
            } else if (keyType.equals(resources.getString(R.string.base64))) {
                keySourceView.setEnabled(true);
            } else {
                keySourceView.setEnabled(false);
                keySourceView.setText(null);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(compoundButton == userAuthenticationRequiredCheckBox) {
            userAuthenticationValidityDurationSecondsView.setEnabled(userAuthenticationRequiredCheckBox.isChecked());
        }
    }

    /**
     * Creates and returns a key protection object.
     * @return the key protection object
     */
    private KeyProtection getKeyProtection() {
        boolean userAuthenticationRequired = userAuthenticationRequiredCheckBox.isChecked();
        KeyProtection.Builder builder = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC, KeyProperties.BLOCK_MODE_CTR, KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(userAuthenticationRequired);
        if (userAuthenticationRequired) {
            int userAuthenticationValidityDurationSeconds = Integer.valueOf(userAuthenticationValidityDurationSecondsView.getText().toString());
            builder.setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds);
        }
        return builder.build();
    }
}
