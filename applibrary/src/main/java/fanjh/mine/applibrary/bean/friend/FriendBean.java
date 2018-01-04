package fanjh.mine.applibrary.bean.friend;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.SystemClock;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.bean.message.ApplyAgreeMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.utils.GsonUtils;

/**
* @author fanjh
* @date 2017/12/8 15:24
* @description 好友实体
* @note
**/
public class FriendBean {
    public static final int STATUS_HIDDEN = 0;
    public static final int STATUS_FRIEND = 1;

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SERVER_ID = "server_id";
    public static final String COLUMN_APPLY_ID = "apply_id";
    public static final String COLUMN_CONFIRM_ID = "confirm_id";
    public static final String COLUMN_FRIEND_MESSAGE = "friend_message";
    public static final String COLUMN_CREATE_TIME = "create_time";
    public static final String COLUMN_STATUS = "status";

    public static final String TABLE_NAME = "friend";
    public static final String CREATE_TABLE = "CREATE table IF NOT EXISTS " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_SERVER_ID + " INTEGER," +
            COLUMN_APPLY_ID + " INTEGER," + COLUMN_CONFIRM_ID + " INTEGER,"+ COLUMN_FRIEND_MESSAGE + " TEXT," +
            COLUMN_STATUS + " INTEGER," + COLUMN_CREATE_TIME + " REAL)";

    public int _id;
    @SerializedName("id")
    public int serverID;
    public int applyID;
    public int confirmID;
    public int status;
    private UserBean friend;
    public String friendMessage;
    public long createTime;

    public UserBean getFriend() {
        if(null == friend){
            friend = GsonUtils.getInstance().fromJson(friendMessage,UserBean.class);
        }
        return friend;
    }

    public void setFriend(UserBean friend) {
        this.friend = friend;
        friendMessage = GsonUtils.getInstance().toJson(friend);
    }

    public void setFriend(String friendMessage) {
        this.friendMessage = friendMessage;
        this.friend = GsonUtils.getInstance().fromJson(friendMessage,UserBean.class);
    }

    public static FriendBean parseFromCursor(Cursor cursor){
        FriendBean friendBean = new FriendBean();
        friendBean._id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        friendBean.serverID = cursor.getInt(cursor.getColumnIndex(COLUMN_SERVER_ID));
        friendBean.applyID = cursor.getInt(cursor.getColumnIndex(COLUMN_APPLY_ID));
        friendBean.confirmID = cursor.getInt(cursor.getColumnIndex(COLUMN_CONFIRM_ID));
        friendBean.friendMessage = cursor.getString(cursor.getColumnIndex(COLUMN_FRIEND_MESSAGE));
        friendBean.friend = GsonUtils.getInstance().fromJson(friendBean.friendMessage,UserBean.class);
        friendBean.createTime = cursor.getLong(cursor.getColumnIndex(COLUMN_CREATE_TIME));
        friendBean.status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));
        return friendBean;
    }

    public static FriendBean parseFromMessage(CommonMessage message){
        ApplyAgreeMessage applyAgreeMessage = (ApplyAgreeMessage) message.getMessage();
        FriendBean friendBean = new FriendBean();
        if(applyAgreeMessage.serverID > 0) {
            friendBean.serverID = applyAgreeMessage.serverID;
        }
        friendBean.createTime = System.currentTimeMillis();
        friendBean.status = STATUS_FRIEND;
        friendBean.applyID = message.receiver_id;
        friendBean.confirmID = message.sender_id;

        friendBean.friend = new UserBean();
        friendBean.friend.id = message.sender_id;
        friendBean.friend.portrait = message.sender_avator;
        friendBean.friend.nickname = message.sender_name;
        friendBean.friendMessage = GsonUtils.getInstance().toJson(friendBean.friend);
        return friendBean;
    }

    public static FriendBean parseFromFriendApplyBean(FriendApplyBean applyBean){
        FriendBean friendBean = new FriendBean();

        friendBean.createTime = System.currentTimeMillis();
        friendBean.status = STATUS_FRIEND;
        friendBean.applyID = applyBean.applyID;
        friendBean.confirmID = applyBean.confirmID;

        friendBean.friend = new UserBean();
        friendBean.friend.id = applyBean.getFriend().id;
        friendBean.friend.portrait = applyBean.getFriend().portrait;
        friendBean.friend.nickname = applyBean.getFriend().nickname;
        friendBean.friendMessage = GsonUtils.getInstance().toJson(friendBean.friend);
        return friendBean;
    }

    public ContentValues toContentValues(){
        ContentValues contentValues = new ContentValues();
        if(_id > 0) {
            contentValues.put(COLUMN_ID, _id);
        }
        if(serverID > 0) {
            contentValues.put(COLUMN_SERVER_ID, serverID);
        }
        if(null == friendMessage && null != friend){
            friendMessage = GsonUtils.getInstance().toJson(friend);
        }
        if(null != friendMessage){
            contentValues.put(COLUMN_FRIEND_MESSAGE,friendMessage);
        }
        if(createTime > 0) {
            contentValues.put(COLUMN_CREATE_TIME, createTime);
        }
        if(applyID > 0) {
            contentValues.put(COLUMN_APPLY_ID, applyID);
        }
        if(confirmID > 0) {
            contentValues.put(COLUMN_CONFIRM_ID, confirmID);
        }
        if(status > 0){
            contentValues.put(COLUMN_STATUS, status);
        }
        return contentValues;
    }

}
