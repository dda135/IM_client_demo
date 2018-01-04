package fanjh.mine.messenger.im;

import fanjh.mine.applibrary.bean.message.CommonMessage;

/**
* @author fanjh
* @date 2017/11/29 18:07
* @description
* @note
**/
public interface MessageArrivedListener {
    void onMessageArrived(CommonMessage message);
}
