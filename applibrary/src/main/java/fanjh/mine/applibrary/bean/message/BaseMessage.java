package fanjh.mine.applibrary.bean.message;

public class BaseMessage {
	public static final int TYPE_TEXT = 1;
	public static final int TYPE_APPLY = 2;
	public static final int TYPE_APPLY_REJECT = 3;
	public static final int TYPE_APPLY_AGREE = 4;
	public static final int TYPE_APPLY_SUCCESS = 5;
	public static final int TYPE_APPLY_AGREE_SUCCESS = 6;
	public static final int TYPE_IMAGE = 7;
	public static final int TYPE_RECORD = 8;
	public int type;
	
	public BaseMessage(int type) {
		super();
		this.type = type;
	}

}
