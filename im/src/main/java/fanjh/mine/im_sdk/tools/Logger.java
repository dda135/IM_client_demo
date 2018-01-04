package fanjh.mine.im_sdk.tools;

import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
* @author fanjh
* @date 2017/11/1 13:57
* @description 日志处理者
* @note
**/
public class Logger {
    public static final String TAG = "InstantMessenger";

    public static void LogDebug(String content){
        if(TextUtils.isEmpty(content)){
            throw new NullPointerException("输出日志内容不能为空！");
        }
        Log.d(TAG,content);
    }

    public static void LogDebug(String tag,String content){
        if(TextUtils.isEmpty(content)){
            throw new NullPointerException("输出日志内容不能为空！");
        }
        Log.d(tag,content);
    }

    public static void LogDebug(String tag,String content,long timestamp){
        if(TextUtils.isEmpty(content)){
            throw new NullPointerException("输出日志内容不能为空！");
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d(tag,content+"-->time:"+format.format(new Date(timestamp)));
    }

}
