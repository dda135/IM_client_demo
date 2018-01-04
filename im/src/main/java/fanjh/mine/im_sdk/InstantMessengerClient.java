package fanjh.mine.im_sdk;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.util.LongSparseArray;
import android.util.SparseArray;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import fanjh.mine.client.IMConnector;
import fanjh.mine.im_sdk.aidl.IServiceAidlInterface;
import fanjh.mine.im_sdk.aidl.Node;
import fanjh.mine.im_sdk.core.ForegroundChecker;
import fanjh.mine.im_sdk.core.InstantMessengerService;
import fanjh.mine.im_sdk.core.NodeLoader;
import fanjh.mine.im_sdk.core.NodePuller;
import fanjh.mine.im_sdk.core.NotifyAction;
import fanjh.mine.im_sdk.tools.Logger;
import fanjh.mine.im_sdk.tools.Utils;

/**
 * @author fanjh
 * @date 2017/11/1 13:41
 * @description 客户端管理
 * @note sample：
 * public class MainApplication extends Application {
 * @Override
 * public void onCreate() {
 *  super.onCreate();
 *  startPushService();
 * }
 * <p>
 * private void startPushService(){
 * InstantMessengerClient.attach(this).start(new NodePuller() {
 * @Override
 * public List<Node> getNodes() {
 *  List<Node> nodes = new ArrayList<Node>();
 *  Node node = new Node("119.23.231.0", 1883);
 *  nodes.add(node);
 *  return nodes;
 * }
 * },null);
 * <p>
 * }
 * <p>
 * }
 * 此类为所有API的入口
 * 客户端只能于当前InstantMessengerClient进行交互
 * 即命令模式
 **/
public class InstantMessengerClient implements ForegroundChecker.Callback{
    public static Application application;
    private ForegroundChecker foregroundChecker;
    private IServiceAidlInterface serviceAidlInterface;
    private NodeLoader nodeLoader;
    private ServiceConnection currentServiceConnection;
    private LongSparseArray<IMConnector.ResultListener> listenerSparseArray;
    private AtomicBoolean isReceiverRegistered = new AtomicBoolean(false);
    private Handler mainHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            int action = bundle.getInt(NotifyAction.EXTRA_MSG,-1);
            long listenerID = bundle.getLong(NotifyAction.EXTRA_LISTENER_ID,-1);
            synchronized (InstantMessengerClient.class) {
                IMConnector.ResultListener listener = listenerSparseArray.get(listenerID);
                if(null != listener) {
                    switch (action) {
                        case NotifyAction.MSG_SEND_FAILURE:
                            listener.onError(new Exception("发送失败！"));
                            break;
                        case NotifyAction.MSG_SEND_SUCCESS:
                            listener.onSuccess();
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    };
    private BroadcastReceiver defaultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(NotifyAction.ACTION.equals(intent.getAction())){
                int action = intent.getIntExtra(NotifyAction.EXTRA_MSG,-1);
                long listenerID = intent.getLongExtra(NotifyAction.EXTRA_LISTENER_ID,-1);
                Message message = mainHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt(NotifyAction.EXTRA_MSG,action);
                bundle.putLong(NotifyAction.EXTRA_LISTENER_ID,listenerID);
                message.setData(bundle);
                message.sendToTarget();
            }
        }
    };

    @Override
    public void callForeground() {
        Intent intent = new Intent(application,InstantMessengerService.class);
        intent.putExtra(InstantMessengerService.EXTRA_IS_FOREGROUND,true);
        application.startService(intent);
    }

    @Override
    public void callBackground() {
        Intent intent = new Intent(application,InstantMessengerService.class);
        intent.putExtra(InstantMessengerService.EXTRA_IS_FOREGROUND,false);
        application.startService(intent);
    }

    private static class InstantMessengerClientHolder {
        static final InstantMessengerClient CLIENT_HOLDER = new InstantMessengerClient();
    }

    public static InstantMessengerClient getInstance() {
        return InstantMessengerClientHolder.CLIENT_HOLDER;
    }

    /**
     * 关联当前主进程
     * 注意需要区分进程，特别是在有多个进程的app中
     * 必须在所有操作之前
     * @param app 当前要关联的Application
     */
    public static InstantMessengerClient attach(Application app) {
        application = app;
        return getInstance();
    }

    /**
     * 必须在attach之后调用
     * @return Android环境
     */
    public static Context getApplicationContext() {
        return application.getApplicationContext();
    }

    private InstantMessengerClient() {
        nodeLoader = new NodeLoader();
        foregroundChecker = ForegroundChecker.getInstance();
        foregroundChecker.attach(application);
        foregroundChecker.addListener(this);
        foregroundChecker.start();
        bindService(null);
        listenerSparseArray = new LongSparseArray<>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NotifyAction.ACTION);
        application.registerReceiver(defaultReceiver,intentFilter);
    }

    public void tryConnect(String clientID) {
        Intent intent = new Intent(getApplicationContext(), InstantMessengerService.class);
        intent.putExtra(InstantMessengerService.EXTRA_CLIENT_ID, clientID);
        getApplicationContext().startService(intent);
    }

    public void tryConnect() {
        Intent intent = new Intent(getApplicationContext(), InstantMessengerService.class);
        getApplicationContext().startService(intent);
    }

    public void disconnect() {
        Intent intent = new Intent(getApplicationContext(), InstantMessengerService.class);
        intent.putExtra(InstantMessengerService.EXTRA_STOP_FLAG,true);
        if(null != currentServiceConnection) {
            getApplicationContext().unbindService(currentServiceConnection);
        }
        getApplicationContext().startService(intent);
        if(isReceiverRegistered.get()){
            application.unregisterReceiver(defaultReceiver);
        }
        synchronized (InstantMessengerClient.class){
            listenerSparseArray.clear();
        }
    }

    public void start(NodePuller nodePuller, String clientID) {
        if (Utils.isMainProcess()) {
            if (null == nodePuller) {
                throw new IllegalArgumentException("节点必须有获取方式！");
            }
            nodeLoader.setNodePuller(nodePuller);
            Logger.LogDebug("main application start,try start service");
            InstantMessengerClient.getInstance().tryConnect(clientID);
        }
    }

    public void sendTextMessage(final String content, final IMConnector.ResultListener listener) {
        if (null == serviceAidlInterface) {
            bindService(new Runnable() {
                @Override
                public void run() {
                    sendTextMessage(content,listener);
                }
            });
        } else {
            try {
                long listenerID = -1;
                if(null != listener) {
                    synchronized (InstantMessengerClient.class) {
                        listenerID = SystemClock.uptimeMillis();
                        listenerSparseArray.put(listenerID, listener);
                    }
                }
                serviceAidlInterface.sendTextMessage(content,listenerID);
            } catch (RemoteException e) {
                e.printStackTrace();
                NotifyAction.notifyAction(NotifyAction.MSG_SEND_FAILURE,content);
            }
        }
    }

    public void sendFileMessage(final String content, final String filePath, final IMConnector.ResultListener listener) {
        if (null == serviceAidlInterface) {
            bindService(new Runnable() {
                @Override
                public void run() {
                    sendFileMessage(content,filePath,listener);
                }
            });
        } else {
            try {
                long listenerID = -1;
                if(null != listener) {
                    synchronized (InstantMessengerClient.class) {
                        listenerID = SystemClock.uptimeMillis();
                        listenerSparseArray.put(listenerID, listener);
                    }
                }
                serviceAidlInterface.sendFileMessage(content,filePath,listenerID);
            } catch (RemoteException e) {
                e.printStackTrace();
                NotifyAction.notifyAction(NotifyAction.MSG_SEND_FAILURE,content);
            }
        }
    }

    private void bindService(final Runnable runnable) {
        Intent intent = new Intent(getApplicationContext(), InstantMessengerService.class);
        currentServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Logger.LogDebug("messenger已连接！");
                serviceAidlInterface = IServiceAidlInterface.Stub.asInterface(service);
                if (null != runnable) {
                    runnable.run();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Logger.LogDebug("messenger断开！");
                serviceAidlInterface = null;
            }
        };
        getApplicationContext().bindService(intent, currentServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void changeNode(final List<Node> nodes) {
        if (null == serviceAidlInterface) {
            bindService(new Runnable() {
                @Override
                public void run() {
                    changeNode(nodes);
                }
            });
        } else {
            try {
                serviceAidlInterface.refreshNode(nodes);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerListenerReceiver(BroadcastReceiver receiver){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NotifyAction.ACTION);
        getApplicationContext().registerReceiver(receiver,intentFilter);
    }

}
