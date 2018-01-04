package fanjh.mine.messenger;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import java.util.ArrayList;
import java.util.List;

import fanjh.mine.applibrary.base.BaseApplication;
import fanjh.mine.applibrary.utils.ThreadUtils;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.messenger.im.CommonReceiver;

/**
 * @author fanjh
 * @date 2017/11/28 16:37
 * @description
 * @note
 **/
public class MainApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new MainCrashHandler());
        CommonReceiver.init();
        Fresco.initialize(this);
        InstantMessengerClient.attach(this);
    }


}
