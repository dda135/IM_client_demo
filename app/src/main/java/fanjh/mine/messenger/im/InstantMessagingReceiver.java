package fanjh.mine.messenger.im;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fanjh.mine.im_sdk.core.NotifyAction;

/**
* @author fanjh
* @date 2017/11/29 17:55
* @description
* @note
**/
public class InstantMessagingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(NotifyAction.ACTION.equals(action)){
            CommonReceiver.MESSAGE_DELIVERY_HANDLER.sendMessage(CommonReceiver.MESSAGE_DELIVERY_HANDLER.obtainMessage(0,intent));
        }
    }
}
