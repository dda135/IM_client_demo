package fanjh.mine.applibrary.bean;

import java.io.Serializable;

/**
* @author fanjh
* @date 2017/9/30 16:39
* @description 图片信息实体
**/
public class ImageEntity implements Serializable{
    private static final long serialVersionUID = -7057091230767790010L;
    private String filePath;
    private int width;
    private int height;

    public ImageEntity(String filePath, int width, int height) {
        this.filePath = filePath;
        this.width = width;
        this.height = height;
    }

    public String getFilePath() {
        return null == filePath ? "" : filePath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
