package fanjh.mine.client;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
* @author fanjh
* @date 2017/11/23 14:06
**/
public class ProtoType {
    public static final int NONE = 0;
    public static final int PING_REQ = 1;
    public static final int PING_ACK = 2;
    public static final int CONNECT_REQ = 3;
    public static final int CONNECT_ACK = 4;
    public static final int SEND_REQ = 5;
    public static final int SEND_ACK = 6;
    public static final int MESSAGE = 7;
    public static final int MESSAGE_ACK = 8;

    @IntDef({NONE, PING_REQ, PING_ACK,CONNECT_REQ,CONNECT_ACK,SEND_REQ,SEND_ACK,MESSAGE,MESSAGE_ACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProtoTypes {}

}
