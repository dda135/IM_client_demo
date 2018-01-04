package fanjh.mine.messenger.conversation.adapter;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.util.MultiTypeDelegate;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import fanjh.mine.applibrary.bean.message.BaseMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.message.ImageMessage;
import fanjh.mine.applibrary.bean.message.TextMessage;
import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.utils.TimeUtils;
import fanjh.mine.messenger.R;

/**
* @author fanjh
* @date 2017/11/30 9:55
* @description
* @note
**/
public class ConversationAdapter extends BaseQuickAdapter<CommonMessage,BaseViewHolder>{
    public static final int TYPE_SEND_TEXT = 1;
    public static final int TYPE_RECEIVER_TEXT = 2;
    public static final int TYPE_SEND_IMAGE = 3;
    public static final int TYPE_RECEIVER_IMAGE = 4;
    private long lastTime;

    public ConversationAdapter(@Nullable List<CommonMessage> data) {
        super(data);
        setMultiTypeDelegate(new MultiTypeDelegate<CommonMessage>() {
            @Override
            protected int getItemType(CommonMessage message) {
                int userID = UserConfig.getID();
                BaseMessage baseMessage = message.getMessage();
                boolean isSendMessage = (userID == message.sender_id);
                switch (baseMessage.type){
                    case BaseMessage.TYPE_TEXT:
                        return isSendMessage?TYPE_SEND_TEXT:TYPE_RECEIVER_TEXT;
                    case BaseMessage.TYPE_IMAGE:
                        return isSendMessage?TYPE_SEND_IMAGE:TYPE_RECEIVER_IMAGE;
                    default:
                        break;
                }
                return 0;
            }
        });
        getMultiTypeDelegate().registerItemType(TYPE_RECEIVER_TEXT, R.layout.item_conversation_left_text);
        getMultiTypeDelegate().registerItemType(TYPE_SEND_TEXT, R.layout.item_conversation_right_text);
        getMultiTypeDelegate().registerItemType(TYPE_RECEIVER_IMAGE, R.layout.item_conversation_left_image);
        getMultiTypeDelegate().registerItemType(TYPE_SEND_IMAGE, R.layout.item_conversation_right_image);
    }

    @Override
    protected void convert(BaseViewHolder helper, CommonMessage item) {
        TextView time = helper.getView(R.id.tv_time);
        if(lastTime != 0) {
            if (item.time - lastTime < 1000 * 60 * 3) {
                time.setVisibility(View.GONE);
            } else {
                time.setVisibility(View.VISIBLE);
                time.setText(TimeUtils.getSimpleTime(item.time));
            }
        }else{
            time.setVisibility(View.GONE);
        }
        lastTime = item.time;
        BaseMessage message = item.getMessage();
        int type = helper.getItemViewType();
        switch (type){
            case TYPE_RECEIVER_TEXT:
            case TYPE_SEND_TEXT:
                TextMessage textMessage = (TextMessage) message;
                helper.setText(R.id.tv_text,textMessage.text);
                break;
            case TYPE_SEND_IMAGE:
            case TYPE_RECEIVER_IMAGE:
                ImageMessage imageMessage = (ImageMessage) item.getMessage();
                if(!TextUtils.isEmpty(imageMessage.imageUrl)) {
                    SimpleDraweeView avator = helper.getView(R.id.iv_image);
                    if(imageMessage.width != 0 && imageMessage.height != 0) {
                        ViewGroup.LayoutParams params = avator.getLayoutParams();
                        if (params.height != imageMessage.height || params.width != imageMessage.width) {
                            params.height = imageMessage.height;
                            params.width = imageMessage.width;
                        }
                    }
                    avator.setImageURI(imageMessage.imageUrl);
                }
                break;
            default:
                break;
        }

        switch (type){
            case TYPE_SEND_IMAGE:
            case TYPE_SEND_TEXT:
                handleSendStatus(helper,item);
                break;
            default:
                break;
        }

    }

    private void handleSendStatus(BaseViewHolder helper,CommonMessage item){
        if(!TextUtils.isEmpty(item.sender_avator)) {
            SimpleDraweeView avator = helper.getView(R.id.iv_avator);
            avator.setImageURI(item.sender_avator);
        }
        SimpleDraweeView failureView = helper.getView(R.id.iv_failure);
        ProgressBar uploading = helper.getView(R.id.pb_uploading);
        switch (item.send_status) {
            case CommonMessage.SEND_FAILURE:
                failureView.setVisibility(View.VISIBLE);
                uploading.setVisibility(View.GONE);
                break;
            case CommonMessage.SEND_SUCCESS:
                failureView.setVisibility(View.GONE);
                uploading.setVisibility(View.GONE);
                break;
            case CommonMessage.SEND_UPLOADING:
                failureView.setVisibility(View.GONE);
                uploading.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

}
