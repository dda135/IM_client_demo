package fanjh.mine.im_sdk.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fanjh.mine.client.Utils;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.im_sdk.tools.Logger;

/**
* @author fanjh
* @date 2017/11/3 17:31
* @description 前台检查者
* @note
**/
public class ForegroundChecker {
    public static final String TAG = "ForegroundChecker";
    private Application application;
    private boolean isRunning;
    private int resumeCount;
    private List<Callback> listeners;
    private boolean isForeground = true;

    public boolean isForeground(){
        return isForeground;
    }

    public static class Holder{
        static ForegroundChecker INSTANCE = new ForegroundChecker();
    }

    public static ForegroundChecker getInstance(){
        return Holder.INSTANCE;
    }

    public void addListener(Callback callback){
        if(null == listeners){
            listeners = new ArrayList<>();
        }
        if(!listeners.contains(callback)) {
            listeners.add(callback);
        }
    }

    public void removeListener(Callback callback){
        if(null != listeners){
            listeners.remove(callback);
        }
    }

    public interface Callback{
        void callForeground();
        void callBackground();
    }

    public void attach(Application application) {
        this.application = application;
    }

    public void start(){
        if(isRunning){
            return;
        }
        isRunning = true;
        application.registerActivityLifecycleCallbacks(callbacks);
    }

    public void pause(){
        if(!isRunning){
            return;
        }
        isRunning = false;
        application.unregisterActivityLifecycleCallbacks(callbacks);
    }

    private Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
            if(resumeCount == 0){
                isForeground = true;
                callForeground();
                Utils.LogDebug(TAG,"callForeground");
            }
            resumeCount++;
            Utils.LogDebug(TAG,"resumeCount-->"+resumeCount);
        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            resumeCount--;
            if(resumeCount == 0) {
                isForeground = false;
                Utils.LogDebug(TAG,"callBackground");
                callBackground();
            }
            Utils.LogDebug(TAG,"resumeCount-->"+resumeCount);
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };

    private void callBackground(){
        if(null != listeners){
            for(Callback callback:listeners){
                callback.callBackground();
            }
        }
    }

    private void callForeground(){
        if(null != listeners){
            for(Callback callback:listeners){
                callback.callForeground();
            }
        }
    }

}
