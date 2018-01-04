package fanjh.mine.messenger.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseActivity;
import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.network.NetworkWorker;
import fanjh.mine.applibrary.utils.BusinessUtils;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.messenger.BuildConfig;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.TabActivity;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.messenger.im.CommonNodePuller;
import fanjh.mine.messenger.im.PreLoaderWorker;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * @author fanjh
 * @date 2017/11/28 17:12
 * @description
 * @note
 **/
public class LoginActivity extends BaseActivity {
    @BindView(R.id.et_telephone)
    EditText etTelephone;
    @BindView(R.id.et_password)
    EditText etPassword;
    @BindView(R.id.btn_register)
    Button btnRegister;
    @BindView(R.id.btn_login)
    Button btnLogin;
    @BindView(R.id.fl_parent_layout)
    FrameLayout flParentLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_register, R.id.btn_login})
    public void onClick(View view) {
        String mobile = etTelephone.getText().toString();
        String password = etPassword.getText().toString();
        if(TextUtils.isEmpty(mobile)){
            Toast.makeText(getApplicationContext(),"手机号不能为空！",Toast.LENGTH_LONG).show();
            return;
        }
        if(TextUtils.isEmpty(password)){
            Toast.makeText(getApplicationContext(),"密码不能为空！",Toast.LENGTH_LONG).show();
            return;
        }
        UserBean userBean = new UserBean();
        userBean.mobile = mobile;
        userBean.password = password;
        switch (view.getId()) {
            case R.id.btn_register:
                new NetworkWorker.Builder().
                        url(BuildConfig.URL_PREFIX + "user/regist").
                        content(GsonUtils.getInstance().toJson(userBean)).
                        tag(getClass().getSimpleName()).
                        isShowDialog(true).
                        build().
                        execute(this,new Consumer<NetworkResultBean>() {
                    @Override
                    public void accept(NetworkResultBean networkResultBean) throws Exception {
                        handleSuccess(networkResultBean);
                    }
                });
                break;
            case R.id.btn_login:
                new NetworkWorker.Builder().
                        url(BuildConfig.URL_PREFIX + "user/login").
                        content(GsonUtils.getInstance().toJson(userBean)).
                        tag(getClass().getSimpleName()).
                        isShowDialog(true).
                        build().
                        execute(this,new Consumer<NetworkResultBean>() {
                            @Override
                            public void accept(NetworkResultBean networkResultBean) throws Exception {
                                handleSuccess(networkResultBean);
                            }
                        });
                break;
            default:
                break;
        }
    }

    private void handleSuccess(NetworkResultBean networkResultBean){
        if(networkResultBean.status == 1) {
            UserBean userBean = GsonUtils.getInstance().fromJson(networkResultBean.data, UserBean.class);
            UserConfig.updateUserMessage(userBean);
            InstantMessengerClient.getInstance().start(CommonNodePuller.getInstance(),
                    BusinessUtils.getClientID(userBean.id, userBean.token.tokenValue));
        }
        Observable.just(networkResultBean).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<NetworkResultBean>() {
            @Override
            public void accept(NetworkResultBean networkResultBean) throws Exception {
                if(networkResultBean.status == 1) {
                    new PreLoaderWorker(context).execute(new Runnable() {
                        @Override
                        public void run() {
                            TabActivity.start(context);
                            finish();
                        }
                    });
                }else {
                    Toast.makeText(getApplicationContext(), networkResultBean.hint, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public static void start(Context context){
        Intent intent = new Intent(context,LoginActivity.class);
        context.startActivity(intent);
    }

}
