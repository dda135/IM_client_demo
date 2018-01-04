package fanjh.mine.messenger.friend;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.thread.EventThread;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseFragment;
import fanjh.mine.applibrary.bean.friend.FriendBean;
import fanjh.mine.applibrary.bean.message.BaseMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.rxbus.FriendListRefreshBean;
import fanjh.mine.applibrary.db.friend.FriendDBHelper;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.conversation.ConversationActivity;
import fanjh.mine.messenger.im.CommonReceiver;
import fanjh.mine.messenger.im.MessageArrivedListener;
import fanjh.mine.messenger.search.SearchActivity;
import io.reactivex.functions.Consumer;

/**
 * @author fanjh
 * @date 2017/11/30 13:47
 * @description
 * @note
 **/
public class FriendFragment extends BaseFragment {

    @BindView(R.id.iv_search)
    SimpleDraweeView ivSearch;
    @BindView(R.id.iv_apply)
    SimpleDraweeView ivApply;
    @BindView(R.id.rv_friend)
    RecyclerView rvFriend;
    @BindView(R.id.tv_red_dot)
    TextView tvRedDot;
    private View view;
    private List<FriendBean> friendItems = new ArrayList<>();
    private BaseQuickAdapter<FriendBean, BaseViewHolder> userAdapter;
    private MessageArrivedListener messageArrivedListener = new MessageArrivedListener() {
        @Override
        public void onMessageArrived(CommonMessage message) {
            BaseMessage baseMessage = message.getMessage();
            switch (baseMessage.type){
                case BaseMessage.TYPE_APPLY_AGREE:
                    for (FriendBean item : friendItems) {
                        if (item.getFriend().id == message.sender_id) {
                            return;
                        }
                    }
                    friendItems.add(0, FriendBean.parseFromMessage(message));
                    rvFriend.post(new Runnable() {
                        @Override
                        public void run() {
                            userAdapter.notifyDataSetChanged();
                        }
                    });
                    break;
                case BaseMessage.TYPE_APPLY:
                    rvFriend.post(new Runnable() {
                        @Override
                        public void run() {
                            tvRedDot.setVisibility(View.VISIBLE);
                            String text = tvRedDot.getText().toString();
                            if(TextUtils.isEmpty(text)){
                                tvRedDot.setText("1");
                            }else{
                                tvRedDot.setText((Integer.parseInt(text) + 1) + "");
                            }
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null == view) {
            view = inflater.inflate(R.layout.fragment_friend, container, false);
        }
        ButterKnife.bind(this, view);
        RxBus.get().register(this);
        if (null == userAdapter) {
            userAdapter = new BaseQuickAdapter<FriendBean, BaseViewHolder>(R.layout.item_user, friendItems) {
                @Override
                protected void convert(BaseViewHolder helper, FriendBean item) {
                    helper.setText(R.id.tv_name, item.getFriend().nickname);
                    if (!TextUtils.isEmpty(item.getFriend().portrait)) {
                        SimpleDraweeView image = helper.getView(R.id.iv_avator);
                        image.setImageURI(item.getFriend().portrait);
                    }
                }
            };
            userAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    FriendBean message = friendItems.get(position);
                    ConversationActivity.start(context, message.getFriend().id, message.getFriend().portrait, message.getFriend().nickname);
                }
            });
            rvFriend.setLayoutManager(new LinearLayoutManager(context));
            rvFriend.addItemDecoration(
                    new HorizontalDividerItemDecoration.Builder(context)
                            .color(context.getResources().getColor(R.color.dark_gray))
                            .sizeResId(R.dimen.conversation_item_divider_height)
                            .marginResId(R.dimen.conversation_item_left_margin, R.dimen.conversation_item_right_margin)
                            .build());
            rvFriend.setAdapter(userAdapter);
        }
        FriendDBHelper.getInstance().queryAllFriendBean(new Consumer<List<FriendBean>>() {
            @Override
            public void accept(List<FriendBean> commonMessages) throws Exception {
                friendItems.clear();
                friendItems.addAll(commonMessages);
                userAdapter.notifyDataSetChanged();
            }
        });
        CommonReceiver.getInstance().registerArrivedListener(messageArrivedListener);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RxBus.get().unregister(this);
        CommonReceiver.getInstance().unregisterArrivedListener(messageArrivedListener);
    }

    @OnClick({R.id.iv_search, R.id.iv_apply})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_search:
                SearchActivity.start(context);
                break;
            case R.id.iv_apply:
                tvRedDot.setVisibility(View.GONE);
                tvRedDot.setText("0");
                FriendApplyActivity.start(context);
                break;
            default:
                break;
        }
    }

    @Subscribe(thread = EventThread.IO)
    public void refreshBySendMessage(FriendListRefreshBean refreshBean) {
        FriendBean message = refreshBean.message;
        for (int i = 0; i < friendItems.size(); ++i) {
            FriendBean item = friendItems.get(i);
            if (item.getFriend().id == message.getFriend().id) {
                return;
            }
        }
        friendItems.add(0,message);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                userAdapter.notifyDataSetChanged();
            }
        });
    }

}
