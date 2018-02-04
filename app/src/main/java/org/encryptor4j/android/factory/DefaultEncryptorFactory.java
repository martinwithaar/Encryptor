package org.encryptor4j.android.factory;

import org.encryptor4j.Encryptor;
import org.encryptor4j.factory.EncryptorFactory;

import java.security.Key;

/**
 * Default <code>EncryptorFactory</code> implementation for factory creation of message and stream encryptors.
 *
 * Created by Martin on 16-4-2017.
 */

public class DefaultEncryptorFactory implements EncryptorFactory {

    private static final int AES_IV_LENGTH = 16;
    private static final int AES_GCM_IV_LENGTH = 12;
    private static final int AES_GCM_TAG_LENGTH = 128;

    @Override
    public Encryptor messageEncryptor(Key key) {
        Encryptor encryptor = new Encryptor(key, "AES/GCM/NoPadding", AES_GCM_IV_LENGTH, AES_GCM_TAG_LENGTH);
        encryptor.setGenerateIV(false);
        return encryptor;
    }

    @Override
    public Encryptor streamEncryptor(Key key) {
        return new Encryptor(key, "AES/CTR/NoPadding", AES_IV_LENGTH);
    }
}
