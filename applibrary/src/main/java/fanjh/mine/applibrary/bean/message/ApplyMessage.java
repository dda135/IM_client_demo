package fanjh.mine.applibrary.bean.message;

public class ApplyMessage extends BaseMessage {
	public String text;
	public int serverID;

	public ApplyMessage() {
		super(TYPE_APPLY);
	}

}
