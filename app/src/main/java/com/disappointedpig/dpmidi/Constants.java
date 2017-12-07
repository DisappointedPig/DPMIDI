package com.disappointedpig.dpmidi;

public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.disappointedpig.stagecaller.action.main";

        public static String STARTCMGR_ACTION = "com.disappointedpig.stagecaller.action.startcmgr";
        public static String STOPCMGR_ACTION = "com.disappointedpig.stagecaller.action.stopcmgr";

        public static String START_MIDI_ACTION = "com.disappointedpig.stagecaller.action.startmidi";
        public static String STOP_MIDI_ACTION = "com.disappointedpig.stagecaller.action.stopmidi";
        public static String START_OSC_ACTION = "com.disappointedpig.stagecaller.action.startosc";
        public static String STOP_OSC_ACTION = "com.disappointedpig.stagecaller.action.stoposc";
    }

    public interface PREF {
        public static final String SHAREDPREFERENCES_KEY = "SCPreferences";
        public static final String MIDI_STATE_PREF = "com.disappointedpig.stagecaller.pref.MIDIState";
        public static final String BACKGROUND_STATE_PREF = "com.disappointedpig.stagecaller.pref.BackgroundState";
    }
    public interface NOTIFICATION_ID {
        public static int CONNECTIONMGR = 1120;
    }

    public static final String AB_DIALOG_FRAGMENT_KEY = "AddressBookDialogFragment";

}