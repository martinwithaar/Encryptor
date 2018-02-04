package org.encryptor4j.android.util;

import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.crypto.generators.HKDFBytesGenerator;
import org.spongycastle.crypto.params.HKDFParameters;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for performing tasks in support of key agreements.
 * Created by Martin on 5-8-2017.
 */

public class KeyAgreementUtils {

    private KeyAgreementUtils() {
    }

    /**
     * Priority ordered list of supported curves. These may change over time but can offer backward compatibility by moving down the list.
     */
    public static final List<String> SUPPORTED_CURVES_LIST = Arrays.asList(
            "curve25519"
    );

    /**
     * Resolves the <code>ECParameterSpec</code> for string <code>curve</code> to also include curves without OID.
     * @param curve the curve
     * @return a <code>ECParameterSpec</code> for the given curve name, <code>null</code> otherwise
     */
    public static ECParameterSpec resolveECParameterSpec(String curve) {
        if(curve.equals("curve25519")) {
            X9ECParameters params = CustomNamedCurves.getByName("curve25519");
            return new ECParameterSpec(params.getCurve(), params.getG(), params.getN(), params.getH(), params.getSeed());
        }
        return ECNamedCurveTable.getParameterSpec(curve);
    }

    /**
     * Exports the <code>ECPublicKey</code> to a byte array.
     * @param ecPublicKey the elliptic curve public key
     * @return a byte array with the encoded and compressed key
     */
    public static byte[] exportKey(ECPublicKey ecPublicKey) {
        return ecPublicKey.getQ().getEncoded(true);
    }

    /**
     * Imports the <code>ECPublicKey</code> from a byte array.
     * @param curveName the curve name
     * @param data      a byte array with the encoded key
     * @return the elliptic curve public key
     * @throws InvalidKeySpecException
     */
    public static ECPublicKey importKey(String curveName, byte[] data) throws InvalidKeySpecException {
        ECParameterSpec params = ECNamedCurveTable.getParameterSpec(curveName);
        ECPublicKeySpec publicKey = new ECPublicKeySpec(params.getCurve().decodePoint(data), params);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("ECDH", "SC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
        return (ECPublicKey) keyFactory.generatePublic(publicKey);
    }

    /**
     * Derives a 256-bit AES secret key from a shared secret using a HKDF.
     * @param sharedSecret the shared secret
     * @return a secret key derived from the shared secret
     */
    public static SecretKey deriveSecretKey(byte[] sharedSecret) {
        HKDFBytesGenerator bytesGenerator = new HKDFBytesGenerator(new SHA256Digest());
        bytesGenerator.init(HKDFParameters.defaultParameters(sharedSecret));
        byte[] bytes = new byte[32];
        bytesGenerator.generateBytes(bytes, 0, bytes.length);
        return new SecretKeySpec(bytes, "AES");
    }
}
