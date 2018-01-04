package fanjh.mine.applibrary.db.conversation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import fanjh.mine.applibrary.base.BaseApplication;
import fanjh.mine.applibrary.bean.ConversationListBean;
import fanjh.mine.applibrary.db.BaseDataBaseHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
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
public class ConversationListDBHelper extends BaseDataBaseHelper {
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_AVATOR = "avator";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_NICKNAME = "nickname";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_DOT = "dot";
    public static final String TABLE_NAME = "conversation_list";
    public static final String DB_NAME = "conversation";
    public static final int VERSION = 1;
    public static final String CREATE_TABLE = "CREATE table IF NOT EXISTS " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_AVATOR + " TEXT," + COLUMN_MESSAGE + " TEXT," + COLUMN_USER_ID + " INTEGER," +
            COLUMN_DOT + " INTEGER," + COLUMN_NICKNAME + " TEXT," + COLUMN_TIME + " REAL)";

    private static class Holder {
        static ConversationListDBHelper INSTANCE = new ConversationListDBHelper(BaseApplication.application, DB_NAME, null, VERSION);
    }

    public static ConversationListDBHelper getInstance() {
        return Holder.INSTANCE;
    }

    private ConversationListDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void updateConversationList(final ConversationListBean conversationListBean, final boolean addRedDot) {
        if (null == conversationListBean || conversationListBean.userID == 0) {
            return;
        }
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                Cursor cursor = query(TABLE_NAME, new String[]{COLUMN_ID, COLUMN_DOT}, COLUMN_USER_ID + " = ?", new String[]{conversationListBean.userID + ""}, null);
                if (null == cursor) {
                    e.onNext(false);
                } else {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(COLUMN_AVATOR, conversationListBean.avator);
                    contentValues.put(COLUMN_MESSAGE, conversationListBean.message);
                    contentValues.put(COLUMN_NICKNAME, conversationListBean.nickname);
                    contentValues.put(COLUMN_TIME, conversationListBean.time);
                    contentValues.put(COLUMN_USER_ID, conversationListBean.userID);
                    if (cursor.moveToNext()) {
                        int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
                        if (addRedDot) {
                            int redDotCount = cursor.getInt(cursor.getColumnIndex(COLUMN_DOT));
                            contentValues.put(COLUMN_DOT, ++redDotCount);
                        }
                        contentValues.put(COLUMN_ID, id);
                        update(TABLE_NAME, contentValues, COLUMN_ID + " = ?", new String[]{id + ""});
                    } else {
                        if (addRedDot) {
                            contentValues.put(COLUMN_DOT, 1);
                        }
                        insert(TABLE_NAME, contentValues);
                    }
                    e.onNext(true);
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    public ConversationListBean parseFromCursor(Cursor cursor) {
        ConversationListBean conversationListBean = new ConversationListBean();
        conversationListBean.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        conversationListBean.message = cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE));
        conversationListBean.time = cursor.getLong(cursor.getColumnIndex(COLUMN_TIME));
        conversationListBean.avator = cursor.getString(cursor.getColumnIndex(COLUMN_AVATOR));
        conversationListBean.userID = cursor.getInt(cursor.getColumnIndex(COLUMN_USER_ID));
        conversationListBean.nickname = cursor.getString(cursor.getColumnIndex(COLUMN_NICKNAME));
        conversationListBean.dotCount = cursor.getInt(cursor.getColumnIndex(COLUMN_DOT));
        return conversationListBean;
    }

    public void clearRedDot(final int userID) {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_DOT, 0);
                update(TABLE_NAME, contentValues, COLUMN_USER_ID + " = ?", new String[]{userID + ""});
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    public void queryAll(Consumer<List<ConversationListBean>> consumer) {
        Observable.create(new ObservableOnSubscribe<List<ConversationListBean>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<ConversationListBean>> e) throws Exception {
                List<ConversationListBean> result = new ArrayList<>();
                Cursor cursor = query(TABLE_NAME, null, null, null, COLUMN_TIME + " desc");
                if (null == cursor) {
                    e.onNext(result);
                } else {
                    while (cursor.moveToNext()) {
                        result.add(parseFromCursor(cursor));
                    }
                    e.onNext(result);
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).subscribe(consumer);

    }

}
