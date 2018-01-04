package fanjh.mine.messenger;

import fanjh.mine.im_sdk.InstantMessengerClient;

/**
* @author fanjh
* @date 2017/12/1 9:12
* @description
* @note
**/
public class MainCrashHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler base;

    public MainCrashHandler() {
        base = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        InstantMessengerClient.getInstance().disconnect();
        if(null != base){
            base.uncaughtException(t,e);
        }
    }

}
