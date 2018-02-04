package org.encryptor4j.android.encryptor;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

/**
 * Fragment implementation for showing key information.
 * <p>Star classification:</p>
 * <ol>
 * <li>Is 256 bit</li>
 * <li>Is generated instead of imported/unknown</li>
 * <li>Is inside secure hardware</li>
 * <li>Is user authentication required</li>
 * <li>Is user authentication validity temporary</li>
 * </ol>
 * Created by Martin on 16-4-2017.
 */

public class KeyFragment extends Fragment {

    public static final String ARG_ALIAS = "alias";

    private static final SparseIntArray ORIGIN_RESOURCE_IDS = new SparseIntArray();
    static {
        ORIGIN_RESOURCE_IDS.put(KeyProperties.ORIGIN_GENERATED, R.string.generated);
        ORIGIN_RESOURCE_IDS.put(KeyProperties.ORIGIN_IMPORTED, R.string.imported);
        ORIGIN_RESOURCE_IDS.put(KeyProperties.ORIGIN_UNKNOWN, R.string.unknown);
    }

    private KeyStore keyStore;
    private String alias;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Bundle args = getArguments();
        alias = args.getString(ARG_ALIAS);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_key, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Key key;
        try {
            key = keyStore.getKey(alias, null);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }

        KeyInfo keyInfo = getKeyInfo(key);

        TextView aliasView = view.findViewById(R.id.alias);
        aliasView.setText(keyInfo.getKeystoreAlias());

        TextView algorithmView = view.findViewById(R.id.algorithm);
        algorithmView.setText(key.getAlgorithm());

        TextView keySizeView = view.findViewById(R.id.key_size);
        keySizeView.setText(String.valueOf(keyInfo.getKeySize()));

        TextView originView = view.findViewById(R.id.origin);
        originView.setText(ORIGIN_RESOURCE_IDS.get(keyInfo.getOrigin()));

        TextView userAuthenticationValidityDurationSecondsTextView = view.findViewById(R.id.user_authentication_validity_duration_seconds);
        userAuthenticationValidityDurationSecondsTextView.setText(String.valueOf(keyInfo.getUserAuthenticationValidityDurationSeconds()));

        CheckBox insideSecureHardwareCheckBox = view.findViewById(R.id.inside_secure_hardware);
        insideSecureHardwareCheckBox.setChecked(keyInfo.isInsideSecureHardware());

        CheckBox invalidatedByBiometricEnrollmentCheckBox = view.findViewById(R.id.invalidated_by_biometric_enrollment);
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            invalidatedByBiometricEnrollmentCheckBox.setChecked(keyInfo.isInvalidatedByBiometricEnrollment());
        } else {
            invalidatedByBiometricEnrollmentCheckBox.setError(getResources().getString(R.string.android_api_24_required));
        }

        CheckBox userAuthenticationRequiredCheckBox = view.findViewById(R.id.user_authentication_required);
        userAuthenticationRequiredCheckBox.setChecked(keyInfo.isUserAuthenticationRequired());

        CheckBox userAuthenticationRequirementEnforcedBySecureHardwareCheckBox = view.findViewById(R.id.user_authentication_requirement_enforced_by_secure_hardware);
        userAuthenticationRequirementEnforcedBySecureHardwareCheckBox.setChecked(keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware());
    }

    /**
     * Deletes this key from the keystore.
     */
    public void deleteKey() {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a <code>KeyInfo</code> object for the given key.
     * @param key the key
     * @return the keyinfo object
     */
    private static KeyInfo getKeyInfo(Key key) {
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance(key.getAlgorithm(), "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
        try {
            return (KeyInfo) factory.getKeySpec((SecretKey) key, KeyInfo.class);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
