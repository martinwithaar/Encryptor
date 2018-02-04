package org.encryptor4j.android.util;

import android.util.Base64;

import org.encryptor4j.Encryptor;
import org.encryptor4j.util.TextEncryptor;

import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * TextEncryptor implementation that makes use of Android's own Base64 encoder.
 *
 * Created by Martin on 16-4-2017.
 */

public class AndroidTextEncryptor extends TextEncryptor {

    public AndroidTextEncryptor() {
        super();
    }

    public AndroidTextEncryptor(String password) {
        super(password);
    }

    public AndroidTextEncryptor(Key key) {
        super(key);
    }

    public AndroidTextEncryptor(Encryptor encryptor) {
        super(encryptor);
    }

    @Override
    public String encrypt(String message) throws GeneralSecurityException {
        byte[] bytes = getEncryptor().encrypt(message.getBytes());
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    @Override
    public String decrypt(String message) throws GeneralSecurityException {
        byte[] bytes = Base64.decode(message, Base64.NO_WRAP);
        return new String(getEncryptor().decrypt(bytes));
    }
}
