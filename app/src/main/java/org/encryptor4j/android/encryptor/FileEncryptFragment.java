package org.encryptor4j.android.encryptor;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import org.encryptor4j.Encryptor;
import org.encryptor4j.android.factory.DefaultEncryptorFactory;
import org.encryptor4j.android.util.DebugUtils;
import org.encryptor4j.factory.EncryptorFactory;
import org.encryptor4j.util.FileEncryptor;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStoreException;

/**
 * Fragment implementation for encrypting files.
 *
 * Created by Martin on 31-5-2016.
 */
public class FileEncryptFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "FileEncryptFragment";
    private static final int CHOOSE_FILE = 0x01;
    private static final int CONFIRM_DEVICE_CREDENTIALS = 0x02;
    private static final int PERMISSION_REQUEST = 0x02;
    private static final String ENCRYPTED_EXTENSION = ".encrypted";

    private TextView filenameView;
    private View selectFileButton;
    private View encryptButton;
    private View decryptButton;
    private EncryptorFactory encryptorFactory;
    private EncryptFileTask encryptFileTask;
    private DecryptFileTask decryptFileTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, PERMISSION_REQUEST);

        this.encryptorFactory = new DefaultEncryptorFactory();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_encrypt, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        filenameView = view.findViewById(R.id.filename);

        selectFileButton = view.findViewById(R.id.select_file);
        selectFileButton.setOnClickListener(this);

        encryptButton = view.findViewById(R.id.encrypt);
        encryptButton.setOnClickListener(this);

        decryptButton = view.findViewById(R.id.decrypt);
        decryptButton.setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHOOSE_FILE:
                if(resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    if(uri != null) {
                        filenameView.setText(uri.getPath());
                    }
                }
                break;
            case CONFIRM_DEVICE_CREDENTIALS:
                if (resultCode == Activity.RESULT_OK) {
                    if(encryptFileTask != null && encryptFileTask.getStatus() == AsyncTask.Status.PENDING) {
                        File sourceFile = getValidSourceFile(null);
                        if(sourceFile != null) {
                            File destinationFile = getEncryptDestinationFile(sourceFile);
                            encryptFileTask.execute(sourceFile, destinationFile);
                        }
                    }

                    if(decryptFileTask != null && decryptFileTask.getStatus() == AsyncTask.Status.PENDING) {
                        File sourceFile = getValidSourceFile(ENCRYPTED_EXTENSION);
                        if(sourceFile != null) {
                            File destinationFile = getDecryptDestinationFile(sourceFile);
                            decryptFileTask.execute(sourceFile, destinationFile);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_encrypt, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_help) {
            DialogFragment helpDialogFragment = new HelpDialogFragment(R.string.help_file_encryption);
            helpDialogFragment.show(getFragmentManager(), "help");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == selectFileButton) {
            Intent intent = new Intent();
            intent.setType("*/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, CHOOSE_FILE);
        } else if (v == encryptButton) {
            File sourceFile = getValidSourceFile(null);
            if(sourceFile != null) {
                MainActivity mainActivity = (MainActivity) getActivity();
                Key key = mainActivity.getKey();

                Encryptor encryptor = encryptorFactory.streamEncryptor(key);
                encryptFileTask = new EncryptFileTask(new FileEncryptor(encryptor));

                File destinationFile = getEncryptDestinationFile(sourceFile);
                encryptFileTask.execute(sourceFile, destinationFile);
            }
        } else if (v == decryptButton) {
            File sourceFile = getValidSourceFile(ENCRYPTED_EXTENSION);
            if(sourceFile != null) {
                MainActivity mainActivity = (MainActivity) getActivity();
                Key key = mainActivity.getKey();

                Encryptor encryptor = encryptorFactory.streamEncryptor(key);
                decryptFileTask = new DecryptFileTask(new FileEncryptor(encryptor));

                File destinationFile = getDecryptDestinationFile(sourceFile);
                decryptFileTask.execute(sourceFile, destinationFile);
            }
        }
    }

    /**
     * Checks and returns a selected file if it is found to be valid.
     * @param extensionCheck    optional extension check the file has to adhere to
     * @return the checked to be valid file
     */
    private File getValidSourceFile(String extensionCheck) {
        String filepath = filenameView.getText().toString();
        if (!filepath.isEmpty()) {
            if (extensionCheck == null || filepath.endsWith(extensionCheck)) {
                File src = new File(filepath);
                if(src.exists()) {
                    return src;
                } else {
                    filenameView.setError(getResources().getString(R.string.file_does_not_exist));
                }
            } else {
                filenameView.setError(getResources().getString(R.string.file_must_end_with_ext));
            }
        } else {
            filenameView.setError(getResources().getString(R.string.filepath_required));
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST:
                boolean writeExtStorageGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
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
        Intent intent = keyguardManager != null ? keyguardManager.createConfirmDeviceCredentialIntent(
                resources.getString(R.string.authorize_key),
                resources.getString(R.string.please_authorize_crypto_key)
        ) : null;

        if (intent != null) {
            startActivityForResult(intent, CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    private abstract class FileTask extends AsyncTask<File, Double, Void> {
        private final FileEncryptor fileEncryptor;
        private Exception exception;

        private FileTask(FileEncryptor fileEncryptor) {
            this.fileEncryptor = fileEncryptor;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (exception != null) {
                try {
                    throw exception;
                } catch (UserNotAuthenticatedException | KeyStoreException e) {
                    resetTask();
                    requestUserAuthentication();
                } catch (GeneralSecurityException e) {
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
         * Returns the <code>FileEncryptor</code> instance.
         * @return
         */
        FileEncryptor getFileEncryptor() {
            return fileEncryptor;
        }

        /**
         * Sets the exception that will be handled after task cancellation.
         * @param exception
         */
        void setException(Exception exception) {
            this.exception = exception;
        }
    }

    /**
     *
     */
    private class EncryptFileTask extends FileTask {

        private EncryptFileTask(FileEncryptor fileEncryptor) {
            super(fileEncryptor);
        }

        @Override
        protected void resetTask() {
            encryptFileTask = new EncryptFileTask(getFileEncryptor());
        }

        @Override
        protected Void doInBackground(File... params) {
            try {
                getFileEncryptor().encrypt(params[0], params[1]);
            } catch (GeneralSecurityException | IOException e) {
                setException(e);
                cancel(true);
            }
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            super.onPostExecute(result);
            Toast.makeText(getActivity(), R.string.file_encrypted_successfully, Toast.LENGTH_LONG).show();
        }
    }

    private class DecryptFileTask extends FileTask {

        private DecryptFileTask(FileEncryptor fileEncryptor) {
            super(fileEncryptor);
        }

        @Override
        protected void resetTask() {
            decryptFileTask = new DecryptFileTask(getFileEncryptor());
        }

        @Override
        protected Void doInBackground(File... params) {
            try {
                getFileEncryptor().decrypt(params[0], params[1]);
            } catch (GeneralSecurityException | IOException e) {
                setException(e);
                cancel(true);
            }
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            super.onPostExecute(result);
            Toast.makeText(getActivity(), R.string.file_decrypted_successfully, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Returns the destination file for encryption.
     * @param sourceFile the source file
     * @return the destination file
     */
    private static File getEncryptDestinationFile(File sourceFile) {
        return new File(sourceFile.getAbsolutePath() + ENCRYPTED_EXTENSION);
    }

    /**
     * Returns the destination file for decryption.
     * @param sourceFile the source file
     * @return the destination file
     */
    private static File getDecryptDestinationFile(File sourceFile) {
        String filepath = sourceFile.getAbsolutePath();
        return new File(filepath.substring(0, filepath.length() - ENCRYPTED_EXTENSION.length()));
    }
}
