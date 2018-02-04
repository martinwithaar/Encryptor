package org.encryptor4j.android.encryptor;

import android.content.Intent;
import android.net.Uri;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.encryptor4j.ECDHPeer;
import org.encryptor4j.Encryptor;
import org.encryptor4j.KeyAgreementPeer;
import org.encryptor4j.android.util.DebugUtils;
import org.encryptor4j.android.util.KeyAgreementUtils;
import org.spongycastle.jce.interfaces.ECPublicKey;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.SecretKey;

/**
 * Service for hosting an APDU host.
 */
public class EncryptorHostApduService extends HostApduService {

    private static final String TAG = EncryptorHostApduService.class.getSimpleName();

    private boolean aidSelected;
    private SecretKey transferKey;

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        if (!aidSelected) {
            if(selectAidApdu(apdu)) {
                Log.d(TAG, "SelectAidApdu apdu: " + Arrays.toString(apdu));
                aidSelected = true;

                // Return the supported curves
                return TextUtils.join(",", KeyAgreementUtils.SUPPORTED_CURVES_LIST).getBytes();
            } else {
                Log.e(TAG, "Could not verify selectAid");
            }
        } else if(transferKey == null) {
            // Receive the selected curve and the remote public key
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new ByteArrayInputStream(apdu));

                String curve = dis.readUTF();
                Log.d(TAG, "Selected curve: " + curve);

                byte[] encodedRemotePublicKey = IOUtils.toByteArray(dis);

                KeyAgreementPeer keyAgreementPeer = new ECDHPeer(KeyAgreementUtils.resolveECParameterSpec(curve), null, "SC");
                ECPublicKey publicKey = (ECPublicKey) keyAgreementPeer.getPublicKey();
                Log.d(TAG, "Public key: " + publicKey);

                ECPublicKey remotePublicKey = KeyAgreementUtils.importKey(curve, encodedRemotePublicKey);
                Log.d(TAG, "Received remote public key: " + remotePublicKey);
                byte[] sharedSecret = keyAgreementPeer.computeSharedSecret(remotePublicKey);

                // Derive key from shared secret
                transferKey = KeyAgreementUtils.deriveSecretKey(sharedSecret);

                // Respond with public key
                return KeyAgreementUtils.exportKey(publicKey);
            } catch (IOException | GeneralSecurityException e) {
                clearState();
                Toast.makeText(getBaseContext(), DebugUtils.getFirstExceptionMessage(e), Toast.LENGTH_LONG).show();
            } finally {
                if(dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // Receive the transferred secret key
            Encryptor encryptor = new Encryptor(transferKey);
            try {
                byte[] importKeyBytes = encryptor.decrypt(apdu);

                // Create Uri from query string
                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.encodedQuery(new String(importKeyBytes));
                Uri uri = uriBuilder.build();

                // Import the key into the keystore
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction(MainActivity.ACTION_IMPORT_KEY);
                intent.setData(uri);
                startActivity(intent);
                return new byte[] { 0x00 };
            } catch (GeneralSecurityException e) {
                Toast.makeText(getBaseContext(), DebugUtils.getFirstExceptionMessage(e), Toast.LENGTH_LONG).show();
            } finally {
                clearState();
            }
        }
        return null;
    }

    @Override
    public void onDeactivated(int reason) {
        clearState();
        Log.i(TAG, "Deactivated: " + reason);
    }

    /**
     * Clears the state of the tag.
     */
    private void clearState() {
        aidSelected = false;
        transferKey = null;
    }

    /**
     * Checks whether the APDU constitutes a valid SelectAID message.
     * @param apdu    the APDU
     * @return <code>true</code> when a valid SelectAID message, <code>false</code> otherwise
     */
    private static boolean selectAidApdu(byte[] apdu) {
        byte[] header = new byte[NFCReaderActivity.CLA_INS_P1_P2.length];
        System.arraycopy(apdu, 0, header, 0, header.length);
        return Arrays.equals(header, NFCReaderActivity.CLA_INS_P1_P2);
    }
}