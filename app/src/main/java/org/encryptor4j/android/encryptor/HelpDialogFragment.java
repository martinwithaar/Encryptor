package org.encryptor4j.android.encryptor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Fragment implementation for showing help dialogs.
 *
 * Created by Martin on 17-4-2017.
 */

public class HelpDialogFragment extends DialogFragment {

    private final int messageResourceId;

    public HelpDialogFragment(int messageResourceId) {
        this.messageResourceId = messageResourceId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.help);
        builder.setMessage(messageResourceId);
        return builder.create();
    }
}
