package org.encryptor4j.android.encryptor;

import android.app.Application;

import java.security.Security;

/**
 * Created by Martin on 29-7-2017.
 */

public class EncryptorApplication extends Application {

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }
}
