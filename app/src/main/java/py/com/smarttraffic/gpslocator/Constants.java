package py.com.smarttraffic.gpslocator;

public class Constants {
    //Definitions of some variables used in the implementation...
    public static final String BROADCAST_DETECTED_ACTIVITY = "activity_intent";
    public static final String BROADCAST_DETECTED_TRANSITIONS = "transitions_intent";

    static final long SECOND_IN_MILLISECONDS = 1000; //this is the equivalent of a second in terms of milliseconds..
    static final long DETECTION_INTERVAL_IN_MILLISECONDS = 1; // Every 1 seconds (1000 milli)
    // update the states...

    public static final int MIN_CONFIDENCE = 70;

    public static final String EXTRA_MESSAGE = "py.com.smarttraffic.gpslocator.BOOLEAN";

    //Possibles states of a Polygon(parking spot)...
    public static final String UNKNOWN_STATE = "UNKNOWN";
    public static final String FREE_STATE = "FREE";
    public static final String OCCUPIED_STATE = "OCCUPIED";
    public static final String MOMENTARY_STATE = "MOMENTARY";

    public final static int NOT_USER = 0;
    public static final long MILLI_IN_SECONDS = 60000;
    public static final long LONG_TIME_CONSIDERED = 20;
    public static final int NOT_IN_PARKINGSPOT = -1;
}
