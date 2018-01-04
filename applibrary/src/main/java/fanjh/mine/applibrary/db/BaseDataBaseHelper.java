package fanjh.mine.applibrary.db;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;


import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.encrypt.Worker;

/**
* @author fanjh
* @date 2017/7/7 9:21
* @description 封装一些基础操作
**/
public abstract class BaseDataBaseHelper extends SQLiteOpenHelper {
    protected Context mContext;

    public BaseDataBaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(new DBContextWrapper(context), name, factory, version);
        this.mContext = context;
    }

    public int update(String tableName, ContentValues values, String selection, String[] selectionArgs){
        int result = -1;
        if(null == mContext){
            return result;
        }
        try {
            SQLiteDatabase db = getWritableDatabase();
            result = db.update(tableName, values, selection, selectionArgs);
        }catch (SQLiteException ex){
            ex.printStackTrace();//当前磁盘已满if the database cannot be opened for writing
        }
        return result;
    }

    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int result = update(uri.getPathSegments().get(0), values, selection, selectionArgs);
        mContext.getContentResolver().notifyChange(uri, null);
        return result;
    }

    public long insert(String tableName, ContentValues values){
        long result = -1;
        if(null == mContext){
            return result;
        }
        try {
            SQLiteDatabase db = getWritableDatabase();
            result = db.insert(tableName,null,values);//不允许插入空行
        }catch (SQLiteException ex){
            ex.printStackTrace();//当前磁盘已满if the database cannot be opened for writing
        }
        return result;
    }

    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long insertId = insert(uri.getPathSegments().get(0),values);
        if(-1 == insertId){
            return uri;
        }
        uri = ContentUris.withAppendedId(uri, insertId);
        mContext.getContentResolver().notifyChange(uri, null);
        return uri;
    }

    public int delete(String tableName, String whereClause, String[] whereArgs) {
        int effectRow = -1;
        if(null == mContext){
            return effectRow;
        }
        try {
            SQLiteDatabase db = getWritableDatabase();
            effectRow = db.delete(tableName,whereClause,whereArgs);
        }catch (SQLiteException ex){
            ex.printStackTrace();//当前磁盘已满if the database cannot be opened for writing
        }
        return effectRow;
    }

    public int delete(@NonNull Uri uri, String whereClause, String[] whereArgs) {
        int effectRow = delete(uri.getPathSegments().get(0),whereClause,whereArgs);
        if(-1 != effectRow) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return effectRow;
    }

    public Cursor query(String tableName, String[] columns, String selection, String[] selectionArgs, String sortOrder){
        if(null == mContext){
            return null;
        }
        try {
            SQLiteDatabase db = getReadableDatabase();
            return db.query(tableName,columns, selection, selectionArgs, null, null, sortOrder);
        }catch (SQLiteException ex){
            ex.printStackTrace();//if the database cannot be opened
        }
        return null;
    }

    public Cursor query(String tableName, String[] columns, String selection, String[] selectionArgs, String sortOrder, String limit){
        if(null == mContext){
            return null;
        }
        try {
            SQLiteDatabase db = getReadableDatabase();
            return db.query(tableName,columns, selection, selectionArgs, null, null, sortOrder, limit);
        }catch (SQLiteException ex){
            ex.printStackTrace();//if the database cannot be opened
        }
        return null;
    }

    public Cursor query(@NonNull Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder){
        Cursor cursor = query(uri.getPathSegments().get(0), columns, selection, selectionArgs, sortOrder);
        if (null != cursor) {
            cursor.setNotificationUri(mContext.getContentResolver(), uri);
        }
        return cursor;
    }

    public long getCount(String tableName,String where,String[] whereArgs){
        long result = -1;
        if(null == mContext){
            return result;
        }
        Cursor cursor = null;
        try {
            cursor = query(tableName,new String[]{"count(*)"},where,whereArgs,null);
            if(null == cursor){
                return result;
            }
            if (cursor.moveToFirst()) {
                result = cursor.getLong(0);
            }
            return result;
        }catch (SQLiteException ex){
            ex.printStackTrace();
        }finally {
            if(null != cursor && !cursor.isClosed()){
                cursor.close();
            }
        }
        return result;
    }

    public void insertMoreData(String tableName, List<ContentValues> contentValuesList){
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            for(ContentValues values:contentValuesList){
                db.insert(tableName,null,values);//不允许插入空行
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }catch (SQLiteException ex){
            ex.printStackTrace();//当前磁盘已满if the database cannot be opened for writing
        }
    }

    public Cursor execQuerySQL(String sql){
        if(null == mContext){
            return null;
        }
        try {
            SQLiteDatabase db = getReadableDatabase();
            return db.rawQuery(sql,null);
        }catch (SQLiteException ex){
            ex.printStackTrace();//当前磁盘已满if the database cannot be opened for writing
        }
        return null;
    }

    public Cursor execQuerySQL(@NonNull Uri uri, String sql){
        Cursor cursor = execQuerySQL(sql);
        if(null != cursor) {
            cursor.setNotificationUri(mContext.getContentResolver(), uri);
        }
        return cursor;
    }

    /**
    * @author fanjh
    * @date 2017/9/5 17:39
    * @description 装饰Context用以通过用户来区分DB文件目录
    * @note
    **/
    public static class DBContextWrapper extends ContextWrapper{

        public DBContextWrapper(Context base) {
            super(base);
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
            return this.openOrCreateDatabase(name, mode, factory, null);
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
            return super.openOrCreateDatabase(getConcatDatabasePath(getBaseContext(),name), mode, factory, errorHandler);
        }

        /**
         * 进行存储目录的自定义
         */
        private String getConcatDatabasePath(Context context,String name){
            try {
                if(UserConfig.getID() <= 0){
                    return name;
                }
                //此处是database/aaaaaaaaaaaaaaaaaa这个目录
                File file = context.getDatabasePath(Worker.md5("time" + UserConfig.getID() + "1"));
                file.mkdirs();
                if (file.exists() && file.isDirectory()) {
                    //在当前目录下创建对应的数据库文件
                    File newFile = new File(file, name);
                    if (!newFile.exists()) {
                        try {
                            newFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return newFile.exists() ? newFile.getAbsolutePath() : name;
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
            //当前目录创建失败
            //默认使用原始路径
            return name;
        }

    }

}
