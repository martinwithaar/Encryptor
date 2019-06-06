package org.encryptor4j.android.encryptor;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.encryptor4j.Encryptor;
import org.encryptor4j.android.factory.DefaultEncryptorFactory;
import org.encryptor4j.android.util.AndroidTextEncryptor;
import org.encryptor4j.android.util.DebugUtils;
import org.encryptor4j.factory.EncryptorFactory;
import org.encryptor4j.util.TextEncryptor;

import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * Fragment for encrypting text messages.
 *
 * Created by Martin on 2-6-2016.
 */
public class MessageEncryptFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "MessageEncryptFragment";
    private static final int CONFIRM_DEVICE_CREDENTIALS = 0x01;
    private TextView plainTextView;
    private TextView cipherTextView;
    private View plainTextToClipboardView;
    private View cipherTextToClipboardView;
    private View encryptView;
    private View decryptView;
    private CheckBox useClipboardCheckBox;
    private EncryptorFactory encryptorFactory;
    private EncryptMessageTask encryptMessageTask;
    private DecryptMessageTask decryptMessageTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.encryptorFactory = new DefaultEncryptorFactory();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_encrypt, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        plainTextView = view.findViewById(R.id.plaintext);
        cipherTextView = view.findViewById(R.id.ciphertext);

        plainTextToClipboardView = view.findViewById(R.id.plaintext_to_clipboard);
        plainTextToClipboardView.setOnClickListener(this);

        cipherTextToClipboardView = view.findViewById(R.id.ciphertext_to_clipboard);
        cipherTextToClipboardView.setOnClickListener(this);

        encryptView = view.findViewById(R.id.encrypt);
        encryptView.setOnClickListener(this);

        decryptView = view.findViewById(R.id.decrypt);
        decryptView.setOnClickListener(this);

        useClipboardCheckBox = view.findViewById(R.id.use_clipboard);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_encrypt, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_help) {
            DialogFragment helpDialogFragment = new HelpDialogFragment(R.string.help_message_encryption);
            helpDialogFragment.show(getFragmentManager(), "help");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == Activity.RESULT_OK) {
                if(encryptMessageTask != null && encryptMessageTask.getStatus() == AsyncTask.Status.PENDING) {
                    encryptMessageTask.execute(plainTextView.getText().toString());
                }
                if(decryptMessageTask != null && decryptMessageTask.getStatus() == AsyncTask.Status.PENDING) {
                    decryptMessageTask.execute(cipherTextView.getText().toString());
                }
            } else {
                Toast.makeText(getActivity(), "Could not authorize key!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if(v == encryptView) {
            if(useClipboardCheckBox.isChecked()) {
                plainTextView.setText(copyFromClipboard());
            }

            MainActivity mainActivity = (MainActivity) getActivity();
            Key key = mainActivity.getKey();

            Encryptor encryptor = encryptorFactory.messageEncryptor(key);
            TextEncryptor textEncryptor = new AndroidTextEncryptor(encryptor);
            encryptMessageTask = new EncryptMessageTask(textEncryptor);
            encryptMessageTask.execute(plainTextView.getText().toString());
        } else if(v == decryptView) {
            if(useClipboardCheckBox.isChecked()) {
                cipherTextView.setText(copyFromClipboard());
            }

            MainActivity mainActivity = (MainActivity) getActivity();
            Key key = mainActivity.getKey();

            Encryptor encryptor = encryptorFactory.messageEncryptor(key);
            TextEncryptor textEncryptor = new AndroidTextEncryptor(encryptor);
            decryptMessageTask = new DecryptMessageTask(textEncryptor);
            decryptMessageTask.execute(cipherTextView.getText().toString());
        } else if(v == plainTextToClipboardView) {
            String plainText = plainTextView.getText().toString();
            copyToClipboard(plainText);
            Toast.makeText(getContext(), R.string.plaintext_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        } else if(v == cipherTextToClipboardView) {
            String cipherText = cipherTextView.getText().toString();
            copyToClipboard(cipherText);
            Toast.makeText(getContext(), R.string.ciphertext_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     *
     */
    private void requestUserAuthentication() {
        KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        Resources resources = getResources();
        Intent intent = keyguardManager != null ? keyguardManager.createConfirmDeviceCredentialIntent(resources.getString(R.string.authorize_key), resources.getString(R.string.please_authorize_crypto_key)) : null;
        if (intent != null) {
            startActivityForResult(intent, CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    /**
     * Returns text from the clipboard.
     * @return the text from the clipboard
     */
    private CharSequence copyFromClipboard() {
        Context context = getContext();
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager != null ? clipboardManager.getPrimaryClip() : null;
        if(clipData != null && clipData.getItemCount() > 0) {
            ClipData.Item item = clipData.getItemAt(0);
            return item.coerceToText(context);
        }
        return null;
    }

    /**
     * Copies text to the clipboard.
     * @param text the text to copy to the clipboard
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("", text);
        assert clipboardManager != null;
        clipboardManager.setPrimaryClip(clipData);
    }

    /**
     *
     */
    private abstract class MessageTask extends AsyncTask<String, Double, String> {

        private final TextEncryptor textEncryptor;
        private Exception exception;

        private MessageTask(TextEncryptor textEncryptor) {
            this.textEncryptor = textEncryptor;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if(exception != null) {
                try {
                    throw exception;
                } catch (UserNotAuthenticatedException e) {
                    resetTask();
                    requestUserAuthentication();
                } catch (GeneralSecurityException | IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), DebugUtils.getFirstExceptionMessage(e), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    throw new RuntimeException(exception);
                }
            }
        }

        /**
         * Resets the task.
         */
        protected abstract void resetTask();

        /**
         * Returns the <code>TextEncryptor</code> instance.
         * @return the text encryptor
         */
        TextEncryptor getTextEncryptor() {
            return textEncryptor;
        }

        /**
         * Sets the exception that will be handled after task cancellation.
         * @param exception the exception
         */
        void setException(Exception exception) {
            this.exception = exception;
        }
    }

    /**
     *
     */
    private class EncryptMessageTask extends MessageTask {

        private EncryptMessageTask(TextEncryptor textEncryptor) {
            super(textEncryptor);
        }

        @Override
        protected void resetTask() {
            encryptMessageTask = new EncryptMessageTask(getTextEncryptor());
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                return getTextEncryptor().encrypt(params[0]);
            } catch (GeneralSecurityException e) {
                setException(e);
                cancel(true);
            }
            return null;
        }

        @Override
        public void onPostExecute(String cipherText) {
            super.onPostExecute(cipherText);
            cipherTextView.setText(cipherText);
            if(useClipboardCheckBox.isChecked()) {
                copyToClipboard(cipherText);
                Toast.makeText(getContext(), R.string.ciphertext_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     *
     */
    private class DecryptMessageTask extends MessageTask {

        private DecryptMessageTask(TextEncryptor textEncryptor) {
            super(textEncryptor);
        }

        @Override
        protected void resetTask() {
            decryptMessageTask = new DecryptMessageTask(getTextEncryptor());
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                return getTextEncryptor().decrypt(params[0]);
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                setException(e);
                cancel(true);
            }
            return null;
        }

        @Override
        public void onPostExecute(String plainText) {
            super.onPostExecute(plainText);
            plainTextView.setText(plainText);
            ((EditText)plainTextView).setSelection(plainTextView.getText().length());
            if(useClipboardCheckBox.isChecked()) {
                copyToClipboard(plainText);
                Toast.makeText(getContext(), R.string.plaintext_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
