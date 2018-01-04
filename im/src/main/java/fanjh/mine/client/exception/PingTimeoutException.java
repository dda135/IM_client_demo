package fanjh.mine.client.exception;


public class PingTimeoutException extends Exception{
    private static final long serialVersionUID = 4334972553882142015L;

    public PingTimeoutException(String message) {
        super(message);
    }
}
