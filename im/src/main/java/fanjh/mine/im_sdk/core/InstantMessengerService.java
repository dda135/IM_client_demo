package fanjh.mine.im_sdk.core;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.util.List;

import fanjh.mine.im.R;
import fanjh.mine.im_sdk.DO.SendMessage;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.im_sdk.aidl.IServiceAidlInterface;
import fanjh.mine.im_sdk.aidl.Node;
import fanjh.mine.im_sdk.tools.Logger;

/**
* @author fanjh
* @date 2017/11/1 9:16
* @description 实际运行的服务
* @note 处于不同进程，用于中转处理长连接相关的一切事务
**/
public class InstantMessengerService extends Service {
    public static final int NOTIFY_ID = -1;
    public static final String TAG = "InstantMessengerService";
    public static final String EXTRA_CLIENT_ID = "extra_client_id";
    public static final String EXTRA_STOP_FLAG = "extra_stop_flag";
    public static final String EXTRA_IS_FOREGROUND = "extra_is_foreground";
    private InstantMessengerConnector connector;
    private boolean isForeground;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setForeground();
        if(null != intent && intent.hasExtra(EXTRA_STOP_FLAG) && intent.getBooleanExtra(EXTRA_STOP_FLAG,false)){
            if(null != connector){
                connector.disconnect();
            }
            ConnectivityManager.getInstance().stop();
            stopSelf();
        }else {
            Logger.LogDebug(TAG, "服务被唤醒！");
            if (null == connector) {
                Logger.LogDebug(TAG, "初始化连接者！");
                connector = new InstantMessengerConnector();
            }
            if(null != intent && intent.hasExtra(EXTRA_IS_FOREGROUND)){
                if(null != connector){
                    connector.setForeground(intent.getBooleanExtra(EXTRA_IS_FOREGROUND,false));
                }
            }
            if (null != intent && intent.hasExtra(EXTRA_CLIENT_ID) && null != intent.getStringExtra(EXTRA_CLIENT_ID)) {
                String clientID = intent.getStringExtra(EXTRA_CLIENT_ID);
                Logger.LogDebug(TAG, "当前有clientID，尝试根据当前clientID-->" + clientID + "去连接！");
                connector.reconnect(clientID);
            } else {
                Logger.LogDebug(TAG, "检查连接状态！");
                connector.reconnect();
            }
        }
        ConnectivityManager.getInstance().start();
        return START_STICKY;
    }

    private void setForeground(){
        if(isForeground){
            return;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.setContentTitle("提示");
            builder.setContentText("用于接收消息！");
            startForeground(NOTIFY_ID,builder.build());
        }else{
            startForeground(NOTIFY_ID,new Notification());
        }
        isForeground = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder.asBinder();
    }

    private IServiceAidlInterface.Stub binder = new IServiceAidlInterface.Stub() {

        @Override
        public void sendFileMessage(String content,String filePath,long listenerID) throws RemoteException {
            SendMessage message = new SendMessage();
            message.content = content;
            message.filePath = filePath;
            connector.sendMessage(message, listenerID);
        }

        @Override
        public void sendTextMessage(String content, long listenerID) throws RemoteException {

            SendMessage message = new SendMessage();
            message.content = content;
            connector.sendMessage(message, listenerID);

        }

        @Override
        public void refreshNode(List<Node> nodes) throws RemoteException {
            connector.changeNode(nodes);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != connector){
            connector.disconnect();
        }
        ConnectivityManager.getInstance().stop();
        isForeground = false;

        Intent intent = new Intent(InstantMessengerClient.getApplicationContext(),InstantMessengerService.class);
        InstantMessengerClient.getApplicationContext().startService(intent);
    }
}
