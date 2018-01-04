package fanjh.mine.applibrary.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.PermissionChecker;


public class PermissionUntil {


    public static boolean isHasSDPermission(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static boolean isHasRecordPermission(Context context){
        return PermissionChecker.checkCallingOrSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isHasCameraPermission(Context context){
        return PermissionChecker.checkCallingOrSelfPermission(context,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isHasDocumentPermission(Context context){
        return PermissionChecker.checkCallingOrSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }


}
