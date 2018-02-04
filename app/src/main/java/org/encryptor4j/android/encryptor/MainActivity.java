package org.encryptor4j.android.encryptor;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Main activity implementation.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener {

    public static final String TAG = MainActivity.class.getSimpleName();

//    public static final String ALGORITHM = "algorithm";
//    public static final String ALIAS = "alias";
//    public static final String KEY = "key";
//    public static final String USER_AUTHENTICATION_REQUIRED = "userAuthenticationRequired";
//    public static final String USER_AUTHENTICATION_VALIDITY_DURATION_SECONDS = "userAuthenticationValidityDurationSeconds";

    public static final String ALGORITHM = "r";
    public static final String ALIAS = "a";
    public static final String KEY = "k";
    public static final String USER_AUTHENTICATION_REQUIRED = "u";
    public static final String USER_AUTHENTICATION_VALIDITY_DURATION_SECONDS = "d";

    public static final String ACTION_IMPORT_KEY = "org.encryptor4j.android.encryptor.IMPORT_KEY";

    private static final String PREF_ALIAS = "alias";

    private KeyStore keyStore;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner keySpinner;
    private Fragment contentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content_frame);
        if(fragment == null) {
            setContentFragment(new KeyStoreFragment());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerToggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(this);

        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if(Intent.ACTION_MAIN.equals(getIntent().getAction())) {
                if (keyStore.size() == 0) {
                    KeyStoreFragment keyStoreFragment = new KeyStoreFragment();
                    Bundle args = new Bundle();
                    args.putBoolean(KeyStoreFragment.ARG_SHOW_ADD_KEY_DIALOG, true);
                    keyStoreFragment.setArguments(args);
                    setContentFragment(keyStoreFragment);
                    Toast.makeText(this, R.string.add_key_first, Toast.LENGTH_LONG).show();
                }
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }

        keySpinner = navigationView.getHeaderView(0).findViewById(R.id.key_spinner);
        keySpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keySpinner.setAdapter(adapter);
        updateKeyStoreList();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check for NFC NDEF message
        Intent intent = getIntent();
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            // Get the payload query
            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage message = (NdefMessage) messages[0];

            String encodedQuery = new String(message.getRecords()[0].getPayload());
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.encodedQuery(encodedQuery);
            Uri uri = uriBuilder.build();

            importKeyFromUri(uri);

            // Clear action
            intent.setAction(null);
            Toast.makeText(this, R.string.key_received_successfully, Toast.LENGTH_LONG).show();
        } else if(ACTION_IMPORT_KEY.equals(action)) {

            // Import key from data
            importKeyFromUri(intent.getData());

            // Clear action
            intent.setAction(null);
            Toast.makeText(this, R.string.key_received_successfully, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            if(drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        } else if(id == R.id.action_info) {
            DialogFragment infoDialogFragment = new InfoDialogFragment();
            infoDialogFragment.show(getFragmentManager(), "infoDialog");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(R.id.key_store == id) {
            setContentFragment(new KeyStoreFragment());
        } else if(R.id.file_encryption == id) {
            setContentFragment(new FileEncryptFragment());
        } else if(R.id.message_encryption == id) {
            setContentFragment(new MessageEncryptFragment());
        }
        drawerLayout.closeDrawer(navigationView);
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent == keySpinner) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = preferences.edit();
            edit.putString(PREF_ALIAS, (String) keySpinner.getSelectedItem());
            edit.apply();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Sets the content fragment.
     * @param fragment    the content fragment
     */
    private void setContentFragment(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        fragment.setRetainInstance(true);
        this.contentFragment = fragment;
    }

    /**
     * Updates the keystore list.
     */
    public void updateKeyStoreList() {
        List<String> aliases;
        try {
            aliases = Collections.list(keyStore.aliases());
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(aliases);

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) keySpinner.getAdapter();
        adapter.clear();
        adapter.addAll(aliases);
        adapter.notifyDataSetChanged();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String alias = preferences.getString(PREF_ALIAS, null);
        if(alias != null) {
            int selection = aliases.indexOf(alias);
            if(selection >= 0) {
                keySpinner.setSelection(Math.max(0, selection));
            } else {
                keySpinner.setSelection(0);
            }
        }
        keySpinner.setEnabled(!aliases.isEmpty());

        MenuItem menuItem;
        Menu menu = navigationView.getMenu();

        menuItem = menu.findItem(R.id.file_encryption);
        menuItem.setEnabled(!aliases.isEmpty());

        menuItem = menu.findItem(R.id.message_encryption);
        menuItem.setEnabled(!aliases.isEmpty());
    }

    /**
     * Returns the currently selected key.
     * @return the selected key
     */
    public Key getKey() {
        try {
            return keyStore.getKey((String) keySpinner.getSelectedItem(), null);
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException e) {
            throw new RuntimeException(e);
        }
    }

    private void importKeyFromUri(Uri uri) {
        // Prepare key parameters
        String alias = uri.getQueryParameter(ALIAS);
        String key = uri.getQueryParameter(KEY);
        String algorithm = uri.getQueryParameter(ALGORITHM);
        boolean userAuthenticationRequired = Boolean.valueOf(uri.getQueryParameter(USER_AUTHENTICATION_REQUIRED));
        int userAuthenticationValidityDurationSeconds;
        try {
            userAuthenticationValidityDurationSeconds = Integer.valueOf(uri.getQueryParameter(USER_AUTHENTICATION_VALIDITY_DURATION_SECONDS));
        } catch(NumberFormatException e) {
            userAuthenticationValidityDurationSeconds = -1;
        }

        // Add new secret key
        byte[] keyBytes = Base64.decode(key, Base64.NO_WRAP);
        SecretKey secretKey = new SecretKeySpec(keyBytes, algorithm);

        // Make sure current fragment is keystore fragment
        KeyStoreFragment keyStoreFragment;
        try {
            keyStoreFragment = (KeyStoreFragment) contentFragment;
        } catch(ClassCastException e) {
            keyStoreFragment = null;
        }
        if(keyStoreFragment == null) {
            keyStoreFragment = new KeyStoreFragment();
            setContentFragment(keyStoreFragment);
        }

        // Add the new key to the keystore
        KeyProtection.Builder builder = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC, KeyProperties.BLOCK_MODE_CTR, KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(userAuthenticationRequired);
        if(userAuthenticationRequired) {
            builder.setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds);
        }
        KeyProtection keyProtection = builder.build();
        keyStoreFragment.importKey(alias, secretKey, keyProtection);
    }
}
