package fanjh.mine.applibrary.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

/**
 * @author fanjh
 * @date 2017/12/1 16:55
 * @description
 * @note
 **/
public class NotifyUtils {

    public static void showNotification(Context context,int res,String title,String text,int notifyID) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(res);
        builder.setContentTitle(title);
        builder.setContentText(text);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(notifyID,builder.build());
    }

}
