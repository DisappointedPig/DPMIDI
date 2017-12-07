package com.disappointedpig.dpmidi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.disappointedpig.dpmidi.ui.TitleWithEditText;
import com.disappointedpig.dpmidi.ui.TitleWithSwitch;
import com.disappointedpig.midi.MIDISession;

import java.util.Locale;

import static com.disappointedpig.midi.MIDIConstants.RINFO_ADDR;
import static com.disappointedpig.midi.MIDIConstants.RINFO_NAME;
import static com.disappointedpig.midi.MIDIConstants.RINFO_PORT;
import static com.disappointedpig.midi.MIDIConstants.RINFO_RECON;

public class AddressBookDialog extends AppCompatDialogFragment {

    private final static String TAG = AddressBookDialog.class.getSimpleName();

    public interface AddressBookDialogListener {
        void onAddressBookDialogPositiveClick(AppCompatDialogFragment dialog);
        void onAddressBookDialogNegativeClick(AppCompatDialogFragment dialog);
    }

    AddressBookDialogListener mListener;

    Bundle currentEntry;
    Bundle originalEntry;

    Boolean isEditing = false;

    TitleWithEditText entryName;
    TitleWithEditText entryAddress;
    TitleWithSwitch   entryRecon;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        originalEntry = this.getArguments();
        dumpEntryBundle(originalEntry);

        LayoutInflater inflater = getActivity().getLayoutInflater();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_addressbook_entry, null);

        builder.setView(view);

        String title = "New Entry";
        entryName = (TitleWithEditText) view.findViewById(R.id.abName);
        entryAddress = (TitleWithEditText) view.findViewById(R.id.abAddress);
        entryRecon = (TitleWithSwitch) view.findViewById(R.id.abReconnect);

        if(originalEntry != null) {
            isEditing = true;
            title = "Edit Entry";
            currentEntry = originalEntry;
        } else {
            currentEntry = blankEntry();
        }

        dumpEntryBundle(currentEntry);

        entryName.setEditText(currentEntry.getString(RINFO_NAME,""));

        String addressWithPort = "";
        if(currentEntry.getString(RINFO_ADDR,"") != "" ) {
            addressWithPort = String.format(Locale.US,"%s:%d",currentEntry.getString(RINFO_ADDR),currentEntry.getInt(RINFO_PORT));
        }
        entryAddress.setEditText(addressWithPort);

        entryRecon.setSwitch(currentEntry.getBoolean(RINFO_RECON,false));



        builder.setMessage(title)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        currentEntry.putString(RINFO_NAME,entryName.getEditText());
                        String addressWithPort = entryAddress.getEditText();
                        if(addressWithPort.indexOf(":") >= 0) {
                            String[] addressWithPortArray = entryAddress.getEditText().split(":");
                            if(addressWithPortArray.length == 2) {
                                currentEntry.putInt(RINFO_PORT, Integer.parseInt(addressWithPortArray[1]));
                                currentEntry.putString(RINFO_ADDR, addressWithPortArray[0]);
                            }
                        }
                        MIDISession.getInstance().addToAddressBook(currentEntry);
                        mListener.onAddressBookDialogPositiveClick(AddressBookDialog.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (AddressBookDialogListener) activity;

        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement TriggerDialogListener");
        }
    }

    private Bundle blankEntry() {
        Bundle b = new Bundle();

        b.putString(RINFO_NAME,"");
        b.putString(RINFO_ADDR,"127.0.0.1");
        b.putInt(RINFO_PORT,5004);
        b.putBoolean(RINFO_RECON,false);

        return b;
    }
    private void dumpEntryBundle(Bundle b) {
        if(b == null) return;
//        triggerBundle.putString(TRIGGER_VALUE_KEY, thisAction.getString(ACTION_START_TRIGGER_VALUE_KEY)); // \/cue\/4\/start     43
//        triggerBundle.putString(TRIGGER_KEY_KEY,thisAction.getString(ACTION_START_TRIGGER_KEY_KEY)); //    midi43   osc\/cue\/4\/start
//        triggerBundle.putInt(TRIGGER_TYPE_KEY,thisAction.getInt(ACTION_START_TRIGGER_TYPE_KEY)); // 0: MIDI, 1: OSC
//        triggerBundle.putInt(TRIGGER_COMMAND_KEY, 1); // 0: stop, 1: start  (purpose?)
//        triggerBundle.putString(TRIGGER_DESC_KEY, thisAction.getString(ACTION_START_TRIGGER_DESC_KEY)); // OSC:/cue/4/start     MIDI:note on:43
        Log.d(TAG,"-------------Entry Dump------------");
        Log.d(TAG,"RINFO_NAME: "+b.getString(RINFO_NAME,"(null)"));
        Log.d(TAG,"RINFO_PORT: "+(b.getInt(RINFO_PORT,0) == 0 ? "(0)" : b.getInt(RINFO_PORT)));
        Log.d(TAG,"RINFO_ADDR: "+b.getString(RINFO_ADDR,"(null)"));
        Log.d(TAG,"RINFO_RECON: "+(b.getBoolean(RINFO_RECON) ? "true" : "false"));
//        Log.d(TAG,"RINFO_FAIL: "+b.getBoolean(RINFO_FAIL) ? "true" : "false");
        Log.d(TAG,"-------------------------------------");

    }

    public Bundle getRinfo() {
        return currentEntry;
    }
}