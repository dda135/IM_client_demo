package fanjh.mine.messenger.im;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.hwangjr.rxbus.RxBus;

import java.util.ArrayList;
import java.util.List;

import fanjh.mine.applibrary.bean.friend.FriendApplyBean;
import fanjh.mine.applibrary.bean.message.ApplyAgreeSuccessMessage;
import fanjh.mine.applibrary.bean.message.BaseMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.ConversationListBean;
import fanjh.mine.applibrary.bean.message.TextMessage;
import fanjh.mine.applibrary.bean.rxbus.ConversationListArrivedMessageBean;
import fanjh.mine.applibrary.bean.rxbus.FriendApplyListRefreshBean;
import fanjh.mine.applibrary.db.conversation.ConversationListDBHelper;
import fanjh.mine.applibrary.db.conversation.ConversationMessageDBHelper;
import fanjh.mine.applibrary.db.friend.FriendDBHelper;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.applibrary.utils.NotifyUtils;
import fanjh.mine.im_sdk.core.NotifyAction;
import fanjh.mine.messenger.MainApplication;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.conversation.ConversationActivity;
import io.reactivex.functions.Consumer;

/**
* @author fanjh
* @date 2017/11/29 18:09
* @description
* @note
**/
public class CommonReceiver {
    public static HandlerThread DELIVERY_THREAD;
    public static Handler MESSAGE_DELIVERY_HANDLER;
    private static Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Intent intent = (Intent) msg.obj;
            int action = intent.getIntExtra(NotifyAction.EXTRA_MSG,-1);
            if(action == -1){
                return true;
            }
            String content = intent.getStringExtra(NotifyAction.EXTRA_CONTENT);
            CommonMessage message = GsonUtils.getInstance().fromJson(content, CommonMessage.class);
            switch (action){
                case NotifyAction.MSG_MESSAGE_ARRIVED:
                    for(MessageArrivedListener listener:messageArrivedListeners){
                        listener.onMessageArrived(message);
                    }
                    break;
                case NotifyAction.MSG_SEND_FAILURE:
                    for(SendMessageResultListener listener:sendMessageResultListeners){
                        listener.onError(message);
                    }
                    break;
                case NotifyAction.MSG_SEND_SUCCESS:
                    for(SendMessageResultListener listener:sendMessageResultListeners){
                        listener.onSuccess(message);
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    };
    private static List<MessageArrivedListener> messageArrivedListeners = new ArrayList<>();
    private static List<SendMessageResultListener> sendMessageResultListeners = new ArrayList<>();

    private static class Holder{
        static CommonReceiver INSTANCE = new CommonReceiver();
    }

    private CommonReceiver() {
        messageArrivedListeners.add(new MessageArrivedListener() {
            @Override
            public void onMessageArrived(CommonMessage message) {
                BaseMessage baseMessage = message.getMessage();
                FriendApplyListRefreshBean refreshBean = null;
                switch (baseMessage.type){
                    case BaseMessage.TYPE_APPLY:
                        FriendDBHelper.getInstance().mergeFriendApplyBeanByApplyID(new Consumer<FriendApplyBean>() {
                            @Override
                            public void accept(FriendApplyBean friendApplyBean) throws Exception {
                                FriendApplyListRefreshBean refreshBean = new FriendApplyListRefreshBean();
                                refreshBean.message = friendApplyBean;
                                RxBus.get().post(refreshBean);
                            }
                        }, FriendApplyBean.parseFromApplyMessage(message, FriendApplyBean.STATUS_APPLYING));
                        NotifyUtils.showNotification(MainApplication.application, R.drawable.logo,message.sender_name,"好友申请！",1);
                        break;
                    case BaseMessage.TYPE_APPLY_AGREE_SUCCESS:
                        //用于填充服务端id
                        ApplyAgreeSuccessMessage applyAgreeSuccessMessage = (ApplyAgreeSuccessMessage) baseMessage;
                        if(applyAgreeSuccessMessage.serverID != 0) {
                            FriendDBHelper.getInstance().updateFriendApplyBean(FriendApplyBean.parseFromApplyAgreeSuccessMessage(message));
                        }
                        break;
                    case BaseMessage.TYPE_TEXT:
                    case BaseMessage.TYPE_IMAGE:
                        ConversationMessageDBHelper.getInstance().mergeMessage(message, new Consumer<CommonMessage>() {
                            @Override
                            public void accept(CommonMessage message) throws Exception {
                                boolean shouldAddRedDot = true;
                                for(Activity activity: MainApplication.activities){
                                    if(activity instanceof ConversationActivity){
                                        ConversationActivity conversationActivity = (ConversationActivity) activity;
                                        if(conversationActivity.receiverID == message.sender_id){
                                            shouldAddRedDot = false;
                                            break;
                                        }
                                    }
                                }
                                if(shouldAddRedDot){
                                    TextMessage textMessage = (TextMessage) message.getMessage();
                                    NotifyUtils.showNotification(MainApplication.application, R.drawable.logo,message.sender_name,textMessage.text,1);
                                }
                                ConversationListDBHelper.getInstance().updateConversationList(ConversationListBean.parseFromCommonMessage(message,message.sender_id),shouldAddRedDot);
                                ConversationListArrivedMessageBean arrivedMessageBean = new ConversationListArrivedMessageBean();
                                arrivedMessageBean.shouldSetUnread = shouldAddRedDot;
                                arrivedMessageBean.message = message;
                                RxBus.get().post(arrivedMessageBean);
                            }
                        });
                        break;
                    case BaseMessage.TYPE_APPLY_AGREE:
                        FriendDBHelper.getInstance().updateFriendApplyBean(FriendApplyBean.parseFromApplyAgreeMessage(message));
                        NotifyUtils.showNotification(MainApplication.application, R.drawable.logo,message.sender_name,"好友申请通过！",1);
                        refreshBean = new FriendApplyListRefreshBean();
                        refreshBean.message = FriendApplyBean.parseFromApplyAgreeMessage(message);
                        RxBus.get().post(refreshBean);
                        break;
                    case BaseMessage.TYPE_APPLY_REJECT:
                        FriendDBHelper.getInstance().updateFriendApplyBean(FriendApplyBean.parseFromApplyRejectMessage(message));
                        NotifyUtils.showNotification(MainApplication.application, R.drawable.logo,message.sender_name,"好友申请未通过！",1);
                        refreshBean = new FriendApplyListRefreshBean();
                        refreshBean.message = FriendApplyBean.parseFromApplyRejectMessage(message);
                        RxBus.get().post(refreshBean);
                        break;
                    default:
                        break;
                }

            }
        });
    }

    public static CommonReceiver getInstance(){
        return Holder.INSTANCE;
    }

    public void registerArrivedListener(MessageArrivedListener listener){
        if(!messageArrivedListeners.contains(listener)){
            messageArrivedListeners.add(listener);
        }
    }

    public void unregisterArrivedListener(MessageArrivedListener listener){
        messageArrivedListeners.remove(listener);
    }

    public void registerSendMessageResultListener(SendMessageResultListener listener){
        if(!sendMessageResultListeners.contains(listener)){
            sendMessageResultListeners.add(listener);
        }
    }

    public void unregisterSendMessageResultListener(SendMessageResultListener listener){
        sendMessageResultListeners.remove(listener);
    }

    public static void init() {
        DELIVERY_THREAD = new HandlerThread("delivery_thread");
        DELIVERY_THREAD.start();
        MESSAGE_DELIVERY_HANDLER = new Handler(DELIVERY_THREAD.getLooper(),callback);
    }

}
