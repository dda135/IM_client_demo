package fanjh.mine.client.strategy;

import android.content.Context;
import android.content.SharedPreferences;

import fanjh.mine.client.Utils;
import fanjh.mine.im_sdk.InstantMessengerClient;

/**
* @author fanjh
* @date 2017/11/28 9:18
* @description 前台状态下的心跳策略
* @note 采用短连接多检测的方式去测试NAT超时时间，同时为了保证前台活跃度，最大间隔不宜太大
**/
public class ForegroundStrategy implements PingStrategy{
    public static final String TAG = "ForegroundStrategy";
    /**
     * 初始间隔
     */
    public static final long INIT_INTERVAL = 1000 * 60 * 3;
    /**
     * 最大间隔
     */
    public static final long MAX_INTERVAL = 1000 * 60 * 6;
    /**
     * 达到稳定态的校验次数
     */
    public static final int STABLE_CHECK_COUNT = 3;
    /**
     * 步长为1分钟
     */
    public static final long STEP = 1000 * 60;
    public static final String CACHE_FILE = "ForegroundStrategy";
    private SharedPreferences sp;
    private long currentPingInterval;
    private long checkPingInterval;
    private long lastStableInterval;
    /**
     * -1表示稳定态
     */
    private int checkCount;

    public ForegroundStrategy() {
        sp = InstantMessengerClient.getApplicationContext().getSharedPreferences(CACHE_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public void pingSuccess() {
        checkCount--;
        if(checkCount == 0){
            //当前期望间隔校验成功
            //存储
            Utils.LogDebug(TAG,"当前到达稳定态！");
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong(getCacheKey(),checkPingInterval);
            editor.apply();
            lastStableInterval = checkPingInterval;
            currentPingInterval = checkPingInterval;
            if(checkPingInterval + STEP > MAX_INTERVAL){
                //当前已经达到最大值，不应该继续校验，保持最大值的稳定态即可
                Utils.LogDebug(TAG,"当前到达稳定态,并且已经为最大值！");
                checkCount = -1;
            }else{
                //进行下一个步长的校验
                Utils.LogDebug(TAG,"当前到达稳定态,进行下一个步长的校验！");
                checkPingInterval += STEP;
                checkCount = STABLE_CHECK_COUNT;
                currentPingInterval = checkPingInterval / checkCount;
            }
        }else if(checkCount > 0){
            Utils.LogDebug(TAG,"当前校验中！");
            //当前处于校验中，继续校验
            //不需要任何操作，保持interval即可
        }
        log();
    }

    @Override
    public void pingFailure() {
        if(lastStableInterval > 0){
            //当次心跳过程中已经测出存在稳定态的心跳间隔
            //使用稳定态间隔进行心跳
            Utils.LogDebug(TAG,"心跳失败，使用之前已经测量的稳定态进行心跳！");
            checkCount = -1;
            currentPingInterval = lastStableInterval;
            checkPingInterval = -1;
        }else{
            Utils.LogDebug(TAG,"心跳失败，之前没有测量出的稳定态，向下校验！");
            //向下检测
            checkPingInterval -= STEP;
            checkCount = STABLE_CHECK_COUNT;
            currentPingInterval = checkPingInterval / checkCount;
        }
        log();
    }

    @Override
    public void startPing() {
        long cacheInterval = sp.getLong(getCacheKey(),0);
        Utils.LogDebug(TAG,"开始心跳！"+(cacheInterval==0?"当前没有缓存！":"当前有缓存！"));
        //开始检测，标记当前检测的间隔
        checkPingInterval = (cacheInterval == 0?INIT_INTERVAL:cacheInterval);
        //计算当前短心跳间隔
        currentPingInterval = checkPingInterval / STABLE_CHECK_COUNT;
        //标记检测开始
        checkCount = STABLE_CHECK_COUNT;
        log();
    }

    @Override
    public void stopPing() {
        checkCount = -1;
        currentPingInterval = -1;
        checkPingInterval = -1;
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
        String temp1 = (checkPingInterval == 0?"0":checkPingInterval/1000+"秒");
        String temp2 = (currentPingInterval == 0?"0":currentPingInterval/1000+"秒");
        Utils.LogDebug(TAG,"稳定态-->"+temp+"-->校验态-->"+temp1+"-->下一次心跳-->"+temp2+"-->剩余校验次数-->"+checkCount);
    }

}
