package fanjh.mine.im_sdk.DO;

import java.io.Serializable;

/**
* @author fanjh
* @date 2017/11/1 18:13
* @description 可以订阅的主题节点
* @note
**/
public class TopicNode implements Serializable{
    private static final long serialVersionUID = -6468985924423983726L;
    public String topicName;
    public int qos;


    public TopicNode(String topicName, int qos) {
        this.topicName = topicName;
        this.qos = qos;
    }


}
