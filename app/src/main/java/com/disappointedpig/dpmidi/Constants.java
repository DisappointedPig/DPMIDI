package com.disappointedpig.dpmidi;

public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.disappointedpig.dpmidi.action.main";
        public static String PREV_ACTION = "com.disappointedpig.dpmidi.action.prev";
        public static String START_ACTION = "com.disappointedpig.dpmidi.action.play";
        public static String STOP_ACTION = "com.disappointedpig.dpmidi.action.next";
        public static String STARTFOREGROUND_ACTION = "com.disappointedpig.dpmidi.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.disappointedpig.dpmidi.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int MIDI_SERVICE = 101;
    }
}
