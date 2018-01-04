package fanjh.mine.applibrary.base;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.xiasuhuei321.loadingdialog.manager.StyleManager;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

import java.util.ArrayList;
import java.util.List;

/**
* @author fanjh
* @date 2017/11/28 17:02
* @description
* @note
**/
public class BaseApplication extends Application{
    public static Application application;
    public static List<Activity> activities = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        application = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StyleManager s = new StyleManager();
        s.Anim(true).
                failedText("......").
                successText("......").
                textSize(1).
                repeatTime(0).
                contentSize(-1).
                intercept(true);
        LoadingDialog.initStyle(s);
    }
}
