package fanjh.mine.messenger.splash;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fanjh.mine.applibrary.base.BaseActivity;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.utils.BusinessUtils;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.TabActivity;
import fanjh.mine.messenger.im.CommonNodePuller;
import fanjh.mine.messenger.im.PreLoaderWorker;
import fanjh.mine.messenger.login.LoginActivity;

/**
 * @author fanjh
 * @date 2017/11/28 16:33
 * @description
 * @note
 **/
public class SplashActivity extends BaseActivity {

    @BindView(R.id.sdv_image)
    SimpleDraweeView sdvImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        int id = UserConfig.getID();
        if(id <= 0){
            LoginActivity.start(context);
            finish();
        }else {
            new PreLoaderWorker(context).execute(new Runnable() {
                @Override
                public void run() {
                    finish();
                    TabActivity.start(context);
                }
            });
            InstantMessengerClient.getInstance().start(CommonNodePuller.getInstance(),
                    BusinessUtils.getClientID(id, UserConfig.getToken()));
        }
    }
}
