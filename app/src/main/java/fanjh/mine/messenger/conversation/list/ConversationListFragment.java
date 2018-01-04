package fanjh.mine.messenger.conversation.list;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
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
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.ConversationListBean;
import fanjh.mine.applibrary.bean.rxbus.ConversationListArrivedMessageBean;
import fanjh.mine.applibrary.bean.rxbus.ConversationListRefreshBean;
import fanjh.mine.applibrary.db.conversation.ConversationListDBHelper;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.conversation.ConversationActivity;
import fanjh.mine.messenger.conversation.adapter.ConversationListAdapter;
import fanjh.mine.messenger.search.SearchActivity;
import io.reactivex.functions.Consumer;

/**
 * @author fanjh
 * @date 2017/11/29 16:41
 * @description
 * @note
 **/
public class ConversationListFragment extends BaseFragment {

    @BindView(R.id.rv_content)
    RecyclerView rvContent;
    @BindView(R.id.iv_back)
    SimpleDraweeView ivBack;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.iv_right)
    SimpleDraweeView ivRight;

    private ConversationListAdapter adapter;
    private List<ConversationListBean> items;
    private View view;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        items = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null == view) {
            view = inflater.inflate(R.layout.fragment_conversation_list, container, false);
        }
        ButterKnife.bind(this, view);
        ivRight.setVisibility(View.VISIBLE);
        ivRight.setActualImageResource(R.drawable.ic_search);
        tvTitle.setText(getString(R.string.conversation));
        ivBack.setVisibility(View.GONE);
        RxBus.get().register(this);
        if (null == adapter) {
            adapter = new ConversationListAdapter(R.layout.item_conversation, items);
            adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    ConversationListBean conversationListBean = items.get(position);
                    ConversationListDBHelper.getInstance().clearRedDot(conversationListBean.userID);
                    conversationListBean.dotCount = 0;
                    adapter.notifyDataSetChanged();
                    ConversationActivity.start(context, conversationListBean.userID, conversationListBean.avator, conversationListBean.nickname);
                }
            });
            rvContent.setLayoutManager(new LinearLayoutManager(context));
            rvContent.addItemDecoration(
                    new HorizontalDividerItemDecoration.Builder(context)
                            .color(context.getResources().getColor(R.color.dark_gray))
                            .sizeResId(R.dimen.conversation_item_divider_height)
                            .marginResId(R.dimen.conversation_item_left_margin, R.dimen.conversation_item_right_margin)
                            .build());
            rvContent.setAdapter(adapter);
        }
        queryAndShow();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != view) {
            ViewParent parent = view.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(view);
            }
        }
        RxBus.get().unregister(this);
    }

    @Subscribe(thread = EventThread.IO)
    public void afterArrivedMesssage(ConversationListArrivedMessageBean arrivedMessageBean) {
        CommonMessage message = arrivedMessageBean.message;
        final ConversationListBean newItem = ConversationListBean.parseFromCommonMessage(message, message.sender_id);
        boolean isExists = false;
        for (int i = 0; i < items.size(); ++i) {
            ConversationListBean item = items.get(i);
            if (item.userID == message.sender_id) {
                item.time = newItem.time;
                item.message = newItem.message;
                if(arrivedMessageBean.shouldSetUnread) {
                    item.dotCount++;
                }
                isExists = true;
                break;
            }
        }
        if(!isExists) {
            if(arrivedMessageBean.shouldSetUnread) {
                newItem.dotCount = 1;
            }
            if (items.size() > 0) {
                items.add(0, newItem);
            } else {
                items.add(newItem);
            }
        }
        rvContent.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Subscribe(thread = EventThread.IO)
    public void refreshBySendMessage(ConversationListRefreshBean refreshBean) {
        ConversationListBean bean = refreshBean.conversationListBean;
        boolean oldItem = false;
        for (int i = 0; i < items.size(); ++i) {
            ConversationListBean item = items.get(i);
            if (item.userID == bean.userID) {
                item.time = bean.time;
                item.message = bean.message;
                oldItem = true;
                break;
            }
        }
        if (!oldItem) {
            if(items.size() == 0){
                items.add(bean);
            }else {
                items.set(0, bean);
            }
        }
        rvContent.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void queryAndShow() {
        ConversationListDBHelper.getInstance().queryAll(new Consumer<List<ConversationListBean>>() {
            @Override
            public void accept(List<ConversationListBean> conversationListBeen) throws Exception {
                items.clear();
                items.addAll(conversationListBeen);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @OnClick({R.id.iv_back, R.id.tv_title, R.id.iv_right})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_title:
                break;
            case R.id.iv_right:
                SearchActivity.start(context);
                break;
        }
    }
}
