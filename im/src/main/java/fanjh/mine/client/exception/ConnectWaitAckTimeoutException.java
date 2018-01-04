package fanjh.mine.client.exception;


public class ConnectWaitAckTimeoutException extends Exception{
    private static final long serialVersionUID = -5926620070279669652L;

    public ConnectWaitAckTimeoutException(String message) {
        super(message);
    }
}
