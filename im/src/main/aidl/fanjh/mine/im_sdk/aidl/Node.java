package fanjh.mine.im_sdk.aidl;

import android.os.Parcel;
import android.os.Parcelable;

/**
* @author fanjh
* @date 2017/10/31 15:26
* @description 地址
* @note
**/
public class Node implements Parcelable{

    private String ip;
    private int port;
    private long successCount;
    private long errorCount;
    private boolean alreadySelected;

    /**
     * 心跳间隔，单位s
     * 默认为4分半
     */
    private int heartbeatInterval = (int) (60 * 4.5);

    public Node(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    protected Node(Parcel in) {
        ip = in.readString();
        port = in.readInt();
        successCount = in.readLong();
        errorCount = in.readLong();
        alreadySelected = in.readByte() != 0;
        heartbeatInterval = in.readInt();
    }

    public static final Creator<Node> CREATOR = new Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel in) {
            return new Node(in);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[size];
        }
    };

    public void establishConnection(){
        successCount++;
    }

    public void connectFailure(){
        errorCount++;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Node)){
            return false;
        }
        Node temp = (Node) obj;
        if(!ip.equals(temp.ip)){
            return false;
        }else if(port != temp.port){
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (ip == null && port == 0) {
            return 0;
        }

        int result = 1;
        result = 31 * result + (ip == null ? 0 : ip.hashCode());
        result = 31 * result +  port;

        return result;
    }

    public String getUrl(){
        return "http://"+ip+":"+port;
    }

    @Override
    public String toString() {
        return ip+"-"+port+"-"+successCount+"-"+errorCount+"-"+heartbeatInterval;
    }

    public Node(String str) {
        String []nodeString = str.split("-");
        if(nodeString.length != 5){
            throw new IllegalArgumentException("不满足转换的校验规则！要满足1-1-1-1-1...");
        }
        ip = nodeString[0];
        port = Integer.parseInt(nodeString[1]);
        successCount = Long.parseLong(nodeString[2]);
        errorCount = Long.parseLong(nodeString[3]);
        heartbeatInterval = Integer.parseInt(nodeString[4]);
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setAlreadySelected(boolean alreadySelected) {
        this.alreadySelected = alreadySelected;
    }

    public boolean isAlreadySelected() {
        return alreadySelected;
    }

    @Override
    public int describeContents() {
        return 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ip);
        dest.writeInt(port);
        dest.writeLong(successCount);
        dest.writeLong(errorCount);
        dest.writeByte((byte) (alreadySelected ? 1 : 0));
        dest.writeInt(heartbeatInterval);
    }
}
