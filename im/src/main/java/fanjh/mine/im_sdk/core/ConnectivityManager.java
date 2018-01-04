package fanjh.mine.im_sdk.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

import fanjh.mine.im_sdk.InstantMessengerClient;

/**
* @author fanjh
* @date 2017/11/6 15:42
* @description 统一管理网络波动
* @note
**/
public class ConnectivityManager {
    private boolean isRegistered;
    public interface OnNetworkStateChangedCallback{
        /**
         * 监听回调
         */
        void onChanged();
    }
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(android.net.ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())){
                for(OnNetworkStateChangedCallback callback:callbacks) {
                    callback.onChanged();
                }
            }
        }
    };
    private List<OnNetworkStateChangedCallback> callbacks;

    private ConnectivityManager() {
        callbacks = new ArrayList<>();
    }

    private static class ConnectivityManagerHolder{
        static final ConnectivityManager HOLDER = new ConnectivityManager();
    }

    public static ConnectivityManager getInstance(){
        return ConnectivityManagerHolder.HOLDER;
    }

    public void addCallback(OnNetworkStateChangedCallback callback){
        if(callbacks.contains(callback)){
            return;
        }
        callbacks.add(callback);
    }

    public void removeCallback(OnNetworkStateChangedCallback callback){
        callbacks.remove(callback);
    }

    public void start(){
        if(!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
            InstantMessengerClient.getApplicationContext().registerReceiver(receiver, filter);
            isRegistered = true;
        }
    }

    public void stop(){
        if(isRegistered) {
            InstantMessengerClient.getApplicationContext().unregisterReceiver(receiver);
            isRegistered = false;
        }
    }

}
