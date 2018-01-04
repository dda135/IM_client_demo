package fanjh.mine.im_sdk.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.im_sdk.aidl.Node;
import fanjh.mine.im_sdk.tools.Logger;

/**
 * @author fanjh
 * @date 2017/10/31 15:18
 * @description 地址提供者
 * @note
 **/
public class NodeManager {
    public static final String SP_FILE_NAME = "node";
    public static final String KEY_NODE_LIST = "node_list";
    public static final String NODE_STRING_SPLIT = ",";
    private CopyOnWriteArrayList<Node> nodeList;

    public NodeManager() {
        Logger.LogDebug("getNodeFromCache-->start");
        SharedPreferences sp = InstantMessengerClient.getApplicationContext().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        String temp = sp.getString(KEY_NODE_LIST, null);
        if(null != temp){
            nodeList = getNodesFromString(temp);
        }
        boolean shouldGetNode = (null == nodeList);
        if (shouldGetNode) {
            Logger.LogDebug("getNodeFromCache-->empty");
        }else{
            Logger.LogDebug("getNodeFromCache-->success,already has node");
        }
        tryGetNode();
    }

    public void serializable() {
        if (null == nodeList) {
            return;
        }
        SharedPreferences sp = getSP();
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_NODE_LIST, getNodesString());
        editor.apply();
    }

    private SharedPreferences getSP() {
        return InstantMessengerClient.getApplicationContext().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void addNode(Node node) {
        if (null == node) {
            return;
        }
        if (null == nodeList) {
            nodeList = new CopyOnWriteArrayList<>();
        }
        int index = nodeList.indexOf(node);
        if (index != -1) {
            nodeList.get(index).setAlreadySelected(false);
            return;
        }
        nodeList.add(node);
    }

    public Node getAvailableNode() {
        if (null == nodeList) {
            nodeList = new CopyOnWriteArrayList<>();
        }
        Node node = null;
        if(nodeList.size() == 0){
            Logger.LogDebug("nodeList-->empty");
            tryGetNode();
        }else{
            boolean shouldGetNode = true;
            int minErrorIndex = 0;
            long minCount = Long.MAX_VALUE;
            for(int i = 0;i < nodeList.size();++i){
                node = nodeList.get(i);
                long errorCount = node.getErrorCount();
                if(errorCount < minCount){
                    minCount = errorCount;
                    minErrorIndex = i;
                }
                if(!node.isAlreadySelected()){
                    shouldGetNode = false;
                }
            }
            node = nodeList.get(minErrorIndex);
            node.setAlreadySelected(true);
            if(shouldGetNode){
                Logger.LogDebug("getAvailableNode-->nodelist already use,should update");
                tryGetNode();
            }
            Logger.LogDebug("getAvailableNode-->ok-->node: " + node.toString());
        }
        return node;
    }

    private void tryGetNode() {
        Logger.LogDebug("try get node");
        Intent intent = new Intent(NodeLoader.ACTION_GET_NODE);
        InstantMessengerClient.getApplicationContext().sendBroadcast(intent);
    }

    private String getNodesString() {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        int size = nodeList.size();
        for (Node node : nodeList) {
            i++;
            stringBuilder.append(node.toString());
            if(i < size){
                stringBuilder.append(NODE_STRING_SPLIT);
            }
        }
        return stringBuilder.toString();
    }

    private CopyOnWriteArrayList<Node> getNodesFromString(String temp) {
        CopyOnWriteArrayList<Node> queue = new CopyOnWriteArrayList<>();
        String []nodeStrings = temp.split(NODE_STRING_SPLIT);
        for(String nodeString:nodeStrings){
            queue.add(new Node(nodeString));
        }
        return queue;
    }

    public void establishConnection(Node node,boolean shouldSerializable){
        node.establishConnection();
        if(shouldSerializable) {
            Logger.LogDebug("serializable nodelist after connect success");
            serializable();
        }
    }

    public void connectFailure(Node node,boolean shouldSerializable){
        node.connectFailure();
        if(shouldSerializable) {
            Logger.LogDebug("serializable nodelist after connect failure");
            serializable();
        }
    }

    public void changeNode(List<Node> nodes){
        if(null == nodes){
            return;
        }
        for(Node node:nodes){
            addNode(node);
        }
        serializable();
        InstantMessengerClient.getInstance().tryConnect();
    }

}
