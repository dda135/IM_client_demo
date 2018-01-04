package fanjh.mine.client.strategy;

/**
* @author fajnh
* @date 2017/11/28 9:27
* @description 抽象ping策略的几个阶段
**/
public interface PingStrategy {
    /**
     * 处理ping成功
     */
    void pingSuccess();

    /**
     * 处理ping失败
     */
    void pingFailure();

    /**
     * 开始ping操作
     */
    void startPing();

    /**
     * 终止ping操作
     */
    void stopPing();

    /**
     * 获得下一次进行ping的时间间隔
     * @return 毫秒
     */
    long getNextInterval();

}
