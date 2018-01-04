package fanjh.mine.im_sdk.tools;


import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fanjh.mine.im_sdk.InstantMessengerClient;

/**
 * @author fanjh
 * @date 2017/10/31 18:11
 * @description 通用工具
 * @note
 **/
public class Utils {
    private static ThreadPoolExecutor FIX_POOL_EXECUTOR;
    private static ThreadPoolExecutor SINGLE_POOL_EXECUTOR;
    private static Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface CacheResultListener<T extends Serializable> {
        void onCall(@NonNull T object);
    }

    public static void runOnMainThread(Runnable runnable){
        MAIN_HANDLER.post(runnable);
    }

    public static ThreadPoolExecutor getFixThreadPoolExecutor() {
        if (null == FIX_POOL_EXECUTOR) {
            synchronized (Utils.class) {
                if (null == FIX_POOL_EXECUTOR) {
                    int core = Math.max(1,Runtime.getRuntime().availableProcessors() - 1);
                    FIX_POOL_EXECUTOR = new ThreadPoolExecutor(core, core,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                        @Override
                        public Thread newThread(@NonNull Runnable r) {
                            Thread thread = new Thread(r);
                            thread.setName("Utils_Fix_Thread:" + thread.getId());
                            return thread;
                        }
                    });
                }
            }
        }
        return FIX_POOL_EXECUTOR;
    }

    public static ThreadPoolExecutor getSingleThreadPoolExecutor() {
        if (null == SINGLE_POOL_EXECUTOR) {
            synchronized (Utils.class) {
                if (null == SINGLE_POOL_EXECUTOR) {
                    SINGLE_POOL_EXECUTOR = new ThreadPoolExecutor(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                        @Override
                        public Thread newThread(@NonNull Runnable r) {
                            Thread thread = new Thread(r);
                            thread.setName("Utils_Single_Thread:" + thread.getId());
                            return thread;
                        }
                    });
                }
            }
        }
        return FIX_POOL_EXECUTOR;
    }

    public static boolean isMainProcess(){
        int pid = android.os.Process.myPid();
        String mainProcessName = InstantMessengerClient.getApplicationContext().getApplicationContext().getApplicationInfo().processName;
        ActivityManager manager = (ActivityManager) InstantMessengerClient.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return processInfo.processName.equals(mainProcessName);
            }
        }
        return false;
    }

    public static boolean isOnline(){
        ConnectivityManager cm = (ConnectivityManager) InstantMessengerClient.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null
                && networkInfo.isAvailable()
                && networkInfo.isConnected()) {
            return true;
        }

        return false;
    }

}
