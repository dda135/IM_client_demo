package fanjh.mine.im_sdk.core;

import java.util.List;

import fanjh.mine.im_sdk.aidl.Node;

/**
* @author fanjh
* @date 2017/11/22 9:55
* @description 对于不同的实现者来说，获取节点方式以及获取到的节点应该不同
**/
public interface NodePuller {
    /**
     * 当前方法默认在子线程中运行，为了能够直接通过网络请求来获取节点
     * @return 当前获取到的节点
     */
    List<Node> getNodes();
}
