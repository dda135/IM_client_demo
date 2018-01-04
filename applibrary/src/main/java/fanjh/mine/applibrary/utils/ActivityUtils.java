package fanjh.mine.applibrary.utils;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

/**
* @author fanjh
* @date 2017/11/28 15:48
* @description
* @note
**/
public class ActivityUtils {

    public static void noTitle(Activity activity){
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public static void fullScreen(Activity activity){
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

}
