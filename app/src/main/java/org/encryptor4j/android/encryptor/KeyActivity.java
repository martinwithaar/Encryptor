package org.encryptor4j.android.encryptor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Activity implementation for showing key information.
 *
 * Created by Martin on 16-4-2017.
 */

public class KeyActivity extends AppCompatActivity {

    public static final String EXTRA_ALIAS = "alias";
    private static final String CONFIRM_DELETE = "confirmDelete";

    private KeyFragment keyFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.key);
        }

        // Copy alias
        keyFragment = new KeyFragment();
        Bundle args = new Bundle();
        args.putString(KeyFragment.ARG_ALIAS, getIntent().getStringExtra(EXTRA_ALIAS));
        keyFragment.setArguments(args);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.content_frame, keyFragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_key, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if(id == R.id.action_delete) {
            DialogFragment confirmFragment = new ConfirmFragment();
            confirmFragment.show(getFragmentManager(), CONFIRM_DELETE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *
     */
    private class ConfirmFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.warning)
                .setMessage(R.string.confirm_delete_key)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        keyFragment.deleteKey();
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
            return builder.create();
        }
    }
}
