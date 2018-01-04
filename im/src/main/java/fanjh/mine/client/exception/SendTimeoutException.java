package fanjh.mine.client.exception;


public class SendTimeoutException extends Exception{
    private static final long serialVersionUID = 2190560978062018548L;

    public SendTimeoutException(String message) {
        super(message);
    }
}
