package fanjh.mine.im_sdk.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.im_sdk.aidl.Node;
import fanjh.mine.im_sdk.tools.Logger;
import fanjh.mine.im_sdk.tools.Utils;

/**
* @author fanjh
* @date 2017/11/21 15:13
* @description 加点读取者
* @note 用于获取节点，最通俗易懂的流程就是先获取节点，然后根据节点的ip和端口去建立长连接
**/
public class NodeLoader {
    public static final String TAG = "NodeLoader";
    public static final String ACTION_GET_NODE = "action_NodeLoader_getNode";
    public static final String ALARM_RECEIVER_ACTION = "fanjh.mine.imworker.nodemanager.alarm.receiver";
    /**
     * 一次拉取节点的最大尝试数量
     */
    public static final int MAX_RETRY_COUNT = 3;
    /**
     * 当前有网络的情况下下一次定时唤醒的间隔
     */
    public static final int NET_ALARM_TIME = 60 * 1000;
    /**
     * 节点拉取的锁，节点不应该重复拉取
     */
    private AtomicBoolean nodeLocker = new AtomicBoolean(false);
    private NodePuller nodePuller;
    private int retryCount = 0;
    private DelayReceiverManager delayReceiverManager;

    /**
     * 网络状态变化回调
     */
    private ConnectivityManager.OnNetworkStateChangedCallback callback = new ConnectivityManager.OnNetworkStateChangedCallback() {
        @Override
        public void onChanged() {
            Logger.LogDebug(TAG,"网络状态变化！");
            boolean isOnline = getNodeWhenOnline();
        }
    };

    private void tryGetNodeDelayed() {
        if(null == delayReceiverManager){
            delayReceiverManager = new DelayReceiverManager(ALARM_RECEIVER_ACTION, new DelayReceiverManager.ReceiverCallback() {

                @Override
                public void onAlarmCall() {
                    Logger.LogDebug(TAG,"定时器唤醒",System.currentTimeMillis());
                    tryGetNode();
                }
            });
        }
        ConnectivityManager.getInstance().addCallback(callback);
        if(Utils.isOnline()) {
            delayReceiverManager.tryStartDelayedReceiver(NET_ALARM_TIME);
            Logger.LogDebug(TAG, "延时加载准备完成", System.currentTimeMillis());
        }else{
            Logger.LogDebug(TAG, "当前没有网络，等待网络变化！");
        }
    }

    private void cancelGetNodeDelayed() {
        if (null != delayReceiverManager) {
            delayReceiverManager.cancelStartDelayedReceiver();
        }
        ConnectivityManager.getInstance().removeCallback(callback);
        Logger.LogDebug(TAG,"取消延时加载");
    }

    private boolean getNodeWhenOnline() {
        boolean isOnline = Utils.isOnline();
        if (isOnline) {
            Logger.LogDebug(TAG,"当前有可使用网络");
            tryGetNode();
        }else{
            Logger.LogDebug(TAG,"当前无可使用网络");
        }
        return isOnline;
    }

    private void tryGetNode() {
        Logger.LogDebug(TAG,"开始尝试去拉取节点");
        cancelGetNodeDelayed();
        nodeLocker.set(true);
        Utils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                getNodeInterval();
            }
        });
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(null == nodePuller){
                Logger.LogDebug(TAG,"当前节点拉取者还没有初始化，无法拉取节点");
                return;
            }
            if(nodeLocker.get()){
                Logger.LogDebug(TAG,"当前节点拉取中，不应该重复拉取");
                return;
            }
            if(ACTION_GET_NODE.equals(intent.getAction())){
                nodeLocker.set(true);
                Logger.LogDebug(TAG,"成功收到外部通知，开始拉取节点");
                Utils.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        getNodeInterval();
                    }
                });
            }
        }
    };

    private void getNodeInterval(){
        if (retryCount < MAX_RETRY_COUNT) {
            if(retryCount == 0){
                Logger.LogDebug(TAG,"开始第" + (retryCount+1) + "次拉取节点");
            }
            cancelGetNodeDelayed();
            Utils.getFixThreadPoolExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    List<Node> nodes = nodePuller.getNodes();
                    if(null == nodes){
                        retryCount++;
                        Logger.LogDebug(TAG,"节点获取失败！");
                        getNodeInterval();
                    }else {
                        Logger.LogDebug(TAG,"节点获取成功！");
                        retryCount = 0;
                        Logger.LogDebug(TAG,"尝试修改当前节点列表！");
                        InstantMessengerClient.getInstance().changeNode(nodePuller.getNodes());
                        nodeLocker.set(false);
                    }
                }
            });
        } else {
            Logger.LogDebug(TAG,"当前已经达到拉取节点的最大次数！");
            retryCount = 0;
            tryGetNodeDelayed();
            nodeLocker.set(false);
        }
    }

    public NodeLoader() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GET_NODE);
        InstantMessengerClient.getApplicationContext().registerReceiver(receiver,filter);
        Logger.LogDebug(TAG,"广播接收消息初始化完成！");
    }

    public void setNodePuller(NodePuller nodePuller) {
        this.nodePuller = nodePuller;
        Logger.LogDebug(TAG,"节点拉取者初始化完成！");
    }
}
