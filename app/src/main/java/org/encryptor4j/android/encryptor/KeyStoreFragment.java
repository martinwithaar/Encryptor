package org.encryptor4j.android.encryptor;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.KeyProtection;
import android.support.annotation.Nullable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Fragment implementation for managing the keystore.
 *
 * Created by Martin on 1-6-2016.
 */
public class KeyStoreFragment extends Fragment implements AdapterView.OnItemClickListener {

    public static final String ARG_SHOW_ADD_KEY_DIALOG = "showAddKeyDialog";
    private static final String TAG_ADD_KEY = "addKey";
    private KeyStore keyStore;
    private ListView listView;
    private ContextMenu.ContextMenuInfo menuInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        try {
            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateKeyStoreList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_keystore, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_add_key) {
            showAddKeyDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        this.menuInfo = menuInfo;
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_keystore, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_delete) {
            AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            String alias = (String) listView.getAdapter().getItem(adapterMenuInfo.position);
            try {
                keyStore.deleteEntry(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
            menuInfo = null;
            updateKeyStoreList();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_keystore, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(android.R.id.list);
        try {
            List<String> aliases = Collections.list(keyStore.aliases());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, aliases);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(this);
            registerForContextMenu(listView);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        Bundle args = getArguments();
        if(args != null && args.getBoolean(ARG_SHOW_ADD_KEY_DIALOG)) {
            showAddKeyDialog();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(parent == listView) {
            String alias = (String) listView.getAdapter().getItem(position);
            Intent intent = new Intent(getActivity(), KeyActivity.class);
            intent.putExtra(KeyActivity.EXTRA_ALIAS, alias);
            startActivity(intent);
        }
    }

    /**
     * Shows the add key dialog.
     */
    private void showAddKeyDialog() {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag(TAG_ADD_KEY);
        if (prev != null) {
            transaction.remove(prev);
        }
        transaction.addToBackStack(null);
        DialogFragment fragment = new AddKeyFragment();
        fragment.setShowsDialog(true);
        fragment.show(transaction, TAG_ADD_KEY);
    }

    /**
     * Imports a key.
     * @param alias the alias
     * @param key the key
     * @param keyProtection the key's protection parameters
     */
    public void importKey(String alias, SecretKey key, KeyProtection keyProtection) {
        try {
            keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(key), keyProtection);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        updateKeyStoreList();
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

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
        adapter.clear();
        adapter.addAll(aliases);
        adapter.notifyDataSetChanged();

        // Refresh the main activity list
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.updateKeyStoreList();
    }
}