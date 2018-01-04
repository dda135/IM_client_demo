package fanjh.mine.im_sdk.core;

import android.content.Intent;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import fanjh.mine.im_sdk.InstantMessengerClient;

/**
* @author fanjh
* @date 2017/11/23 18:09
* @description
* @note
**/
public class NotifyAction {
    public static final String ACTION = "action.mine.fanjh.imsdk.notify";
    public static final String EXTRA_MSG = "extra_msg";
    public static final String EXTRA_CONTENT = "extra_content";
    public static final String EXTRA_LISTENER_ID = "extra_listener_id";
    public static final int MSG_SEND_SUCCESS = 1;
    public static final int MSG_SEND_FAILURE = 2;
    public static final int MSG_MESSAGE_ARRIVED = 3;

    @IntDef({MSG_SEND_SUCCESS,MSG_SEND_FAILURE,MSG_MESSAGE_ARRIVED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action{}

    public static void notifyAction(@NotifyAction.Action int action,String content){
        Intent intent = new Intent(ACTION);
        intent.putExtra(EXTRA_MSG,action);
        intent.putExtra(EXTRA_CONTENT,content);
        InstantMessengerClient.getApplicationContext().sendBroadcast(intent);
    }

    public static void notifyAction(@NotifyAction.Action int action,String content,long listenerID){
        Intent intent = new Intent(ACTION);
        intent.putExtra(EXTRA_MSG,action);
        intent.putExtra(EXTRA_CONTENT,content);
        intent.putExtra(EXTRA_LISTENER_ID,listenerID);
        InstantMessengerClient.getApplicationContext().sendBroadcast(intent);
    }

}
