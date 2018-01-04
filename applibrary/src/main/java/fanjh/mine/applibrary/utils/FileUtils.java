package fanjh.mine.applibrary.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
* @author fanjh
* @date 2017/11/29 10:19
* @description
* @note
**/
public class FileUtils {

    public static String getFileName(String filePath){
        int index = filePath.indexOf(File.separator);
        if(index != -1){
            return filePath.substring(index);
        }
        return null;
    }

    /**
     * 获得缓存目录，注意该目录下的数据会伴随App卸载而被删除
     */
    public static File getCacheDir(Context context,String dirName){
        if(null == context){
            return null;
        }
        File file = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            File temp = context.getExternalCacheDir();
            if(null != temp) {
                file = new File(temp.getAbsolutePath() + File.separator + dirName);
            }
        }
        if(null == file){
            file = new File(context.getCacheDir() + File.separator + dirName);
        }
        if(!file.exists()){
            file.mkdirs();
        }
        return file;
    }

    public static File getExternalStorePath(Context context,String dirName,String secondDirName){
        File dir = getCacheDir(context, dirName);
        dir.mkdirs();
        File path = new File(dir,secondDirName);
        path.mkdirs();
        return path;
    }

}
