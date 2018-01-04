package fanjh.mine.applibrary.utils;

import android.os.Looper;

/**
* @author fanjh
* @date 2017/11/29 11:17
* @description
* @note
**/
public class ThreadUtils {

    public static boolean isMainThread(){
        return Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper();
    }

}
