package fanjh.mine.applibrary.bean.friend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.bean.message.ApplyAgreeMessage;
import fanjh.mine.applibrary.bean.message.ApplyAgreeSuccessMessage;
import fanjh.mine.applibrary.bean.message.ApplyMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.utils.GsonUtils;

/**
* @author fanjh
* @date 2017/12/8 15:22
* @description 好友申请实体
* @note
**/
public class FriendApplyBean {
    public static final int STATUS_HIDDEN = 0;
    public static final int STATUS_CONFIRM = 1;
    public static final int STATUS_REJECT = 2;
    public static final int STATUS_APPLYING = 3;

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SERVER_ID = "server_id";
    public static final String COLUMN_APPLY_ID = "apply_id";
    public static final String COLUMN_CONFIRM_ID = "confirm_id";
    public static final String COLUMN_APPLY_CONTENT = "content";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_FRIEND_MESSAGE = "friend_message";
    public static final String COLUMN_CREATE_TIME = "create_time";

    public static final String TABLE_NAME = "friend_apply";
    public static final String CREATE_TABLE = "CREATE table IF NOT EXISTS " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_SERVER_ID + " INTEGER," + COLUMN_CONFIRM_ID + " INTEGER,"+ COLUMN_APPLY_ID + " INTEGER," +
            COLUMN_STATUS + " INTEGER,"+ COLUMN_APPLY_CONTENT +" TEXT," + COLUMN_FRIEND_MESSAGE +" TEXT," +
            COLUMN_CREATE_TIME + " REAL)";

    public int _id;
    @SerializedName("id")
    public int serverID;
    public int applyID;
    public int confirmID;
    public String content;
    public long createTime;
    public int status;
    private String friendMessage;
    private UserBean friend;

    public UserBean getFriend(){
        if(null == friend){
            friend = GsonUtils.getInstance().fromJson(friendMessage,UserBean.class);
        }
        return friend;
    }

    public void setFriend(String friendMessage) {
        this.friendMessage = friendMessage;
        this.friend = GsonUtils.getInstance().fromJson(friendMessage,UserBean.class);
    }

    public void setFriend(UserBean userBean){
        this.friendMessage = GsonUtils.getInstance().toJson(userBean);
        this.friend = userBean;
    }

    public static FriendApplyBean parseFromCursor(Cursor cursor){
        FriendApplyBean friendApplyBean = new FriendApplyBean();
        friendApplyBean._id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        friendApplyBean.serverID = cursor.getInt(cursor.getColumnIndex(COLUMN_SERVER_ID));
        friendApplyBean.applyID = cursor.getInt(cursor.getColumnIndex(COLUMN_APPLY_ID));
        friendApplyBean.confirmID = cursor.getInt(cursor.getColumnIndex(COLUMN_CONFIRM_ID));
        friendApplyBean.content = cursor.getString(cursor.getColumnIndex(COLUMN_APPLY_CONTENT));
        friendApplyBean.friendMessage = cursor.getString(cursor.getColumnIndex(COLUMN_FRIEND_MESSAGE));
        friendApplyBean.createTime = cursor.getLong(cursor.getColumnIndex(COLUMN_CREATE_TIME));
        friendApplyBean.status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));
        return friendApplyBean;
    }

    public ContentValues toContentValues(){
        ContentValues contentValues = new ContentValues();
        if(_id > 0) {
            contentValues.put(COLUMN_ID, _id);
        }
        if(serverID > 0) {
            contentValues.put(COLUMN_SERVER_ID, serverID);
        }
        if(applyID > 0) {
            contentValues.put(COLUMN_APPLY_ID, applyID);
        }
        if(content != null) {
            contentValues.put(COLUMN_APPLY_CONTENT, content);
        }
        if(confirmID > 0) {
            contentValues.put(COLUMN_CONFIRM_ID, confirmID);
        }
        if(friendMessage == null && friend != null){
            friendMessage = GsonUtils.getInstance().toJson(friend);
        }
        if(friendMessage != null) {
            contentValues.put(COLUMN_FRIEND_MESSAGE, friendMessage);
        }
        if(createTime > 0) {
            contentValues.put(COLUMN_CREATE_TIME, createTime);
        }
        if(status > 0){
            contentValues.put(COLUMN_STATUS, status);
        }
        return contentValues;
    }

    public static FriendApplyBean parseFromApplyMessage(CommonMessage message,int status){
        ApplyMessage applyMessage = (ApplyMessage) message.getMessage();
        FriendApplyBean newItem = new FriendApplyBean();
        newItem.applyID = message.sender_id;
        newItem.status = status;
        newItem.createTime = System.currentTimeMillis();
        newItem.confirmID = UserConfig.getID();
        newItem.content = applyMessage.text;
        UserBean userBean = new UserBean();
        userBean.id = message.sender_id;
        userBean.nickname = message.sender_name;
        userBean.portrait = message.sender_avator;
        newItem.setFriend(userBean);
        if(applyMessage.serverID > 0) {
            newItem.serverID = applyMessage.serverID;
        }
        return newItem;
    }

    public static FriendApplyBean parseFromApplyAgreeSuccessMessage(CommonMessage message){
        ApplyAgreeSuccessMessage applyMessage = (ApplyAgreeSuccessMessage) message.getMessage();
        FriendApplyBean newItem = new FriendApplyBean();
        newItem.applyID = applyMessage.applyID;
        newItem.confirmID = applyMessage.confirmID;
        newItem.serverID = applyMessage.serverID;

        return newItem;
    }

    public static FriendApplyBean parseFromApplyAgreeMessage(CommonMessage message){
        FriendApplyBean newItem = new FriendApplyBean();
        newItem.applyID = message.receiver_id;
        newItem.status = FriendApplyBean.STATUS_CONFIRM;
        newItem.confirmID = message.sender_id;

        return newItem;
    }

    public static FriendApplyBean parseFromApplyRejectMessage(CommonMessage message){
        FriendApplyBean newItem = new FriendApplyBean();
        newItem.applyID = message.receiver_id;
        newItem.status = FriendApplyBean.STATUS_REJECT;
        newItem.confirmID = message.sender_id;

        return newItem;
    }

}
