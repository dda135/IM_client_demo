package fanjh.mine.client;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import fanjh.mine.client.exception.ConnectWaitAckTimeoutException;
import fanjh.mine.client.exception.SendTimeoutException;
import fanjh.mine.proto.MessageProtocol;

/**
* @author fanjh
* @date 2017/11/23 13:55
* @description IM连接者
* @note
**/
public class IMConnector {
    public static final int SEND_TEXT_TIMEOUT = 1000 * 30;//30秒
    public static final int SEND_FILE_TIMEOUT = 1000 * 60;//1分钟
    public static final int MSG_CHECK_SEND = 2;
    public static final int MSG_CHECK_CONNECT_ACK = 3;
    /**
     * 服务端返回连接ACK报文的最大等待时长，如果当前没有收到此报文，则终止当前连接
     */
    public static final int CONNECT_ACK_WAITING_DURATION = 5000;
    /**
     * 单线程池，用于连接操作
     */
    private ExecutorService connectService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("IMConnector_connectService");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    });
    /**
     * 单线程池，用于接收消息
     */
    private ExecutorService receiverService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("IMConnector_receiverService");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    });
    /**
     * 单线程池，用于消息发送操作
     */
    private ExecutorService sendService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(2048), new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("IMConnector_sendService");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    });
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private static final Object object = new Object();
    private static final Object sendListenerLock = new Object();
    private static final Object outputStreamLock = new Object();
    private ResultListener connectListener;
    private LongSparseArray<ResultListener> sendListener = new LongSparseArray<>();
    private ConnectionListener connectionListener;
    private AtomicBoolean connectEstablished = new AtomicBoolean(false);
    private AtomicBoolean isForeground = new AtomicBoolean(false);
    private Handler handler = new Handler(Looper.getMainLooper());
    private Handler backgroundHandler;
    private HandlerThread thread;
    private PingRunner pingRunner;

    public interface ConnectionListener{
        void onConnectionLost();
        void messageArrived(String content);
    }

    public interface ResultListener{
        void onSuccess();
        void onError(Throwable ex);
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void changeForeground(boolean is){
        isForeground.set(is);
        if(null != pingRunner){
            pingRunner.changeForeground(is);
        }
    }

    public void connect(final Address address, final int connectTimeOut, final ResultListener listener){
        if(null == address || TextUtils.isEmpty(address.host) || address.port == 0){
            return;
        }
        if(null != socket){
            return;
        }
        this.connectListener = listener;
        connectService.execute(new ConnectRunnable(address, connectTimeOut));
    }

    public void sendMessage(String content,String filePath,ResultListener listener) {
        sendMessage(content,filePath,null == filePath?SEND_TEXT_TIMEOUT:SEND_FILE_TIMEOUT,listener);
    }

    public void sendMessage(final String content,final String filePath, final int timeout,final ResultListener listener) {
        sendService.execute(new Runnable() {
            @Override
            public void run() {
                long msgID = SystemClock.uptimeMillis();
                try {
                    synchronized (sendListenerLock) {
                        sendListener.put(msgID, listener);
                    }

                    ByteArrayOutputStream byteArrayOutputStream = null;
                    FileInputStream fileInputStream = null;
                    if(null != filePath){
                        try {
                            //发送文件流
                            File file = new File(filePath);
                            fileInputStream = new FileInputStream(file);
                            byteArrayOutputStream = new ByteArrayOutputStream();
                            int num;
                            while((num = fileInputStream.read()) != -1) {
                                byteArrayOutputStream.write(num);
                            }
                            byteArrayOutputStream.flush();
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }finally {
                            Utils.close(fileInputStream);
                            Utils.close(byteArrayOutputStream);
                        }
                    }

                    long contentLength = -1;
                    byte[] fileBytes = null;
                    if(null != byteArrayOutputStream){
                        fileBytes = byteArrayOutputStream.toByteArray();
                        contentLength = fileBytes.length;
                    }

                    MessageProtocol.MessageProto.Builder builder = MessageProtocol.MessageProto.newBuilder();
                    builder.setId(msgID);
                    builder.setContent(content);
                    builder.setType(ProtoType.SEND_REQ);
                    if(contentLength != -1){
                        builder.setContentLength(contentLength);
                    }
                    MessageProtocol.MessageProto messageProto = builder.build();
                    byte[] protos = messageProto.toByteArray();

                    Utils.Log("发送"+(protos.length+4)+"个字节的数据");
                    synchronized (outputStreamLock) {
                        dataOutputStream.writeInt(protos.length);
                        dataOutputStream.write(protos);
                        if(null != fileBytes) {
                            dataOutputStream.write(fileBytes);
                        }
                        dataOutputStream.flush();
                    }

                    if(timeout != -1) {
                        Message message = Message.obtain();
                        message.what = MSG_CHECK_SEND;
                        message.obj = msgID;
                        backgroundHandler.sendMessageDelayed(message, timeout);
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                    checkConnectWhenSendFailure();
                    synchronized (sendListenerLock) {
                        ResultListener temp = sendListener.get(msgID);
                        sendListener.delete(msgID);
                        callListenerError(temp,ex);
                    }
                }
            }
        });
    }

    Socket getSocket(){
        return socket;
    }

    boolean sendInnerMessage(@ProtoType.ProtoTypes int type, String content){
        return sendInnerMessage(0,type,content);
    }

    boolean sendInnerMessage(long protoID,@ProtoType.ProtoTypes int type, String content){
        try {
            MessageProtocol.MessageProto.Builder builder = MessageProtocol.MessageProto.newBuilder();
            if(protoID != 0){
                builder.setId(protoID);
            }
            builder.setContent(content);
            builder.setType(type);
            MessageProtocol.MessageProto messageProto = builder.build();
            byte[] protos = messageProto.toByteArray();
            Utils.Log("发送"+(protos.length+4)+"个字节的数据");
            synchronized (outputStreamLock) {
                dataOutputStream.writeInt(protos.length);
                dataOutputStream.write(protos);
                dataOutputStream.flush();
            }
            return true;
        }catch (Exception ex){
            ex.printStackTrace();
            checkConnectWhenSendFailure();
            return false;
        }
    }

    private class ConnectRunnable implements Runnable{
        private Address address;
        private int connectTimeOut;

        public ConnectRunnable(Address address, int connectTimeOut) {
            this.address = address;
            this.connectTimeOut = connectTimeOut;
        }

        @Override
        public void run() {
            try {
                socket = new Socket();
                InetSocketAddress inetSocketAddress = new InetSocketAddress(address.host,address.port);
                socket.setKeepAlive(true);
                socket.setOOBInline(false);
                socket.setTcpNoDelay(true);
                socket.connect(inetSocketAddress,connectTimeOut);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                receiverService.execute(new ReceiverRunnable(address));
                synchronized (object) {
                    object.wait();
                    initTimeoutCheckHandler();
                    sendInnerMessage(ProtoType.CONNECT_REQ, null == address.clientID?"":address.clientID);
                    backgroundHandler.sendEmptyMessageDelayed(MSG_CHECK_CONNECT_ACK,CONNECT_ACK_WAITING_DURATION);
                }
            } catch (Exception e) {
                e.printStackTrace();
                disconnect();
                callListenerError(connectListener,e);
            }
        }
    }

    private class ReceiverRunnable implements Runnable{
        private Address address;

        public ReceiverRunnable(Address address) {
            this.address = address;
        }

        @Override
        public void run() {
            try {
                synchronized (object) {
                    object.notifyAll();
                }
                while (true) {

                    int len = dataInputStream.readInt();
                    byte[] data = new byte[len];
                    dataInputStream.readFully(data, 0, len);

                    final MessageProtocol.MessageProto messageProto = MessageProtocol.MessageProto.
                            parseFrom(data);
                    System.out.println(messageProto.getType() + "-->" + messageProto.getContent());
                    int type = messageProto.getType();
                    switch (type){
                        case ProtoType.CONNECT_ACK:
                            Utils.Log("连接服务器成功！");
                            connectEstablished.set(true);
                            backgroundHandler.removeMessages(MSG_CHECK_CONNECT_ACK);
                            callListenerSuccess(connectListener);
                            ping();
                            break;
                        case ProtoType.PING_ACK:
                            pingRunner.receiverACK(messageProto);
                            break;
                        case ProtoType.MESSAGE:
                            sendInnerMessage(messageProto.getId(),ProtoType.MESSAGE_ACK,address.clientID);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(null != connectionListener){
                                        connectionListener.messageArrived(messageProto.getContent());
                                    }
                                }
                            });
                            Utils.Log("收到一条服务器发送过来的消息！");
                            break;
                        case ProtoType.SEND_ACK:
                            synchronized (sendListenerLock) {
                                ResultListener listener = sendListener.get(messageProto.getId());
                                sendListener.delete(messageProto.getId());
                                callListenerSuccess(listener);
                            }
                            Utils.Log("消息成功发送到服务器！");
                            break;
                        default:
                            break;
                    }
                }
            }catch (Exception ex){
                ex.printStackTrace();
                Utils.Log("接收消息中出现异常，关闭连接！");
                callDisconnect(ex);
            }
        }

    }

    public void callAllSendError(Throwable ex){
        for(int i = 0;i < sendListener.size();++i){
            sendListener.get(sendListener.keyAt(i)).onError(ex);
        }
        sendListener.clear();
    }

    public void disconnect(){
        Utils.close(dataInputStream);
        Utils.close(dataOutputStream);
        Utils.close(socket);
        if(null != pingRunner){
            pingRunner.destroy();
        }
        socket = null;
        dataInputStream = null;
        dataOutputStream = null;
        connectEstablished.set(false);
    }

    public boolean isConnected(){
        return null != socket && socket.isConnected();
    }

    void callDisconnect(final Exception ex){
        if(!pingRunner.isPingSuccess()){
            Utils.Log("ping失败！");
        }
        disconnect();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(null != connectionListener){
                    connectionListener.onConnectionLost();
                }
                callAllSendError(ex);
            }
        });
    }

    private boolean checkConnectWhenSendFailure(){
        try {
            socket.sendUrgentData(0xFF);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Utils.Log("数据发送失败，且校验不通过，关闭连接！");
            callDisconnect(e);
            return false;
        }
    }

    public void ping(){
        if(null == pingRunner){
            pingRunner = new PingRunner(this);
        }
        pingRunner.run(isForeground.get());
    }

    private void initTimeoutCheckHandler(){
        thread = new HandlerThread("超时校验线程");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case MSG_CHECK_SEND:
                        long msgID = (long) msg.obj;
                        synchronized (sendListenerLock) {
                            ResultListener listener = sendListener.get(msgID);
                            if (null != listener) {
                                sendListener.remove(msgID);
                                Utils.Log("消息发送超时！");
                                callListenerError(listener,new SendTimeoutException("消息发送超时！"));
                            }
                        }
                        break;
                    case MSG_CHECK_CONNECT_ACK:
                        if(!connectEstablished.get()){
                            Utils.Log("等待服务端返回连接ACK超时！");
                            disconnect();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(null != connectListener){
                                        connectListener.onError(new ConnectWaitAckTimeoutException("等待服务端返回连接ACK超时！"));
                                    }
                                }
                            });
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void callListenerError(final ResultListener listener,final Exception ex){
        if(null == listener || null == ex){
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onError(ex);
            }
        });
    }

    private void callListenerSuccess(final ResultListener listener){
        if(null == listener){
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onSuccess();
            }
        });
    }

}
