package fanjh.mine.messenger.conversation.adapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import fanjh.mine.applibrary.bean.ConversationListBean;
import fanjh.mine.applibrary.utils.TimeUtils;
import fanjh.mine.messenger.R;

/**
* @author fanjh
* @date 2017/11/29 16:52
* @description
* @note
**/
public class ConversationListAdapter extends BaseQuickAdapter<ConversationListBean,BaseViewHolder> {

    public ConversationListAdapter(@LayoutRes int layoutResId, @Nullable List<ConversationListBean> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ConversationListBean item) {
        helper.setText(R.id.tv_nickname,item.nickname);
        helper.setText(R.id.tv_content,item.message);
        helper.setText(R.id.tv_time, TimeUtils.getSimpleTime(item.time));
        helper.setGone(R.id.tv_red_dot,item.dotCount > 0);
        helper.setText(R.id.tv_red_dot, item.dotCount + "");
        if(!TextUtils.isEmpty(item.avator)) {
            SimpleDraweeView image = helper.getView(R.id.iv_avator);
            image.setImageURI(item.avator);
        }
    }
}
