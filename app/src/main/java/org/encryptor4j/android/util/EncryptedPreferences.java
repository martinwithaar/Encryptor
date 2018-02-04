package org.encryptor4j.android.util;

import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;

import org.encryptor4j.Encryptor;
import org.encryptor4j.util.TextEncryptor;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.KeyGenerator;

/**
 * <code>SharedPreferences</code> implementation that stores its values using encryption.
 * Created by Martin on 13-4-2017.
 */

public class EncryptedPreferences implements SharedPreferences {

    private static final String DEFAULT_ALIAS = "_EncryptedPreferences";

    private SharedPreferences sharedPreferences;
    private TextEncryptor textEncryptor;

    /**
     *
     * @param sharedPreferences
     * @param textEncryptor
     */
    public EncryptedPreferences(SharedPreferences sharedPreferences, TextEncryptor textEncryptor) {
        this.sharedPreferences = sharedPreferences;
        this.textEncryptor = textEncryptor;
    }

    @Override
    public Map<String, ?> getAll() {
        Map<String, Object> allMap = (Map<String, Object>) sharedPreferences.getAll();
        for(Map.Entry<String, Object> entry: allMap.entrySet()) {
            try {
                entry.setValue(textEncryptor.decrypt((String) entry.getValue()));
            } catch(ClassCastException ignored) {
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        return allMap;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        String value = sharedPreferences.getString(key, null);
        if(value != null) {
            try {
                return textEncryptor.decrypt(value);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        return defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        Set<String> set = sharedPreferences.getStringSet(key, null);
        if(set != null) {
            Set<String> decryptedSet = new LinkedHashSet<>();
            for (String value : set) {
                try {
                    decryptedSet.add(value != null ? textEncryptor.decrypt(value) : null);
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException(e);
                }
            }
            return decryptedSet;
        }
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        String value = getString(key, null);
        return value != null ? Integer.valueOf(value) : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        String value = getString(key, null);
        return value != null ? Long.valueOf(value) : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        String value = getString(key, null);
        return value != null ? Float.valueOf(value) : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        String value = getString(key, null);
        return value != null ? Boolean.valueOf(value) : defValue;
    }

    @Override
    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    @Override
    public Editor edit() {
        return new EncryptedEditor(sharedPreferences.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     *
     */
    private class EncryptedEditor implements Editor {

        private Editor edit;

        private EncryptedEditor(Editor edit) {
            this.edit = edit;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            try {
                return edit.putString(key, value != null ? textEncryptor.encrypt(value) : null);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> set) {
            Set<String> encryptedSet = new LinkedHashSet<>();
            if(set != null) {
                for(String value: set) {
                    try {
                        encryptedSet.add(value != null ? textEncryptor.encrypt(value) : null);
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException(e);
                    }
                }
                edit.putStringSet(key, encryptedSet);
            }
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            putString(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            putString(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            putString(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            putString(key, String.valueOf(value));
            return this;
        }

        @Override
        public Editor remove(String key) {
            return edit.remove(key);
        }

        @Override
        public Editor clear() {
            edit.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return edit.commit();
        }

        @Override
        public void apply() {
            edit.apply();
        }
    }

    /**
     *
     * @param sharedPreferences
     * @return
     * @throws UnrecoverableKeyException
     */
    public static SharedPreferences wrapDefault(SharedPreferences sharedPreferences) throws UnrecoverableKeyException {
        return new EncryptedPreferences(sharedPreferences, new AndroidTextEncryptor(new Encryptor(getDefaultKey(), "AES/CBC/PKCS7Padding", 16)));
    }

    /**
     *
     * @return
     * @throws UnrecoverableKeyException
     */
    private static Key getDefaultKey() throws UnrecoverableKeyException {
        Key key;
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            key = keyStore.getKey(DEFAULT_ALIAS, null);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
        if(key == null) {
            KeyGenerator keyGenerator;
            try {
                keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                throw new RuntimeException(e);
            }
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    DEFAULT_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            KeyGenParameterSpec keygenParameterSpec = builder.build();
            try {
                keyGenerator.init(keygenParameterSpec);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
            return keyGenerator.generateKey();
        }
        return key;
    }
}
