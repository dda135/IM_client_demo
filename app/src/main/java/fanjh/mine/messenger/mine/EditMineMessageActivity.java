package fanjh.mine.messenger.mine;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseActivity;
import fanjh.mine.applibrary.bean.Codes;
import fanjh.mine.applibrary.bean.ImageEntity;
import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.network.NetworkWorker;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.applibrary.utils.ImageChooser;
import fanjh.mine.messenger.BuildConfig;
import fanjh.mine.messenger.R;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * @author fanjh
 * @date 2017/12/6 16:12
 * @description
 * @note
 **/
public class EditMineMessageActivity extends BaseActivity {

    @BindView(R.id.iv_back)
    SimpleDraweeView ivBack;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.tv_select_image)
    TextView tvSelectImage;
    @BindView(R.id.tv_mobile)
    TextView tvMobile;
    @BindView(R.id.et_nickname)
    EditText etNickname;

    @BindView(R.id.et_address)
    EditText etAddress;
    @BindView(R.id.btn_submit)
    Button btnSubmit;
    @BindView(R.id.iv_avator)
    SimpleDraweeView ivAvator;
    @BindView(R.id.rb_man)
    RadioButton rbMan;
    @BindView(R.id.rb_woman)
    RadioButton rbWoman;
    @BindView(R.id.rg_sex)
    RadioGroup rgSex;

    private ImageChooser imageChooser;
    private String avatorPath;
    private String sex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mine_message);
        ButterKnife.bind(this);
        rgSex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId){
                    case R.id.rb_man:
                        sex = "0";
                        break;
                    case R.id.rb_woman:
                        sex = "1";
                        break;
                    default:
                        break;
                }
            }
        });
        String avator = UserConfig.getPortrait();
        if (null != avator) {
            ivAvator.setImageURI(avator);
        }
        etNickname.setText(UserConfig.getNickname());
        tvMobile.setText(UserConfig.getMobile());
        sex = UserConfig.getSex();

        if(UserConfig.isMan()){
            rbMan.setChecked(true);
        }else{
            rbWoman.setChecked(true);
        }
        etAddress.setText(UserConfig.getAddress());
        tvTitle.setText(getString(R.string.update_data));
    }

    @OnClick({R.id.iv_back, R.id.tv_title, R.id.tv_select_image, R.id.btn_submit})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.tv_title:
                break;
            case R.id.tv_select_image:
                if (null == imageChooser) {
                    imageChooser = new ImageChooser(this);
                    imageChooser.setResultListener(new ImageChooser.OnResultListener() {
                        @Override
                        public void onError() {
                            Toast.makeText(getApplicationContext(), "获取图片失败！", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onSuccess(ImageEntity entity) {
                            avatorPath = entity.getFilePath();
                            ivAvator.setImageURI("file://" + avatorPath);
                        }
                    });
                }
                imageChooser.chooseGallery();
                break;
            case R.id.btn_submit:
                String address = etAddress.getText().toString().trim();
                if (TextUtils.isEmpty(address)) {
                    Toast.makeText(getApplicationContext(), "地址不能为空！", Toast.LENGTH_LONG).show();
                    return;
                }
                String nickname = etNickname.getText().toString().trim();
                if (TextUtils.isEmpty(nickname)) {
                    Toast.makeText(getApplicationContext(), "昵称不能为空！", Toast.LENGTH_LONG).show();
                    return;
                }
                UserBean bean = new UserBean();
                bean.id = UserConfig.getID();
                bean.address = address;
                bean.nickname = nickname;
                bean.sex = sex;
                bean.portrait = UserConfig.getPortrait();
                //bean.birth =
                Map<String, String> files = new HashMap<>();
                files.put("avator", avatorPath);
                new NetworkWorker.Builder().
                        url(BuildConfig.URL_PREFIX + "user/updateUserMessage").
                        isShowDialog(true).
                        content(GsonUtils.getInstance().toJson(bean)).
                        callInMainThread(true).
                        files(files).
                        build().execute(context, new Consumer<NetworkResultBean>() {
                    @Override
                    public void accept(NetworkResultBean networkResultBean) throws Exception {
                        if (networkResultBean.status == Codes.SUCCESS) {
                            Toast.makeText(getApplicationContext(), "修改成功！", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), networkResultBean.hint, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (null != imageChooser) {
            imageChooser.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, EditMineMessageActivity.class);
        context.startActivity(intent);
    }

}
