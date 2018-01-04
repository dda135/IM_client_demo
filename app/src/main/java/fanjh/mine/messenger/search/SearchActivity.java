package fanjh.mine.messenger.search;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.reflect.TypeToken;
import com.hwangjr.rxbus.RxBus;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseActivity;
import fanjh.mine.applibrary.bean.friend.FriendApplyBean;
import fanjh.mine.applibrary.bean.friend.FriendBean;
import fanjh.mine.applibrary.bean.message.ApplyMessage;
import fanjh.mine.applibrary.bean.message.BaseMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.bean.rxbus.FriendApplyListRefreshBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.db.friend.FriendDBHelper;
import fanjh.mine.applibrary.network.NetworkWorker;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.client.IMConnector;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.messenger.BuildConfig;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.friend.UserAdapter;
import fanjh.mine.messenger.im.CommonReceiver;
import fanjh.mine.messenger.im.SendMessageResultListener;
import io.reactivex.functions.Consumer;

/**
 * @author fanjh
 * @date 2017/11/30 15:02
 * @description
 * @note
 **/
public class SearchActivity extends BaseActivity {

    @BindView(R.id.iv_back)
    SimpleDraweeView ivBack;
    @BindView(R.id.et_search)
    EditText etSearch;
    @BindView(R.id.rv_content)
    RecyclerView rvContent;
    private List<UserBean> items = new ArrayList<>();
    private UserAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        rvContent.setLayoutManager(new LinearLayoutManager(context));
        adapter = new UserAdapter(R.layout.item_user,items);
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                final UserBean userBean = items.get(position);
                FriendDBHelper.getInstance().queryFriendApplyBean(new Consumer<List<FriendApplyBean>>() {
                    @Override
                    public void accept(List<FriendApplyBean> FriendApplyBean) throws Exception {
                        if(FriendApplyBean.size() > 0){
                            Toast.makeText(getApplicationContext(),"当前处于申请中，不能重复申请！",Toast.LENGTH_LONG).show();
                            return;
                        }
                        FriendDBHelper.getInstance().queryFriendBean(new Consumer<List<FriendBean>>() {
                            @Override
                            public void accept(List<FriendBean> friendBeen) throws Exception {
                                if(friendBeen.size() > 0){
                                    Toast.makeText(getApplicationContext(),"当前用户已经是你的好友了！",Toast.LENGTH_LONG).show();
                                    return;
                                }
                                showFriendApply(userBean);
                            }
                        },userBean.id);
                    }
                },UserConfig.getID(),userBean.id);
            }
        });
        rvContent.addItemDecoration(
                new HorizontalDividerItemDecoration.Builder(context)
                        .color(context.getResources().getColor(R.color.dark_gray))
                        .sizeResId(R.dimen.conversation_item_divider_height)
                        .marginResId(R.dimen.conversation_item_left_margin, R.dimen.conversation_item_right_margin)
                        .build());
        rvContent.setAdapter(adapter);
        etSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                searchUser();
                return false;
            }
        });
    }

    private void searchUser() {
        String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(context, "请输入关键词！", Toast.LENGTH_LONG).show();
            return;
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id", UserConfig.getID());
            jsonObject.put("token", UserConfig.getToken());
            jsonObject.put("keyword", etSearch.getText().toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        new NetworkWorker.Builder().
                isShowDialog(true).
                content(jsonObject.toString()).
                callInMainThread(true).
                url(BuildConfig.URL_PREFIX + "search/searchUser").
                build().
                execute(context, new Consumer<NetworkResultBean>() {
                    @Override
                    public void accept(NetworkResultBean networkResultBean) throws Exception {
                        if (networkResultBean.status == 1) {
                            List<UserBean> userBeanList = GsonUtils.getInstance().fromJson(networkResultBean.data, new TypeToken<List<UserBean>>() {
                            }.getType());
                            items.clear();
                            items.addAll(userBeanList);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(context, networkResultBean.hint, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @OnClick(R.id.iv_back)
    public void onClick() {
        finish();
    }

    public static void start(Context context){
        Intent intent = new Intent(context,SearchActivity.class);
        context.startActivity(intent);
    }

    private void showFriendApply(final UserBean userBean){
        View view = LayoutInflater.from(context).inflate(R.layout.view_friend_apply,null);
        TextView tvHint = (TextView) view.findViewById(R.id.tv_apply_hint);
        final EditText edInput = (EditText) view.findViewById(R.id.et_input);
        Button btnCancel = (Button) view.findViewById(R.id.btn_cancel);
        Button btnSend = (Button) view.findViewById(R.id.btn_send);
        tvHint.setText(getString(R.string.friend_apply_hint,userBean.nickname));
        final Dialog dialog = new AlertDialog.Builder(this).setCancelable(true).setView(view).show();
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edInput.getText().toString().trim();
                if(TextUtils.isEmpty(text)){
                    Toast.makeText(getApplicationContext(),"说些什么吧",Toast.LENGTH_LONG).show();
                    return;
                }

                final CommonMessage message = new CommonMessage();
                message.sender_id = UserConfig.getID();
                message.receiver_id = userBean.id;
                message.sender_name = UserConfig.getNickname();
                ApplyMessage applyMessage = new ApplyMessage();
                applyMessage.text = text;
                message.content = GsonUtils.getInstance().toJson(applyMessage);
                message.message_id = UUID.randomUUID().toString();
                message.sender_avator = UserConfig.getPortrait();
                InstantMessengerClient.getInstance().sendTextMessage(GsonUtils.getInstance().toJson(message), new IMConnector.ResultListener() {
                    @Override
                    public void onSuccess() {
                        if(null != dialog && dialog.isShowing() && !isFinishing()) {
                            Toast.makeText(getApplicationContext(),"申请发送成功！",Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                        FriendApplyBean friendApplyBean = new FriendApplyBean();
                        friendApplyBean.applyID = message.sender_id;
                        friendApplyBean.confirmID = message.receiver_id;
                        friendApplyBean.status = FriendApplyBean.STATUS_APPLYING;
                        friendApplyBean.content = ((ApplyMessage)message.getMessage()).text;
                        friendApplyBean.createTime = System.currentTimeMillis();
                        friendApplyBean.setFriend(userBean);
                        FriendDBHelper.getInstance().mergeFriendApplyBeanByApplyID(new Consumer<FriendApplyBean>() {
                            @Override
                            public void accept(FriendApplyBean friendApplyBean) throws Exception {
                                FriendApplyListRefreshBean refreshBean = new FriendApplyListRefreshBean();
                                refreshBean.message = friendApplyBean;
                                RxBus.get().post(refreshBean);
                            }
                        }, friendApplyBean);
                    }

                    @Override
                    public void onError(Throwable ex) {
                        if(null != dialog && dialog.isShowing() && !isFinishing()) {
                            Toast.makeText(getApplicationContext(),"申请发送失败！",Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    }
                });
            }
        });
    }

}
