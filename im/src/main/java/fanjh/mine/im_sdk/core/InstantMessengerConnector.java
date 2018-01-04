package fanjh.mine.im_sdk.core;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import fanjh.mine.client.Address;
import fanjh.mine.client.IMConnector;
import fanjh.mine.im_sdk.DO.SendMessage;
import fanjh.mine.im_sdk.aidl.Node;
import fanjh.mine.im_sdk.tools.Logger;
import fanjh.mine.im_sdk.tools.Utils;

/**
 * @author fanjh
 * @date 2017/10/31 15:12
 * @description 连接者
 * @note 实际处理长连接、订阅和发送等一系列行为的执行者
 **/
public class InstantMessengerConnector {
    public static final String TAG = "InstantMessengerConnector";
    public static final String ALARM_ACTION = "fanjh.mine.imworker.instantmessengerconnector.alarm.receiver";
    /**
     * 下一次定时器唤醒的间隔
     */
    public static final long DEFAULT_ALARM_TIME = 20 * 1000;
    /**
     * 连接最大重试数量
     */
    public static final int MAX_CONNECT_RETRY_COUNT = 4;
    /**
     * 消息发送最大重试数量
     */
    public static final int MAX_SEND_RETRY_COUNT = 2;
    private IMConnector mCurrentClient;
    private NodeManager mCurrentNodeManager;
    private boolean isConnecting;
    private boolean canUsed = true;
    private DelayReceiverManager delayReceiverManager;
    private int retryCount;
    private ConnectivityManager.OnNetworkStateChangedCallback callback = new ConnectivityManager.OnNetworkStateChangedCallback() {
        @Override
        public void onChanged() {
            Logger.LogDebug(TAG,"网络状态变化！");
            if(!isConnected() && Utils.isOnline()) {
                reconnect();
            }
        }
    };
    private String currentClientID;

    public InstantMessengerConnector() {
        mCurrentNodeManager = new NodeManager();
        mCurrentClient = new IMConnector();
    }

    public void setForeground(boolean is){
        mCurrentClient.changeForeground(is);
    }

    public void changeNode(List<Node> nodes){
        mCurrentNodeManager.changeNode(nodes);
    }

    /**
     * 根据当前提供的clientID去连接
     * 如果当前clientID变化，则说明需要废弃旧的连接，并且需要建立新的连接
     * @param clientID 新的clientID
     */
    public void reconnect(String clientID) {
        if(null == clientID){
            if(null == currentClientID){
                Logger.LogDebug(TAG,"clientID都为空，没有发生变化！");
                return;
            }else{
                Logger.LogDebug(TAG,"将已有的clientID变为空！");
                currentClientID = null;
                disconnect();
            }
        }else{
            if(!clientID.equals(currentClientID)){
                Logger.LogDebug(TAG,"当前clientID发生变化！");
                currentClientID = clientID;
                disconnect();
            }
            reconnect();
        }
    }

    public boolean reconnect() {
        Logger.LogDebug(TAG,"准备去重新连接！");
        if(!canUsed){
            Logger.LogDebug(TAG,"当前连接者不可用，终止连接操作！");
            return false;
        }
        if(isConnecting){
            Logger.LogDebug(TAG,"当前正在进行连接，不需要重复连接！");
            return false;
        }
        if(null == currentClientID){
            Logger.LogDebug(TAG,"当前clientID为空，无法进行连接！");
            return false;
        }
        if (null == mCurrentClient || !mCurrentClient.isConnected()) {
            connectAsync();
            return true;
        }
        return false;
    }

    private void connectAsync() {
        unRegisterConnectDelayed();
        if(retryCount > MAX_CONNECT_RETRY_COUNT){
            Logger.LogDebug(TAG,"当前连接次数已经达到最大值！");
            mCurrentNodeManager.serializable();
            retryCount = 0;
            registerConnectDelayed();
            return;
        }
        if(!Utils.isOnline()){
            Logger.LogDebug(TAG,"当前网络不可用，终止连接操作！");
            registerConnectDelayed();
            return;
        }
        Logger.LogDebug(TAG,"尝试去获取已有节点！");
        final Node node = mCurrentNodeManager.getAvailableNode();
        if (null == node) {
            Logger.LogDebug(TAG,"当前没有可用节点，终止连接操作！");
            isConnecting = false;
            unRegisterConnectDelayed();
            return;
        }
        try {
            if(null == currentClientID){
                Logger.LogDebug(TAG,"当前clientID为空，终止连接操作！");
                return;
            }
            Logger.LogDebug(TAG,"进行连接！");
            Address address = new Address();
            address.host = node.getIp();
            address.port = node.getPort();
            address.clientID = currentClientID;
            mCurrentClient.connect(address, 6000, new IMConnector.ResultListener() {
                @Override
                public void onSuccess() {
                    Logger.LogDebug(TAG,"clientID-->" + currentClientID + "-->连接成功！");
                    mCurrentNodeManager.establishConnection(node,true);
                    isConnecting = false;
                    unRegisterConnectDelayed();
                    retryCount = 0;
                }

                @Override
                public void onError(Throwable ex) {
                    Logger.LogDebug(TAG,"clientID-->" + currentClientID + "-->连接失败！");
                    mCurrentNodeManager.connectFailure(node,false);
                    isConnecting = false;
                    retryCount++;
                    reconnect();
                }
            });
            mCurrentClient.setConnectionListener(new IMConnector.ConnectionListener() {
                @Override
                public void onConnectionLost() {
                    Logger.LogDebug(TAG,"连接断开！");
                    reconnect();
                }

                @Override
                public void messageArrived(String content) {
                    Logger.LogDebug(TAG,"收到一条消息");
                    NotifyAction.notifyAction(NotifyAction.MSG_MESSAGE_ARRIVED,content);
                }
            });
            isConnecting = true;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.LogDebug(TAG,"连接中出现异常！");
            isConnecting = false;
            retryCount++;
            reconnect();
        }
    }

    private void registerConnectDelayed(){
        Logger.LogDebug(TAG,"准备延时连接！");
        if(null == currentClientID){
            Logger.LogDebug(TAG,"当前clientID为空，终止延时连接！");
            return;
        }
        if(null == delayReceiverManager){
            delayReceiverManager = new DelayReceiverManager(ALARM_ACTION, new DelayReceiverManager.ReceiverCallback() {
                @Override
                public void onAlarmCall() {
                    Logger.LogDebug(TAG,"定时器唤醒",System.currentTimeMillis());
                    reconnect();
                }
            });
        }
        ConnectivityManager.getInstance().addCallback(callback);
        if(Utils.isOnline()) {
            delayReceiverManager.tryStartDelayedReceiver(DEFAULT_ALARM_TIME);
            Logger.LogDebug(TAG, "当前有网络，延时连接准备完成！");
        }else{
            Logger.LogDebug(TAG, "当前没有网络，等待网络状态变化");
        }
    }

    private void unRegisterConnectDelayed(){
        if(null != delayReceiverManager) {
            delayReceiverManager.cancelStartDelayedReceiver();
        }
        ConnectivityManager.getInstance().removeCallback(callback);
        Logger.LogDebug(TAG,"取消延时进行连接！");
    }

    public boolean isConnected(){
        return null != mCurrentClient && mCurrentClient.isConnected();
    }

    public void setCanUsed(boolean canUsed) {
        this.canUsed = canUsed;
    }

    public void sendMessage(final SendMessage message, final long listenerID){
        Logger.LogDebug(TAG,"尝试去发送消息！");
        if(isConnected()){
            Logger.LogDebug(TAG,"当前连接中，可以发送消息！");
            mCurrentClient.sendMessage(message.content, message.filePath, new IMConnector.ResultListener() {
                @Override
                public void onSuccess() {
                    Logger.LogDebug(TAG,message + "-->消息发送成功！");
                    NotifyAction.notifyAction(NotifyAction.MSG_SEND_SUCCESS,message.content,listenerID);
                }

                @Override
                public void onError(Throwable ex) {
                    handleSendMessageFailure(message,listenerID);
                }
            });
        }else{
            Logger.LogDebug(TAG,"当前连接未建立，无法发送消息！");
            NotifyAction.notifyAction(NotifyAction.MSG_SEND_FAILURE,message.content,listenerID);
        }
    }

    private void handleSendMessageFailure(SendMessage message,long listenerID){
        Logger.LogDebug(TAG,message.content + "-->消息第" + (message.retryCount+1) + "发送失败！");
        if(message.retryCount >= MAX_SEND_RETRY_COUNT){
            Logger.LogDebug(TAG,message.content + "-->消息发送已经到达最大次数，消息发送失败！");
            NotifyAction.notifyAction(NotifyAction.MSG_SEND_FAILURE,message.content,listenerID);
            return;
        }
        Logger.LogDebug(TAG,message.content + "-->尝试重新发送当前消息！");
        message.retryCount++;
        sendMessage(message,listenerID);
    }

    public void disconnect(){
        Logger.LogDebug(TAG,"尝试去关闭之前的连接！");
        if(null != mCurrentClient && mCurrentClient.isConnected()){
            mCurrentClient.disconnect();
            Logger.LogDebug(TAG,"之前的连接成功关闭！");
        }
        ConnectivityManager.getInstance().removeCallback(callback);
        if(null != delayReceiverManager) {
            delayReceiverManager.cancelStartDelayedReceiver();
        }
    }

}
