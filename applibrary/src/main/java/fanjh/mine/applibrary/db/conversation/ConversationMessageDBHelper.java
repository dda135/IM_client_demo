package fanjh.mine.applibrary.db.conversation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import fanjh.mine.applibrary.base.BaseApplication;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.db.BaseDataBaseHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
* @author fanjh
* @date 2017/11/29 17:33
* @description
* @note
**/
public class ConversationMessageDBHelper extends BaseDataBaseHelper {
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_RECEIVER_ID = "receiver_id";
    public static final String COLUMN_SEND_ID = "send_id";
    public static final String COLUMN_AVATOR = "avator";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_SENDER_NAME = "sender_name";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_SEND_STATUS = "send_status";

    public static final String TABLE_NAME = "conversation_message";
    public static final String DB_NAME = "conversation_message";
    public static final int VERSION = 1;
    public static final String CREATE_TABLE = "CREATE table IF NOT EXISTS " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_AVATOR + " TEXT," + COLUMN_MESSAGE_ID + " TEXT,"+ COLUMN_RECEIVER_ID + " INTEGER," +
            COLUMN_SEND_ID + " INTEGER," + COLUMN_CONTENT + " TEXT," + COLUMN_SEND_STATUS + " INTEGER," +
            COLUMN_SENDER_NAME +" TEXT," + COLUMN_TIME + " REAL)";

    private static class Holder{
        static ConversationMessageDBHelper INSTANCE = new ConversationMessageDBHelper(BaseApplication.application,DB_NAME,null,VERSION);
    }

    public static ConversationMessageDBHelper getInstance(){
        return Holder.INSTANCE;
    }

    private ConversationMessageDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void updateMessageStatus(CommonMessage message){
        if(null == message){
            return;
        }
        Observable.just(message).subscribeOn(Schedulers.io()).subscribe(new Consumer<CommonMessage>() {
            @Override
            public void accept(CommonMessage message) throws Exception {
                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_SEND_STATUS,message.send_status);
                update(TABLE_NAME,contentValues,COLUMN_MESSAGE_ID + " = ?",new String[]{message.message_id+""});
            }
        });
    }

    public CommonMessage parseFromCursor(Cursor cursor){
        CommonMessage commonMessage = new CommonMessage();
        commonMessage.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        commonMessage.message_id = cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE_ID));
        commonMessage.time = cursor.getLong(cursor.getColumnIndex(COLUMN_TIME));
        commonMessage.sender_avator = cursor.getString(cursor.getColumnIndex(COLUMN_AVATOR));
        commonMessage.receiver_id = cursor.getInt(cursor.getColumnIndex(COLUMN_RECEIVER_ID));
        commonMessage.sender_id = cursor.getInt(cursor.getColumnIndex(COLUMN_SEND_ID));
        commonMessage.send_status = cursor.getInt(cursor.getColumnIndex(COLUMN_SEND_STATUS));
        commonMessage.sender_name = cursor.getString(cursor.getColumnIndex(COLUMN_SENDER_NAME));
        commonMessage.content = cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT));
        commonMessage.getMessage();

        return commonMessage;
    }

    public void queryAll(final int userID, Consumer<List<CommonMessage>> consumer){
        Observable.create(new ObservableOnSubscribe<List<CommonMessage>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<CommonMessage>> e) throws Exception {
                List<CommonMessage> result = new ArrayList<>();
                Cursor cursor = query(TABLE_NAME,null,COLUMN_SEND_ID + " = ? or " + COLUMN_RECEIVER_ID + " = ?",new String[]{userID+"",userID+""},null);
                if(null == cursor){
                    e.onNext(result);
                }else {
                    while (cursor.moveToNext()) {
                        result.add(parseFromCursor(cursor));
                    }
                    e.onNext(result);
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(consumer);
    }

    public void insertMessage(final CommonMessage message){
        if(TextUtils.isEmpty(message.message_id)){
            return;
        }
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Object> e) throws Exception {
                insert(TABLE_NAME,fromCommonMessage(message));
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    public void mergeMessage(final CommonMessage message){
        if(TextUtils.isEmpty(message.message_id)){
            return;
        }
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                Cursor cursor = query(TABLE_NAME,new String[]{COLUMN_ID},COLUMN_MESSAGE_ID + " = ?",new String[]{message.message_id},null);
                if(null == cursor || cursor.moveToNext()){
                    e.onComplete();
                }else{
                    insert(TABLE_NAME,fromCommonMessage(message));
                }
            }
        }).subscribeOn(Schedulers.io()).
                subscribe();
    }

    public void mergeMessage(final CommonMessage message, Consumer<CommonMessage> consumer){
        if(TextUtils.isEmpty(message.message_id)){
            return;
        }
        Observable.create(new ObservableOnSubscribe<CommonMessage>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<CommonMessage> e) throws Exception {
                Cursor cursor = query(TABLE_NAME,new String[]{COLUMN_ID},COLUMN_MESSAGE_ID + " = ?",new String[]{message.message_id},null);
                if(null == cursor || cursor.moveToNext()){
                    e.onComplete();
                }else{
                    insert(TABLE_NAME,fromCommonMessage(message));
                    e.onNext(message);
                }
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(Schedulers.io()).
                subscribe(consumer);
    }

    private ContentValues fromCommonMessage(CommonMessage message){
        ContentValues contentValues = new ContentValues();
        if(message.id != 0){
            contentValues.put(COLUMN_ID,message.id);
        }
        contentValues.put(COLUMN_TIME,message.time);
        contentValues.put(COLUMN_SEND_STATUS,message.send_status);
        contentValues.put(COLUMN_AVATOR,message.sender_avator);
        contentValues.put(COLUMN_CONTENT,message.content);
        contentValues.put(COLUMN_MESSAGE_ID,message.message_id);
        contentValues.put(COLUMN_RECEIVER_ID,message.receiver_id);
        contentValues.put(COLUMN_SEND_ID,message.sender_id);
        contentValues.put(COLUMN_SENDER_NAME,message.sender_name);
        return contentValues;
    }

}
