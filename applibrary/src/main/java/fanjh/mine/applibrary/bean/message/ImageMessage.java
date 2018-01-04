package fanjh.mine.applibrary.bean.message;

/**
* @author fanjh
* @date 2017/12/13 9:46
* @description
* @note
**/
public class ImageMessage extends BaseMessage{
    public int width;
    public int height;
    public String fileName;
    public String imageUrl;

    public ImageMessage() {
        super(BaseMessage.TYPE_IMAGE);
    }
}
