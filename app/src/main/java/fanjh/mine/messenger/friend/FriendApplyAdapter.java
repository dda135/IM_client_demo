package fanjh.mine.messenger.friend;

import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.util.MultiTypeDelegate;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import fanjh.mine.applibrary.bean.friend.FriendApplyBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.messenger.R;

/**
* @author fanjh
* @date 2017/12/11 15:33
* @description
* @note
**/
public class FriendApplyAdapter extends BaseQuickAdapter<FriendApplyBean,BaseViewHolder>{
    public static final int TYPE_MINE_APPLY = 1;
    public static final int TYPE_OTHER_APPLING = 2;
    public static final int TYPE_OTHER_APPLIED = 3;

    public FriendApplyAdapter(@Nullable List<FriendApplyBean> data) {
        super(data);
        setMultiTypeDelegate(new MultiTypeDelegate<FriendApplyBean>() {
            @Override
            protected int getItemType(FriendApplyBean friendApplyBean) {
                return getItemViewTypes(friendApplyBean);
            }
        });
        getMultiTypeDelegate().registerItemType(TYPE_MINE_APPLY, R.layout.item_friend_apply_mine);
        getMultiTypeDelegate().registerItemType(TYPE_OTHER_APPLING,R.layout.item_friend_appling);
        getMultiTypeDelegate().registerItemType(TYPE_OTHER_APPLIED,R.layout.item_friend_applied);
    }

    private int getItemViewTypes(FriendApplyBean friendApplyBean){
        int applyID = friendApplyBean.applyID;
        int userID = UserConfig.getID();
        boolean isMineApply = (applyID == userID);
        if(isMineApply){
            return TYPE_MINE_APPLY;
        }
        switch (friendApplyBean.status){
            case FriendApplyBean.STATUS_APPLYING:
                return TYPE_OTHER_APPLING;
            case FriendApplyBean.STATUS_CONFIRM:
            case FriendApplyBean.STATUS_REJECT:
                return TYPE_OTHER_APPLIED;
            default:
                return 0;
        }
    }

    @Override
    protected void convert(BaseViewHolder helper, FriendApplyBean item) {
        switch (helper.getItemViewType()){
            case TYPE_MINE_APPLY:
                helper.setText(R.id.tv_title,mContext.getString(R.string.friend_apply_hint,item.getFriend().nickname));
                String content;
                switch (item.status){
                    case FriendApplyBean.STATUS_APPLYING:
                        content = "申请中";
                        break;
                    case FriendApplyBean.STATUS_CONFIRM:
                        content = "已同意";
                        break;
                    case FriendApplyBean.STATUS_REJECT:
                        content = "已被拒绝";
                        break;
                    default:
                        content = "待确认";
                        break;
                }
                helper.setText(R.id.tv_content,content);
                break;
            case TYPE_OTHER_APPLIED:
                boolean isConfirm = FriendApplyBean.STATUS_CONFIRM == item.status;
                helper.setText(R.id.tv_content,isConfirm?mContext.getString(R.string.friend_apply_agree,item.getFriend().nickname):
                        mContext.getString(R.string.friend_apply_reject,item.getFriend().nickname));
                break;
            case TYPE_OTHER_APPLING:
                helper.setText(R.id.tv_name,item.getFriend().nickname);
                helper.setText(R.id.tv_message,item.content);
                helper.addOnClickListener(R.id.btn_agree);
                helper.addOnClickListener(R.id.btn_reject);
                break;
            default:
                break;
        }
        if(null != item.getFriend().portrait) {
            SimpleDraweeView imageView = helper.getView(R.id.iv_avator);
            imageView.setImageURI(item.getFriend().portrait);
        }
    }
}
