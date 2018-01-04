package fanjh.mine.messenger.friend;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.reflect.TypeToken;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.thread.EventThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseActivity;
import fanjh.mine.applibrary.bean.Codes;
import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.bean.friend.FriendBean;
import fanjh.mine.applibrary.bean.message.ApplyAgreeMessage;
import fanjh.mine.applibrary.bean.message.ApplyAgreeSuccessMessage;
import fanjh.mine.applibrary.bean.message.BaseMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.friend.FriendApplyBean;
import fanjh.mine.applibrary.bean.rxbus.FriendApplyListRefreshBean;
import fanjh.mine.applibrary.bean.rxbus.FriendListRefreshBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.db.friend.FriendDBHelper;
import fanjh.mine.applibrary.network.NetworkWorker;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.client.IMConnector;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.messenger.BuildConfig;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.im.CommonReceiver;
import fanjh.mine.messenger.im.MessageArrivedListener;
import fanjh.mine.messenger.im.SendMessageResultListener;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author fanjh
 * @date 2017/11/30 15:16
 * @description
 * @note
 **/
public class FriendApplyActivity extends BaseActivity {

    @BindView(R.id.iv_back)
    SimpleDraweeView ivBack;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.rv_content)
    RecyclerView rvContent;

    private List<FriendApplyBean> items = new ArrayList<>();
    private FriendApplyAdapter adapter;

    private MessageArrivedListener messageArrivedListener = new MessageArrivedListener() {
        @Override
        public void onMessageArrived(final CommonMessage message) {
            Observable.create(new ObservableOnSubscribe<CommonMessage>() {
                @Override
                public void subscribe(@NonNull ObservableEmitter<CommonMessage> e) throws Exception {
                    int type = message.getMessage().type;
                    switch (type){
                        case BaseMessage.TYPE_APPLY_AGREE_SUCCESS:
                            ApplyAgreeSuccessMessage applyAgreeSuccessMessage = (ApplyAgreeSuccessMessage) message.getMessage();
                            int index = indexReceiverItem(applyAgreeSuccessMessage);
                            if(index == -1){
                                return;
                            }
                            items.get(index).serverID = applyAgreeSuccessMessage.serverID;
                            break;
                        default:
                            break;
                    }
                    e.onComplete();
                }
            }).subscribeOn(Schedulers.single()).
                    observeOn(AndroidSchedulers.mainThread()).
                    subscribe(new Consumer<CommonMessage>() {
                @Override
                public void accept(CommonMessage message) throws Exception {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_apply);
        ButterKnife.bind(this);
        tvTitle.setText(getString(R.string.friend_apply_list));
        rvContent.setLayoutManager(new LinearLayoutManager(context));
        adapter = new FriendApplyAdapter(items);
        adapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                FriendApplyBean item = (FriendApplyBean) adapter.getItem(position);
                switch (view.getId()){
                    case R.id.btn_agree:
                        sendAgreeMessage(item);
                        break;
                    case R.id.btn_reject:
                        sendRejectMessage(item);
                        break;
                    default:
                        break;
                }
            }
        });
        rvContent.setAdapter(adapter);
        FriendDBHelper.getInstance().queryAllFriendApplyBean(new Consumer<List<FriendApplyBean>>() {
            @Override
            public void accept(List<FriendApplyBean> friendApplyBeen) throws Exception {
                items.addAll(friendApplyBeen);
                adapter.notifyDataSetChanged();
            }
        });
        CommonReceiver.getInstance().registerArrivedListener(messageArrivedListener);
        RxBus.get().register(this);
    }

    private void sendRejectMessage(final FriendApplyBean item){
        CommonMessage commonMessage = new CommonMessage();
        commonMessage.message_id = UUID.randomUUID().toString();
        commonMessage.sender_id = item.confirmID;
        commonMessage.receiver_id = item.applyID;
        commonMessage.sender_name = UserConfig.getNickname();
        commonMessage.sender_avator = UserConfig.getPortrait();
        BaseMessage baseMessage = new BaseMessage(BaseMessage.TYPE_APPLY_REJECT);
        commonMessage.content = GsonUtils.getInstance().toJson(baseMessage);
        InstantMessengerClient.getInstance().sendTextMessage(GsonUtils.getInstance().toJson(commonMessage), new IMConnector.ResultListener() {
            @Override
            public void onSuccess() {
                item.status = FriendApplyBean.STATUS_REJECT;
                if(null != adapter && !isFinishing()) {
                    Toast.makeText(getApplicationContext(), "发送成功！", Toast.LENGTH_LONG).show();
                    adapter.notifyDataSetChanged();
                }
                FriendDBHelper.getInstance().updateFriendApplyBean(item);
            }

            @Override
            public void onError(Throwable ex) {
                if(null != context && !isFinishing()) {
                    Toast.makeText(getApplicationContext(), "网络异常，请稍后再试！", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void sendAgreeMessage(final FriendApplyBean item){
        final CommonMessage commonMessage = new CommonMessage();
        commonMessage.message_id = UUID.randomUUID().toString();
        commonMessage.sender_id = item.confirmID;
        commonMessage.receiver_id = item.applyID;
        commonMessage.sender_name = UserConfig.getNickname();
        commonMessage.sender_avator = UserConfig.getPortrait();
        ApplyAgreeMessage applyAgreeMessage = new ApplyAgreeMessage();
        commonMessage.content = GsonUtils.getInstance().toJson(applyAgreeMessage);
        InstantMessengerClient.getInstance().sendTextMessage(GsonUtils.getInstance().toJson(commonMessage), new IMConnector.ResultListener() {
            @Override
            public void onSuccess() {
                item.status = FriendApplyBean.STATUS_CONFIRM;
                if(null != adapter && !isFinishing()) {
                    Toast.makeText(getApplicationContext(), "发送成功！", Toast.LENGTH_LONG).show();
                    adapter.notifyDataSetChanged();
                }
                FriendDBHelper.getInstance().updateFriendApplyBean(item);
                FriendDBHelper.getInstance().mergeFriendBeanByFriendID(FriendBean.parseFromFriendApplyBean(item));
                FriendListRefreshBean refreshBean = new FriendListRefreshBean();
                refreshBean.message = FriendBean.parseFromFriendApplyBean(item);
                RxBus.get().post(refreshBean);
            }

            @Override
            public void onError(Throwable ex) {
                if(null != context && !isFinishing()) {
                    Toast.makeText(getApplicationContext(), "网络异常，请稍后再试！", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommonReceiver.getInstance().unregisterArrivedListener(messageArrivedListener);
        RxBus.get().unregister(this);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, FriendApplyActivity.class);
        context.startActivity(intent);
    }

    @OnClick({R.id.iv_back, R.id.tv_title})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.tv_title:
                break;
            default:
                break;
        }
    }

    private int indexReceiverItem(ApplyAgreeSuccessMessage message){
        int result = -1;
        if(null == items || items.size() == 0){
            return result;
        }
        for(int i = 0;i < items.size();++i){
            FriendApplyBean bean = items.get(i);
            if(bean.applyID == message.applyID && bean.confirmID == message.confirmID){
                result = i;
                break;
            }
        }
        return result;
    }

    @Subscribe(thread = EventThread.IO)
    public void mergerItem(FriendApplyListRefreshBean refreshBean){
        FriendApplyBean friendApplyBean = refreshBean.message;
        boolean isSuccess = false;
        for(int i = 0;i < items.size();++i){
            FriendApplyBean item = items.get(i);
            boolean first = (item.applyID == friendApplyBean.applyID && item.confirmID == friendApplyBean.confirmID);
            boolean second = (item.applyID == friendApplyBean.confirmID && item.confirmID == friendApplyBean.applyID);
            if(first || second){
                item.status = friendApplyBean.status;
                isSuccess = true;
                break;
            }
        }
        if(!isSuccess) {
            items.add(0, friendApplyBean);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

}
