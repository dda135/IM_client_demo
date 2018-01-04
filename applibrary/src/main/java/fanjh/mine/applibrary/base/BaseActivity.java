package fanjh.mine.applibrary.base;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import fanjh.mine.applibrary.utils.ActivityUtils;

/**
* @author fanjh
* @date 2017/11/28 15:38
* @description
* @note
**/
public class BaseActivity extends FragmentActivity{
    protected Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        ActivityUtils.noTitle(this);
        BaseApplication.activities.add(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BaseApplication.activities.remove(this);
    }
}
