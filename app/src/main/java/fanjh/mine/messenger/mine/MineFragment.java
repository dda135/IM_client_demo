package fanjh.mine.messenger.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseFragment;
import fanjh.mine.applibrary.bean.Codes;
import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.network.NetworkWorker;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.messenger.BuildConfig;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.login.LoginActivity;
import io.reactivex.functions.Consumer;

/**
 * @author fanjh
 * @date 2017/11/30 13:47
 * @description
 * @note
 **/
public class MineFragment extends BaseFragment {

    @BindView(R.id.iv_back)
    SimpleDraweeView ivBack;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.iv_avator)
    SimpleDraweeView ivAvator;
    @BindView(R.id.tv_nickname)
    TextView tvNickname;
    @BindView(R.id.tv_birth)
    TextView tvBirth;
    @BindView(R.id.tv_sex)
    TextView tvSex;
    @BindView(R.id.tv_address)
    TextView tvAddress;
    @BindView(R.id.iv_edit)
    ImageView ivEdit;
    private View view;
    private boolean isLoaded;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null == view) {
            view = inflater.inflate(R.layout.fragment_mine, container, false);
        }
        ButterKnife.bind(this, view);
        if(!isLoaded){
            getUserMessage();
            isLoaded = true;
        }
        ivBack.setVisibility(View.GONE);
        tvTitle.setText(getActivity().getString(R.string.mine));
        return view;
    }

    private void getUserMessage(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id",UserConfig.getID());
            jsonObject.put("token",UserConfig.getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new NetworkWorker.Builder().
                url(BuildConfig.URL_PREFIX+"user/getUserMessage").
                content(jsonObject.toString()).
                build().
                execute(context, new Consumer<NetworkResultBean>() {
                    @Override
                    public void accept(NetworkResultBean networkResultBean) throws Exception {
                        if(networkResultBean.status == Codes.SUCCESS){
                            final UserBean user = GsonUtils.getInstance().fromJson(networkResultBean.data,UserBean.class);
                            UserConfig.updateUserMessage(user);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(null != user.portrait){
                                        ivAvator.setImageURI(user.portrait);
                                    }
                                    tvNickname.setText(user.nickname);
                                    tvBirth.setText(null == user.birth?getString(R.string.not_fill):user.birth);
                                    tvSex.setText(UserConfig.getSexText());
                                    tvAddress.setText(null == user.address?getString(R.string.not_fill):user.address);
                                }
                            });
                        }
                    }
                });
    }

    @OnClick({R.id.tv_title, R.id.iv_edit})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_title:
                break;
            case R.id.iv_edit:
                EditMineMessageActivity.start(context);
                break;
            default:
                break;
        }
    }
}
