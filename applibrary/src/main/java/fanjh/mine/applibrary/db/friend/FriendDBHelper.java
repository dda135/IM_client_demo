package fanjh.mine.applibrary.db.friend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import fanjh.mine.applibrary.base.BaseApplication;
import fanjh.mine.applibrary.bean.friend.FriendApplyBean;
import fanjh.mine.applibrary.bean.friend.FriendBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.db.BaseDataBaseHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
* @author fanjh
* @date 2017/11/30 15:58
* @description 好友相关数据库
* @note
**/
public class FriendDBHelper extends BaseDataBaseHelper{
    public static final String DB_NAME = "friend";
    public static final int VERSION = 1;

    private static class Holder{
        static FriendDBHelper INSTANCE = new FriendDBHelper(BaseApplication.application,DB_NAME,null,VERSION);
    }

    public static FriendDBHelper getInstance(){
        return FriendDBHelper.Holder.INSTANCE;
    }

    private FriendDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FriendBean.CREATE_TABLE);
        db.execSQL(FriendApplyBean.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void queryAllFriendBean(Consumer<List<FriendBean>> consumer){
        Observable.create(new ObservableOnSubscribe<List<FriendBean>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<FriendBean>> e) throws Exception {
                List<FriendBean> result = new ArrayList<>();
                Cursor cursor = query(FriendBean.TABLE_NAME,null,null,null,FriendBean.COLUMN_CREATE_TIME + " desc");
                if(null == cursor){
                    e.onNext(result);
                }else {
                    while (cursor.moveToNext()) {
                        result.add(FriendBean.parseFromCursor(cursor));
                    }
                    e.onNext(result);
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(consumer);
    }

    public void queryAllFriendApplyBean(Consumer<List<FriendApplyBean>> consumer){
        Observable.create(new ObservableOnSubscribe<List<FriendApplyBean>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<FriendApplyBean>> e) throws Exception {
                List<FriendApplyBean> result = new ArrayList<>();
                Cursor cursor = query(FriendApplyBean.TABLE_NAME,null,null,null,FriendApplyBean.COLUMN_CREATE_TIME + " desc");
                if(null == cursor){
                    e.onNext(result);
                }else {
                    while (cursor.moveToNext()) {
                        result.add(FriendApplyBean.parseFromCursor(cursor));
                    }
                    e.onNext(result);
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(consumer);
    }

    public void mergeFriendApplyBeanByServerID(final FriendApplyBean friendApplyBean){
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                String tableName = FriendApplyBean.TABLE_NAME;
                Cursor cursor = query(tableName,new String[]{FriendApplyBean.COLUMN_ID},FriendApplyBean.COLUMN_SERVER_ID + " = ?",new String[]{friendApplyBean.serverID+""},null);
                if(null == cursor || cursor.moveToNext()){
                    e.onComplete();
                }else{
                    insert(tableName,friendApplyBean.toContentValues());
                }
            }
        }).subscribeOn(Schedulers.single()).
                subscribe();
    }

    public void mergeFriendBean(final FriendBean friendBean){
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                String tableName = FriendBean.TABLE_NAME;
                Cursor cursor = query(tableName,new String[]{FriendBean.COLUMN_ID},FriendBean.COLUMN_SERVER_ID + " = ?",new String[]{friendBean.serverID+""},null);
                if(null == cursor || cursor.moveToNext()){
                    e.onComplete();
                }else{
                    insert(tableName,friendBean.toContentValues());
                }
            }
        }).subscribeOn(Schedulers.single()).
                subscribe();
    }

    public void updateFriendApplyBean(final FriendApplyBean friendApplyBean){
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                String tableName = FriendApplyBean.TABLE_NAME;
                update(tableName,friendApplyBean.toContentValues(),FriendApplyBean.COLUMN_APPLY_ID + " = ? and " +
                        FriendApplyBean.COLUMN_CONFIRM_ID + " = ?",new String[]{friendApplyBean.applyID+"",friendApplyBean.confirmID+""});
            }
        }).subscribeOn(Schedulers.single()).
                subscribe();
    }

    public void mergeFriendBeanByFriendID(final FriendBean friendBean){
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                String tableName = FriendBean.TABLE_NAME;
                ContentValues contentValues = friendBean.toContentValues();
                int result = update(tableName,contentValues,FriendBean.COLUMN_APPLY_ID + " = ? and " + FriendBean.COLUMN_CONFIRM_ID + " = ?",
                        new String[]{friendBean.applyID+"",friendBean.confirmID+""});
                if(result <= 0){
                    insert(tableName,contentValues);
                }
            }
        }).subscribeOn(Schedulers.single()).
                subscribe();
    }

    public void queryFriendApplyMaxID(Consumer<Integer> consumer){
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                String tableName = FriendApplyBean.TABLE_NAME;
                String selections = FriendApplyBean.COLUMN_APPLY_ID + " = ? or " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?";
                Cursor cursor = query(tableName,new String[]{"max("+FriendApplyBean.COLUMN_SERVER_ID+")"},selections,new String[]{UserConfig.getID()+"",UserConfig.getID()+""},
                        null,null);
                if(null != cursor && cursor.moveToFirst()){
                    e.onNext(cursor.getInt(0));
                }else{
                    e.onNext(0);
                }
            }
        }).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).
                subscribe(consumer);
    }

    public void queryFriendApplyBean(Consumer<List<FriendApplyBean>> consumer, final int userID, final int otherID){
        Observable.create(new ObservableOnSubscribe<List<FriendApplyBean>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<FriendApplyBean>> e) throws Exception {
                String selections = "((" + FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?) or (" +
                        FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?)) and " + FriendApplyBean.COLUMN_STATUS +
                        " in (" + FriendApplyBean.STATUS_APPLYING + "," + FriendApplyBean.STATUS_CONFIRM + ")";
                Cursor cursor = query(FriendApplyBean.TABLE_NAME,null,selections,new String[]{userID+"",otherID+"",otherID+"",userID+""},null);
                List<FriendApplyBean> friendApplyBeen = new ArrayList<>();
                if(null != cursor && cursor.moveToNext()){
                    friendApplyBeen.add(FriendApplyBean.parseFromCursor(cursor));
                }
                e.onNext(friendApplyBeen);
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(consumer);
    }

    public void queryFriendBean(Consumer<List<FriendBean>> consumer, final int friendID) {
        Observable.create(new ObservableOnSubscribe<List<FriendBean>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<FriendBean>> e) throws Exception {
                Cursor cursor = query(FriendBean.TABLE_NAME, null, FriendBean.COLUMN_APPLY_ID + " = ? or " + FriendBean.COLUMN_CONFIRM_ID + " = ?",
                        new String[]{friendID+"",friendID+""}, null);
                List<FriendBean> friendBeen = new ArrayList<>();
                if (null != cursor && cursor.moveToNext()) {
                    friendBeen.add(FriendBean.parseFromCursor(cursor));
                }
                e.onNext(friendBeen);
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(consumer);
    }

    public void insertFriendApplyBeanByApplyID(Consumer<FriendApplyBean> consumer, final FriendApplyBean friendApplyBean){
        Observable.create(new ObservableOnSubscribe<FriendApplyBean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<FriendApplyBean> e) throws Exception {
                String selections = "(" + FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?) or (" +
                        FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?)";
                Cursor cursor = query(FriendApplyBean.TABLE_NAME,null,selections,new String[]{friendApplyBean.applyID+"",friendApplyBean.confirmID+"",
                        friendApplyBean.confirmID+"",friendApplyBean.applyID+""},null);
                if(null != cursor && cursor.moveToNext()){
                    e.onComplete();
                }else{
                    int result = (int) insert(FriendApplyBean.TABLE_NAME,friendApplyBean.toContentValues());
                    if(result != -1) {
                        friendApplyBean._id = result;
                    }
                    e.onNext(friendApplyBean);
                }
            }
        }).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).subscribe(consumer);
    }

    public void mergeFriendApplyBeanByApplyID(Consumer<FriendApplyBean> consumer, final FriendApplyBean friendApplyBean){
        Observable.create(new ObservableOnSubscribe<FriendApplyBean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<FriendApplyBean> e) throws Exception {
                String selections = "(" + FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?) or (" +
                        FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?)";
                int result = update(FriendApplyBean.TABLE_NAME,friendApplyBean.toContentValues(),selections,new String[]{friendApplyBean.applyID+"",friendApplyBean.confirmID+"",
                        friendApplyBean.confirmID+"",friendApplyBean.applyID+""});
                if(result <= 0){
                    int newID = (int) insert(FriendApplyBean.TABLE_NAME,friendApplyBean.toContentValues());
                    if(result != -1) {
                        friendApplyBean._id = newID;
                    }
                }
                e.onNext(friendApplyBean);
            }
        }).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).subscribe(consumer);
    }

    public void mergeFriendApplyBeanByApplyID(final List<FriendApplyBean> friendApplyBeans){
        if(friendApplyBeans.size() == 0){
            return;
        }
        Observable.create(new ObservableOnSubscribe<List<FriendApplyBean>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<FriendApplyBean>> e) throws Exception {
                SQLiteDatabase db = null;
                try {
                    db = getWritableDatabase();
                    db.beginTransaction();
                    for (FriendApplyBean friendApplyBean : friendApplyBeans) {
                        String selections = "(" + FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?) or (" +
                                FriendApplyBean.COLUMN_APPLY_ID + " = ? and " + FriendApplyBean.COLUMN_CONFIRM_ID + " = ?)";
                        int effectRows = db.update(FriendApplyBean.TABLE_NAME, friendApplyBean.toContentValues(), selections, new String[]{friendApplyBean.applyID + "", friendApplyBean.confirmID + "",
                                friendApplyBean.confirmID + "", friendApplyBean.applyID + ""});
                        if (effectRows <= 0) {
                            db.insert(FriendApplyBean.TABLE_NAME, null, friendApplyBean.toContentValues());
                        }
                    }
                    db.setTransactionSuccessful();
                }catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    if(null != db) {
                        db.endTransaction();
                    }
                }
            }
        }).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).subscribe();
    }

    public void mergeFriendBeanByFriendID(final List<FriendBean> friendBeans){
        if(friendBeans.size() == 0){
            return;
        }
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> e) throws Exception {
                String tableName = FriendBean.TABLE_NAME;
                SQLiteDatabase db = null;
                try {
                    db = getWritableDatabase();
                    db.beginTransaction();
                    for (FriendBean friendBean : friendBeans) {
                        ContentValues contentValues = friendBean.toContentValues();
                        int result = db.update(tableName, contentValues, FriendBean.COLUMN_APPLY_ID + " = ? and " + FriendBean.COLUMN_CONFIRM_ID + " = ?",
                                new String[]{friendBean.applyID+"",friendBean.confirmID+""});
                        if (result <= 0) {
                            db.insert(tableName, null,contentValues);
                        }
                    }
                    db.setTransactionSuccessful();
                }catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    if(null != db){
                        db.endTransaction();
                    }
                }
            }
        }).subscribeOn(Schedulers.single()).
                subscribe();
    }

}
