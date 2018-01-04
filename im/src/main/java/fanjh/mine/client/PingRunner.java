package fanjh.mine.client;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import fanjh.mine.client.exception.PingTimeoutException;
import fanjh.mine.client.strategy.BackgroundStrategy;
import fanjh.mine.client.strategy.ForegroundStrategy;
import fanjh.mine.client.strategy.PingStrategy;
import fanjh.mine.im_sdk.core.ConnectivityManager;
import fanjh.mine.im_sdk.core.DelayReceiverManager;
import fanjh.mine.proto.MessageProtocol;

/**
 * @author fanjh
 * @date 2017/11/27 11:29
 * @description 心跳执行者
 * @note 目前的方式是区分前后台（活跃态）+自适应的做法
 **/
public class PingRunner implements Handler.Callback {
    public static final String TAG = "PingRunner";
    public static final int MSG_CHECK_PING = 1;
    public static final int MSG_START_PING = 2;
    public static final String ALARM_ACTION = "action.fanjh.mine.client.pingrunner";
    /**
     * ping的等待时长，默认2s，如果ping超过2s没有响应，废弃当前连接
     * 必须小于ping的间隔
     */
    public static final int PING_WAITING_DURATION = 2000;
    /**
     * ping超时重试次数
     */
    public static final int PING_TIMEOUT_RETRY_COUNT = 3;

    private boolean isRunning = false;
    private IMConnector connector;
    private AtomicBoolean isPingSuccess = new AtomicBoolean(false);
    private AtomicBoolean isForeground = new AtomicBoolean(false);
    private ConnectivityManager.OnNetworkStateChangedCallback callback = new ConnectivityManager.OnNetworkStateChangedCallback() {
        @Override
        public void onChanged() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Utils.LogDebug(TAG, "网络状态发生变化！");
                    changePingStrategy();
                    pingSoon();
                }
            });
        }
    };
    private DelayReceiverManager alarmManager;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private HandlerThread thread;
    private Handler threadHandler;
    private PingStrategy pingStrategy;
    private ForegroundStrategy foregroundStrategy = new ForegroundStrategy();
    private BackgroundStrategy backgroundStrategy = new BackgroundStrategy();
    private AtomicInteger pingTimeoutRetryCount = new AtomicInteger(0);
    private long pingMessageID;

    private void changePingStrategy(){
        pingStrategy = isForeground.get()?foregroundStrategy:backgroundStrategy;
        pingStrategy.startPing();
    }

    PingRunner(IMConnector connector) {
        this.connector = connector;
        Utils.LogDebug(TAG, "初始化ping执行者！");
    }

    private void pingSuccess() {
        Utils.LogDebug(TAG, "ping成功！");
        pingStrategy.pingSuccess();
        isPingSuccess.set(true);
        long nextInterval = pingStrategy.getNextInterval();
        if(nextInterval > 0) {
            alarmManager.schedule(nextInterval);
        }
    }

    private void pingFailure() {
        pingStrategy.pingFailure();
        Utils.LogDebug(TAG, "ping失败！");
        isPingSuccess.set(false);
    }

    void changeForeground(boolean is) {
        isForeground.set(is);
        if (isRunning) {
            if (isForeground.get()) {
                callForeground();
            } else {
                callBackground();
            }
        }
    }

    void run(boolean foreground) {
        if (isRunning) {
            Utils.LogDebug(TAG, "当前已经在ping中了，无需重复进行！");
            return;
        }
        isRunning = true;
        ConnectivityManager.getInstance().addCallback(callback);
        thread = new HandlerThread("PingRunner");
        thread.start();
        threadHandler = new Handler(thread.getLooper(), this);
        alarmManager = new DelayReceiverManager(ALARM_ACTION, new DelayReceiverManager.ReceiverCallback() {
            @Override
            public void onAlarmCall() {
                pingSoon();
            }
        });
        isPingSuccess.set(true);
        isForeground.set(foreground);
        changePingStrategy();
        long nextInterval = pingStrategy.getNextInterval();
        if(nextInterval > 0) {
            alarmManager.tryStartDelayedReceiver(nextInterval);
            Utils.LogDebug(TAG, "开始执行ping操作,准备在" + (nextInterval / 1000) + "秒后进行ping操作！");
        }
    }

    boolean isRunning() {
        return isRunning;
    }

    boolean isPingSuccess() {
        return isPingSuccess.get();
    }

    void destroy() {
        Utils.LogDebug(TAG, "销毁ping执行者！");
        isRunning = false;
        pingMessageID = 0;
        if (null != threadHandler) {
            threadHandler.removeCallbacksAndMessages(null);
        }
        if (null != mainHandler) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        if (null != alarmManager) {
            alarmManager.cancelStartDelayedReceiver();
        }
        ConnectivityManager.getInstance().removeCallback(callback);
        if (null != thread) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    thread.quitSafely();
                } else {
                    thread.quit();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        threadHandler = null;
        mainHandler = null;
        if(null != pingStrategy){
            pingStrategy.stopPing();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CHECK_PING:
                if (!isPingSuccess()) {
                    Utils.LogDebug(TAG, "ping超时！");
                    pingFailure();
                    int remainingRetryCount = pingTimeoutRetryCount.decrementAndGet();
                    if(remainingRetryCount <= 0){
                        //当前重试次数已用完，应当先断开连接，然后重新建立连接
                        connector.callDisconnect(new PingTimeoutException("ping超时多次失败，断开连接！"));
                    }else{
                        //再次尝试进行ping操作
                        actuallyPing();
                    }
                }
                return true;
            case MSG_START_PING:
                actuallyPing();
                return true;
            default:
                break;
        }
        return false;
    }

    private void actuallyPing(){
        if (null != connector.getSocket()) {
            isPingSuccess.set(false);
            boolean isSend = connector.sendInnerMessage(++pingMessageID,ProtoType.PING_REQ, "");
            Utils.LogDebug(TAG, "发送ping报文到服务器！");
            if (isSend) {
                Utils.LogDebug(TAG, "启动ping超时检查！");
                threadHandler.sendEmptyMessageDelayed(MSG_CHECK_PING, PING_WAITING_DURATION);
            }
        }
    }

    private void callForeground() {
        pingStrategy = foregroundStrategy;
        //这里可能会不准
        pingStrategy.startPing();
        if(isRunning()) {
            pingSoon();
            Utils.LogDebug(TAG, "回到前台，立刻进行一次ping操作，保证消息收取的及时性！");
        }
        isForeground.set(true);
    }

    private void callBackground() {
        pingStrategy = backgroundStrategy;
        pingStrategy.startPing();
        if(isRunning()) {
            long pingInterval = backgroundStrategy.getNextInterval();
            pingSchedule(pingInterval);
            Utils.LogDebug(TAG, "进入后台，下一次将会在" + (pingInterval / 1000) + "秒后进行ping！");
        }
        isForeground.set(false);
    }

    void receiverACK(MessageProtocol.MessageProto messageProto){
        long pingACKMessageID = messageProto.getId();
        if(pingACKMessageID == pingMessageID) {
            pingSuccess();
            Utils.Log("ping服务器成功！");
        }
        //可能会存在延迟的ping包，丢弃不处理
    }

    private void pingSchedule(long interval){
        alarmManager.cancelSchedule();
        threadHandler.removeCallbacksAndMessages(null);
        alarmManager.schedule(interval);
    }

    private void pingSoon(){
        alarmManager.cancelSchedule();
        threadHandler.removeCallbacksAndMessages(null);
        pingTimeoutRetryCount.set(PING_TIMEOUT_RETRY_COUNT);
        threadHandler.sendEmptyMessage(MSG_START_PING);
    }

}
