package fanjh.mine.applibrary.bean;

import fanjh.mine.applibrary.bean.message.BaseMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.message.TextMessage;

/**
* @author fanjh
* @date 2017/11/29 17:17
* @description
* @note
**/
public class ConversationListBean {
    public int id;
    public String avator;
    public String nickname;
    public String message;
    public long time;
    public int userID;
    public int dotCount;

    public static ConversationListBean parseFromCommonMessage(CommonMessage message, int userID){
        ConversationListBean conversationListBean = new ConversationListBean();
        conversationListBean.userID = userID;
        conversationListBean.avator = message.sender_avator;
        conversationListBean.nickname = message.sender_name;
        BaseMessage baseMessage = message.getMessage();
        if(baseMessage instanceof TextMessage){
            conversationListBean.message = ((TextMessage)baseMessage).text;
        }
        conversationListBean.time = message.time;
        return conversationListBean;
    }

}
