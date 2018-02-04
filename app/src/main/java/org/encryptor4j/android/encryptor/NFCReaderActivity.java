package org.encryptor4j.android.encryptor;

import android.app.DialogFragment;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.encryptor4j.Encryptor;
import org.encryptor4j.ECDHPeer;
import org.encryptor4j.KeyAgreementPeer;
import org.encryptor4j.android.util.DebugUtils;
import org.encryptor4j.android.util.KeyAgreementUtils;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Activity for reading and writing to an NFC tag.
 * Created by Martin on 26-7-2017.
 */

public class NFCReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = NFCReaderActivity.class.getSimpleName();
    public static final byte[] CLA_INS_P1_P2 = Hex.decode("00A40400");
    private static final byte[] AID_ANDROID = Hex.decode("F0410DFCF2F0");
    private static final int ISO_DEP_TIMEOUT = 2500;

    private NfcAdapter nfcAdapter;
    private IsoDep isoDep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Setup layout
        setContentView(R.layout.activity_nfc_key_beam);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.share_via_nfc);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableReaderMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_encrypt, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_help) {
            DialogFragment helpDialogFragment = new HelpDialogFragment(R.string.help_nfc_key_export);
            helpDialogFragment.show(getFragmentManager(), "help");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        isoDep = IsoDep.get(tag);
        Thread thread = new Thread(new TranceiverRunnable());
        thread.start();
    }

    /**
     *
     */
    private class TranceiverRunnable implements Runnable {

        @Override
        public void run() {
            String secretKeyTransfer = getIntent().getData().getEncodedQuery();
            if(secretKeyTransfer == null || secretKeyTransfer.isEmpty()) {
                throw new IllegalArgumentException("There is no secret key transfer present");
            }

            try {
                // Connect to remote
                isoDep.connect();
                isoDep.setTimeout(ISO_DEP_TIMEOUT);
                Log.d(TAG, "Connected");

                byte[] response;

                // Receive supported curves list
                response = isoDep.transceive(createSelectAidApdu(AID_ANDROID));
                List<String> remoteSupportedCurvesList = Arrays.asList(new String(response).split(","));
                Log.d(TAG, "Remotely supported curves: " + remoteSupportedCurvesList);

                // Check if remotely supported curves are also locally supported
                String curve = null;
                for(String supportedCurve: KeyAgreementUtils.SUPPORTED_CURVES_LIST) {
                    if(remoteSupportedCurvesList.contains(supportedCurve)) {
                        curve = supportedCurve;
                        break;
                    }
                }
                if(curve == null) {
                    throw new IllegalStateException("No supported curve present");
                }

                Log.d(TAG, "Selected curve: " + curve);

                // Initiate key agreement
                KeyAgreementPeer keyAgreementPeer = new ECDHPeer(KeyAgreementUtils.resolveECParameterSpec(curve), "SC");
                ECPublicKey publicKey = (ECPublicKey) keyAgreementPeer.getPublicKey();
                Log.d(TAG, "Public key: " + publicKey);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(baos);

                // Send selected curve and public key
                dataOutputStream.writeUTF(curve);
                dataOutputStream.write(KeyAgreementUtils.exportKey(publicKey));
                dataOutputStream.flush();
                response = isoDep.transceive(baos.toByteArray());

                // Receive remote public key and compute shared secret
                ECPublicKey remotePublicKey = KeyAgreementUtils.importKey(curve, response);
                Log.d(TAG, "Received remote public key: " + remotePublicKey);
                byte[] sharedSecret = keyAgreementPeer.computeSharedSecret(remotePublicKey);

                // Derive key from shared secret
                SecretKey transferKey = KeyAgreementUtils.deriveSecretKey(sharedSecret);

                // Encrypt and send the secret key
                Encryptor encryptor = new Encryptor(transferKey);
                byte[] exportKeyBytes = encryptor.encrypt(secretKeyTransfer.getBytes());
                isoDep.transceive(exportKeyBytes);

            } catch (IOException | GeneralSecurityException | IllegalStateException e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(NFCReaderActivity.this, DebugUtils.getFirstExceptionMessage(e), Toast.LENGTH_LONG).show();
                    }
                });
            } finally {
                if(isoDep != null) {
                    try {
                        isoDep.close();
                        Log.d(TAG, "Connection closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Creates and returns a SelectAid APDU message.
     * @param aid the AID
     * @return the SelectAid APDU message
     */
    private static byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte) aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }
}
