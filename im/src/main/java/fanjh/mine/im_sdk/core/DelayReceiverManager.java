package fanjh.mine.im_sdk.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.im_sdk.tools.Utils;

/**
* @author fanjh
* @date 2017/11/3 18:28
* @description 延时通知
* @note
**/
public class DelayReceiverManager {
    private String alarmAction;
    private boolean isAlarmReceiverRegistered;
    private PendingIntent alarmPendingIntent;
    private ReceiverCallback receiverCallback;

    public DelayReceiverManager(String alarmAction,ReceiverCallback receiverCallback) {
        this.alarmAction = alarmAction;
        if(null == receiverCallback){
            throw new IllegalArgumentException("必须关心通知结果！否则你不应该使用这个！");
        }
        this.receiverCallback = receiverCallback;
    }

    public synchronized void tryStartDelayedReceiver(long alarmInterval) {
        if (!isAlarmReceiverRegistered) {
            Context context = InstantMessengerClient.getApplicationContext();
            IntentFilter intent = new IntentFilter();
            intent.addAction(alarmAction);
            context.registerReceiver(delayedReceiver, intent);
            if (null == alarmPendingIntent) {
                Intent alarmIntent = new Intent(alarmAction);
                alarmPendingIntent = PendingIntent.getBroadcast(InstantMessengerClient.getApplicationContext(), 0,
                        alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            isAlarmReceiverRegistered = true;
            schedule(alarmInterval);
        }
    }

    public synchronized void cancelStartDelayedReceiver() {
        if (isAlarmReceiverRegistered) {
            Context context = InstantMessengerClient.getApplicationContext();
            context.unregisterReceiver(delayedReceiver);
            isAlarmReceiverRegistered = false;
        }
    }

    public void cancelSchedule(){
        AlarmManager alarmManager = (AlarmManager) InstantMessengerClient.getApplicationContext().getSystemService(Service.ALARM_SERVICE);
        alarmManager.cancel(alarmPendingIntent);
    }

    public void schedule(long alarmInterval) {
        long nextAlarmInMilliseconds = System.currentTimeMillis()
                + alarmInterval;
        //Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
        AlarmManager alarmManager = (AlarmManager) InstantMessengerClient.getApplicationContext().getSystemService(Service.ALARM_SERVICE);
        alarmManager.cancel(alarmPendingIntent);

        if (Build.VERSION.SDK_INT >= 23) {
            // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
            // the device to run this task whilst dosing.
            //Log.d(TAG, "Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    alarmPendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //Log.d(TAG, "Alarm scheule using setExact, delay: " + delayInMilliseconds);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    alarmPendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    alarmPendingIntent);
        }
    }

    private BroadcastReceiver delayedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {
            Utils.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    if (isAlarmReceiverRegistered) {
                        String action = intent.getAction();
                        if (alarmAction.equals(action)) {
                            receiverCallback.onAlarmCall();
                        }
                    }
                }
            });
        }
    };

    public interface ReceiverCallback{

        /**
         * 定时器通知
         */
        void onAlarmCall();
    }

}
