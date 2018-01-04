package fanjh.mine.applibrary.bean.message;

import fanjh.mine.applibrary.utils.GsonUtils;

/**
* @author fanjh
* @date 2017/11/30 10:42
* @description
* @note
**/
public class CommonMessage {
	public static final int SEND_FAILURE = 3;
	public static final int SEND_SUCCESS = 1;
	public static final int SEND_UPLOADING = 2;
	public int id;
	public String message_id;
	public int sender_id;
	public String content;
	public String sender_avator;
	public String sender_name;
	public int receiver_id;
	public int send_status;
	public long time;

	private transient BaseMessage message;

	public BaseMessage getMessage() {
		if(null == message){
			BaseMessage baseMessage = GsonUtils.getInstance().fromJson(content,BaseMessage.class);
			switch (baseMessage.type){
				case BaseMessage.TYPE_TEXT:
					message = GsonUtils.getInstance().fromJson(content,TextMessage.class);
					break;
				case BaseMessage.TYPE_APPLY:
					message = GsonUtils.getInstance().fromJson(content,ApplyMessage.class);
					break;
				case BaseMessage.TYPE_APPLY_AGREE_SUCCESS:
					message = GsonUtils.getInstance().fromJson(content,ApplyAgreeSuccessMessage.class);
					break;
				case BaseMessage.TYPE_APPLY_AGREE:
					message = GsonUtils.getInstance().fromJson(content,ApplyAgreeMessage.class);
					break;
				case BaseMessage.TYPE_IMAGE:
					message = GsonUtils.getInstance().fromJson(content,ImageMessage.class);
					break;
				default:
					message = baseMessage;
					break;
			}
		}
		return message;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CommonMessage && ((CommonMessage) obj).message_id.equals(message_id);
	}

	@Override
	public int hashCode() {
		return message_id.hashCode();
	}
}
