package fanjh.mine.applibrary.bean.message;

public class ApplyAgreeMessage extends BaseMessage{
	public int serverID;

	public ApplyAgreeMessage() {
		super(BaseMessage.TYPE_APPLY_AGREE);
	}

}
