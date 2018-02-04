package org.encryptor4j.android.encryptor;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Activity implementation for beaming NFC Ndef messages.
 *
 * Created by Martin on 13-4-2017.
 */

public class NFCKeyBeamActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {

    public static final String TAG = NFCKeyBeamActivity.class.getSimpleName();

    private int shareCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_LONG).show();
            return;
        }
        nfcAdapter.setNdefPushMessageCallback(this, this);
        nfcAdapter.setOnNdefPushCompleteCallback(this, this);

        // Setup layout
        setContentView(R.layout.activity_nfc_key_beam);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if(uri != null) {
            String query = uri.getEncodedQuery();
            return new NdefMessage(NdefRecord.createMime("application/vnd.org.encryptor4j.android.encryptor", query.getBytes()));
        }
        return null;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shareCount++;
                Toast.makeText(NFCKeyBeamActivity.this, R.string.key_beamed_successfully, Toast.LENGTH_LONG).show();
            }
        });
    }
}
