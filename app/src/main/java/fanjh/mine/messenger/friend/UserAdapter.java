package fanjh.mine.messenger.friend;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.messenger.R;

/**
* @author fanjh
* @date 2017/11/30 14:32
* @description
* @note
**/
public class UserAdapter extends BaseQuickAdapter<UserBean,BaseViewHolder> {
    public UserAdapter(@LayoutRes int layoutResId, @Nullable List<UserBean> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, UserBean item) {
        helper.setText(R.id.tv_name,item.nickname);
        if(!TextUtils.isEmpty(item.portrait)) {
            SimpleDraweeView image = helper.getView(R.id.iv_avator);
            image.setImageURI(item.portrait);
        }
    }
}
