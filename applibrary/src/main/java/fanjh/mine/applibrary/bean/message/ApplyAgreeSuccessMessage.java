package fanjh.mine.applibrary.bean.message;

public class ApplyAgreeSuccessMessage extends BaseMessage{
	public int applyID;
	public int confirmID;
	public int serverID;

	public ApplyAgreeSuccessMessage() {
		super(BaseMessage.TYPE_APPLY_AGREE_SUCCESS);
	}

}
