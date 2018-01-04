// IServiceAidlInterface.aidl
package fanjh.mine.im_sdk.aidl;

import fanjh.mine.im_sdk.aidl.Node;
// Declare any non-default types here with import statements

interface IServiceAidlInterface {
    void sendTextMessage(String content,long listenerID);
    void sendFileMessage(String content,String filePath,long listenerID);
    void refreshNode(in List<Node> nodes);
}
