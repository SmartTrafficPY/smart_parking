package py.com.smarttraffic.gpslocator;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;


public class DetectedTransitions extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    protected static final String TAG = DetectedTransitions.class.getSimpleName();

    public DetectedTransitions() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    // Handle the callback intent in your service...
    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    broadcastActivity(event);
                }
            }
        }
    }

    private void broadcastActivity(ActivityTransitionEvent event) {
        Intent intent = new Intent(Constants.BROADCAST_DETECTED_TRANSITIONS);
        intent.putExtra("transitionType", event.getTransitionType());
        intent.putExtra("activityType", event.getActivityType());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
