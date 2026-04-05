package com.example.plantmap.fragments;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class HelpDialogFragment extends DialogFragment {
    public static final String TAG = "help_dialog";
    private static final String ARG_MESSAGE_RES = "message_res";

    public static HelpDialogFragment newInstance(@StringRes int messageRes) {
        HelpDialogFragment fragment = new HelpDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_RES, messageRes);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        int messageRes = args != null ? args.getInt(ARG_MESSAGE_RES) : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Справка")
                .setPositiveButton("ОК", null);

        if (messageRes != 0) {
            builder.setMessage(getString(messageRes));
        }

        return builder.create();
    }
}