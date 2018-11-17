package py.com.smarttraffic.gpslocator;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class NotificationHandler extends ContextWrapper {

    public static final String CHANNEL_HIGH_ID = "1";
    private final String CHANNEL_HIGH_NAME = "HIGH_CHANNEL";
    public static final String CHANNEL_LOW_ID = "2";
    private final String CHANNEL_LOW_NAME = "LOW_CHANNEL";

    public NotificationHandler(Context context) {
        super(context);
        createChannels();
    }

    private NotificationManager manager;

    private void createChannels(){
        if(Build.VERSION.SDK_INT >= 26){
            //Creating the High Channel...
            @SuppressLint("WrongConstant") NotificationChannel highChannel = new NotificationChannel(
                    CHANNEL_HIGH_ID,CHANNEL_HIGH_NAME,NotificationManager.IMPORTANCE_MAX);
            // ...Extra Config...
            highChannel.enableLights(true);
            highChannel.setLightColor(Color.BLUE);
            highChannel.setShowBadge(true);
            highChannel.enableVibration(true);
            highChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            highChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            getManager().createNotificationChannel(highChannel);
        }
    }

    public NotificationManager getManager() {
        if(manager == null){
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    public Notification.Builder createNotification(String title, String message){
        if(Build.VERSION.SDK_INT >= 26){
            createNotificationWithChannel(title, message, CHANNEL_HIGH_ID);
        }
        return this.createNotificationWithoutChannel(title, message);
    }

    private Notification.Builder createNotificationWithChannel(String title, String message, String channelId){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return new Notification.Builder(getApplicationContext(), channelId)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setAutoCancel(true);
        }
        return null;
    }

    private Notification.Builder createNotificationWithoutChannel(String title, String message){
        return new Notification.Builder(getApplicationContext())
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setAutoCancel(true);
    }

}
