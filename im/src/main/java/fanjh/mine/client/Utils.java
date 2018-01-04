package fanjh.mine.client;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

import fanjh.mine.im_sdk.InstantMessengerClient;

/**
* @author fanjh
* @date 2017/11/23 14:21
**/
public class Utils {
    public static final String TAG = "IMConnector";

    public static void close(Closeable closeable){
        if(null != closeable){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(Socket socket){
        if(null != socket && socket.isConnected() && !socket.isClosed()){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void Log(String content){
        if(null == content){
            return;
        }
        Log.i(TAG,content);
    }

    public static void LogDebug(String tag,String content){
        if(null == content){
            return;
        }
        Log.d(tag,content);
    }

    /**
     * 返回运营商 需要加入权限 <uses-permission android:name="android.permission.READ_PHONE_STATE"/> <BR>
     *
     * @return 1,代表中国移动，2，代表中国联通，3，代表中国电信，0，代表未知
     * @author youzc@yiche.com
     */
    public static int getOperators() {
        // 移动设备网络代码（英语：Mobile Network Code，MNC）是与移动设备国家代码（Mobile Country Code，MCC）（也称为“MCC /
        // MNC”）相结合, 例如46000，前三位是MCC，后两位是MNC 获取手机服务商信息
        int OperatorsType = 0;
        Context context = InstantMessengerClient.getApplicationContext();
        try {
            String IMSI = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId();
            if (null == IMSI) {
                return OperatorsType;
            }
            if (IMSI.startsWith("46000") || IMSI.startsWith("46002") || IMSI.startsWith("46007")) {
                OperatorsType = 1;
            } else if (IMSI.startsWith("46001") || IMSI.startsWith("46006")) {
                OperatorsType = 2;
            } else if (IMSI.startsWith("46003") || IMSI.startsWith("46005")) {
                OperatorsType = 3;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return OperatorsType;
    }

}
