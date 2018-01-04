package fanjh.mine.client.strategy;

import android.content.Context;
import android.content.SharedPreferences;

import fanjh.mine.client.Utils;
import fanjh.mine.im_sdk.InstantMessengerClient;

/**
* @author fanjh
* @date 2017/11/28 9:38
* @description 后台状态下的心跳策略
* @note 采用较长间隔的方式去检测NAT超时，可能存在NAT超时时长的延迟
**/
public class BackgroundStrategy implements PingStrategy {
    public static final String TAG = "BackgroundStrategy";
    /**
     * 初始间隔
     */
    public static final long INIT_INTERVAL = 1000 * 60 * 4;
    /**
     * 最大间隔
     */
    public static final long MAX_INTERVAL = 1000 * 60 * 27;
    /**
     * 步长为0.5分钟
     */
    public static final long STEP = 1000 * 30;
    public static final String CACHE_FILE = "BackgroundStrategy";
    private SharedPreferences sp;
    private long currentPingInterval;
    private long lastStableInterval;

    public BackgroundStrategy() {
        sp = InstantMessengerClient.getApplicationContext().getSharedPreferences(CACHE_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public void pingSuccess() {
        lastStableInterval = currentPingInterval;
        Utils.LogDebug(TAG,"当前到达稳定态！");
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(getCacheKey(),currentPingInterval);
        editor.apply();
        if(currentPingInterval + STEP > MAX_INTERVAL){
            Utils.LogDebug(TAG,"当前到达稳定态,并且已经为最大值！");
        }else{
            currentPingInterval += STEP;
        }
        log();
    }

    @Override
    public void pingFailure() {
        if(lastStableInterval > 0){
            //当次心跳过程中已经测出存在稳定态的心跳间隔
            //使用稳定态间隔进行心跳
            Utils.LogDebug(TAG,"心跳失败，使用之前已经测量的稳定态进行心跳！");
            currentPingInterval = lastStableInterval;
        }else{
            Utils.LogDebug(TAG,"心跳失败，之前没有测量出的稳定态，向下校验！");
            //向下检测
            currentPingInterval -= STEP;
        }
        log();
    }

    @Override
    public void startPing() {
        long cacheInterval = sp.getLong(getCacheKey(),0);
        Utils.LogDebug(TAG,"开始心跳！"+(cacheInterval==0?"当前没有缓存！":"当前有缓存！"));
        //计算当前短心跳间隔
        currentPingInterval = (cacheInterval == 0?INIT_INTERVAL:cacheInterval);
        log();
    }

    @Override
    public void stopPing() {
        currentPingInterval = -1;
    }

    @Override
    public long getNextInterval() {
        return currentPingInterval;
    }

    private String getCacheKey(){
        return Utils.getOperators() + "";
    }

    private void log(){
        String temp = (lastStableInterval == 0?"0":lastStableInterval/1000+"秒");
        String temp2 = (currentPingInterval == 0?"0":currentPingInterval/1000+"秒");
        Utils.LogDebug(TAG,"稳定态-->"+temp+"-->下一次心跳-->"+temp2);
    }
}
