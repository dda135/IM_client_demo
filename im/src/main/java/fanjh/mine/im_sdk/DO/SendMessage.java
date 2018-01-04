package fanjh.mine.im_sdk.DO;

import java.io.Serializable;

/**
* @author fanjh
* @date 2017/11/22 11:34
* @description 发送消息的基类
**/
public class SendMessage implements Serializable{
    private static final long serialVersionUID = 5657428432432097934L;
    public String content;
    public String filePath;
    public int retryCount;

}
