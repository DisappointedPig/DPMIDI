package com.disappointedpig.dpmidi;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.disappointedpig.midi.MIDIAddressBookEntry;
import com.disappointedpig.midi.MIDISession;

import org.greenrobot.eventbus.EventBus;

import static com.disappointedpig.dpmidi.AddressBookEventType.DELETE;
import static com.disappointedpig.dpmidi.AddressBookEventType.TOUCHED;
import static com.disappointedpig.dpmidi.AddressBookEventType.UPDATED;

public class AddressBookModel implements ViewModel {

    public MIDIAddressBookEntry entry;

    public static AddressBookModel newInstance(MIDIAddressBookEntry a) {
        AddressBookModel abm = new AddressBookModel();
        abm.entry = a;

        return abm;
    }

    @Override
    public int layoutId() {
        return R.layout.row_address_book_entry;
    }

    public String dataId() { return entry.getAddressPort(); }

    public String getAddress() { return entry.getAddress(); }

    public String getAddressPort() { return entry.getAddressPort(); }

    public int getPort() { return entry.getPort(); }

    public boolean getReconnect() { return entry.getReconnect(); }

    public String getName() { return entry.getName(); }

    public Bundle getRinfo() { return  entry.rinfo(); }

    public void onClickDelete(View view) {
        EventBus.getDefault().post(new AddressBookEvent(DELETE,entry));

    }


    public void onClickEntry(View view) {
        EventBus.getDefault().post(new AddressBookEvent(TOUCHED,entry));
    }

    public void onCheckedChanged(boolean checked) {
        Log.d("AddressBookModel","checked: "+(checked ? "ON" : "OFF"));
        entry.setReconnect(checked);

        MIDISession.getInstance().addToAddressBook(entry);
        EventBus.getDefault().post(new AddressBookEvent(UPDATED,entry));

    }


}