package fanjh.mine.applibrary.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fanjh.mine.applibrary.bean.ImageEntity;
import fanjh.mine.applibrary.config.UserConfig;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * @author fanjh
 * @date 2017/9/27 15:12
 * @description 进行图片的选择和选择后处理
 * @note 主要是统一管理选择图片的操作，图片的压缩和计算
 **/
public class ImageChooser {
    public static final String DIR = "picture";
    public static final int REQUEST_CODE_GALLERY = 1001;
    public static final int REQUEST_CODE_CAMERA = 1002;
    private Activity activity;
    private OnResultListener listener;
    private String tempCameraFilePath;

    public interface OnResultListener{
        void onError();
        void onSuccess(ImageEntity entity);
    }

    public void setResultListener(OnResultListener listener) {
        this.listener = listener;
    }

    public ImageChooser(Activity activity) {
        this.activity = activity;
    }

    /**
     * 选择已有图片
     */
    public void chooseGallery() {
        try {
            if (!PermissionUntil.isHasSDPermission() || !PermissionUntil.isHasDocumentPermission(activity)) {
                listener.onError();
                return;
            }
            Intent innerIntent = new Intent();
            innerIntent.setAction(Intent.ACTION_GET_CONTENT);
            innerIntent.setType("image/*");
            innerIntent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent intent = Intent.createChooser(innerIntent, "选择图片");
            List<ResolveInfo> activities = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (activities.size() > 0) {
                activity.startActivityForResult(intent, REQUEST_CODE_GALLERY);
            }else{
                listener.onError();
            }
        } catch (Exception e) {
            listener.onError();
        }
    }

    /**
     * 照相
     */
    public void chooseCamera() {
        try {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                listener.onError();
                return;
            }
            if (!PermissionUntil.isHasCameraPermission(activity)) {
                listener.onError();
                return;
            }
            File tempCameraFile = new File(FileUtils.getExternalStorePath(activity, UserConfig.USER_MESSAGE, DIR), UUID.randomUUID() + ".jpg");
            tempCameraFilePath = tempCameraFile.getAbsolutePath();
            Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri imageUri = Uri.fromFile(tempCameraFile);//7.0之后file://不再允许，需要修改
            openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            List<ResolveInfo> activities = activity.getPackageManager().queryIntentActivities(openCameraIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (activities.size() > 0) {
                activity.startActivityForResult(openCameraIntent, REQUEST_CODE_CAMERA);
            }else{
                listener.onError();
            }
        } catch (Exception e) {
            tempCameraFilePath = null;
            listener.onError();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (Activity.RESULT_OK == resultCode) {
            switch (requestCode) {
                case REQUEST_CODE_CAMERA:
                    if (null == tempCameraFilePath) {
                        return;
                    }
                    Observable.create(new ObservableOnSubscribe<ImageEntity>() {
                        @Override
                        public void subscribe(@NonNull ObservableEmitter<ImageEntity> e) throws Exception {
                            ImageEntity entity = chooseAndUploadImageFile(tempCameraFilePath);
                            e.onNext(null == entity?new ImageEntity(null,0,0):entity);
                        }
                    }).subscribeOn(Schedulers.io()).
                            observeOn(AndroidSchedulers.mainThread()).
                            subscribe(new Consumer<ImageEntity>() {
                        @Override
                        public void accept(ImageEntity imageEntity) throws Exception {
                            callback(imageEntity);
                        }
                    });
                    break;
                case REQUEST_CODE_GALLERY:
                    if (null != data && null != data.getData()) {
                        Observable.create(new ObservableOnSubscribe<ImageEntity>() {
                            @Override
                            public void subscribe(@NonNull ObservableEmitter<ImageEntity> e) throws Exception {
                                String filePath = Pictureutil.getPath(activity, data.getData());
                                ImageEntity imageEntity = chooseAndUploadImageFile(filePath);
                                e.onNext(null == imageEntity ? new ImageEntity(null, 0, 0) : imageEntity);
                            }
                        }).subscribe(new Consumer<ImageEntity>() {
                            @Override
                            public void accept(ImageEntity imageEntity) throws Exception {
                                callback(imageEntity);
                            }
                        });
                    }else{
                        callback(null);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void callback(ImageEntity imageEntity) {
        if (null != listener) {
            if (null == imageEntity || TextUtils.isEmpty(imageEntity.getFilePath())) {
                listener.onError();
            } else {
                listener.onSuccess(imageEntity);
            }
        }
    }

    /**
     * 在选取图片返回之后，获取图片的bitmap进行压缩等处理并上传
     */
    private ImageEntity chooseAndUploadImageFile(String filePath) {
        try {
            Bitmap bitmap = compress(filePath);
            if (bitmap != null) {
                return createImage(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 计算当前的压缩比例
     */
    private int compressSize(String filePath){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath,options);

        if (options.outHeight == -1 || options.outWidth == -1) {//一种异常的处理方式
            try {
                ExifInterface exifInterface = new ExifInterface(filePath);
                options.outHeight = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的高度
                options.outWidth = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的宽度
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int degreesToRotate = getExifOrientationDegrees(filePath);
        if(degreesToRotate != 90 && degreesToRotate != 270) {

        } else {
            int temp = options.outWidth;
            options.outWidth = options.outHeight;
            options.outHeight = temp;
        }

        //目前要求图片宽高都在1024之内
        if(options.outWidth < 1024 && options.outHeight < 1024){//不需要压缩
            return 1;
        }
        int widthRadio = options.outWidth / 1024;
        int heightRadio = options.outHeight / 1024;
        int radio = Math.max(widthRadio,heightRadio);
        return Integer.highestOneBit(radio);
    }

    /**
     * 进行图片压缩
     */
    private Bitmap compress(String filePath) throws Exception{
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = compressSize(filePath);
        Bitmap bitmap = BitmapFactory.decodeFile(filePath,options);
        if(null == bitmap){
            throw new Exception("无法获得对应的Bitmap!");
        }

        bitmap = rotateBitmap(filePath,bitmap);
        //因为BitmapFactory只支持1,2,4,8这种2的倍数，所以说宽高不一定满足要求，要再进行矩阵处理
        if(bitmap.getWidth() > 1024 || bitmap.getHeight() > 1024){
            Matrix matrix = new Matrix();
            float scaleRadio = Math.min(1024f / bitmap.getWidth(),1024f / bitmap.getHeight());
            matrix.postScale(scaleRadio,scaleRadio);
            Bitmap result = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,false);
            if(result != bitmap && !bitmap.isRecycled()){//当前产生了新的bitmap，旧的应该手动回收
                bitmap.recycle();
            }
            return result;
        }
        return bitmap;
    }

    private Bitmap rotateBitmap(String filePath,Bitmap bitmap){
        Matrix matrix = new Matrix();
        boolean shouldRotated = initializeMatrixForRotation(getExifOrientation(filePath),matrix);
        if(shouldRotated) {
            Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            if (result != bitmap && !bitmap.isRecycled()) {//当前产生了新的bitmap，旧的应该手动回收
                bitmap.recycle();
            }
            return result;
        }else{
            return bitmap;
        }
    }

    /**
     * 根据当前bitmap创建回调对象
     */
    private ImageEntity createImage(Bitmap bitmap) throws Exception{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int options = 80;
        bitmap.compress(Bitmap.CompressFormat.JPEG, options, byteArrayOutputStream);
        while (byteArrayOutputStream.toByteArray().length / 1024 > 1024) {
            byteArrayOutputStream.reset();
            options -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, byteArrayOutputStream);
        }
        //获得上传图片的宽高
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] bitmapData = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        String filePath = FileUtils.getExternalStorePath(activity, UserConfig.USER_MESSAGE, DIR) + "/" + UUID.randomUUID() + ".jpg";
        File f = new File(filePath);
        f.createNewFile();
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            bos = new BufferedOutputStream(fos);
            bos.write(bitmapData);
            bos.flush();
        }finally {
            if(null != bos){
                bos.close();
            }
            if(null != fos){
                fos.close();
            }
            if(!bitmap.isRecycled()){//实际上bitmap已经没用了，手动回收
                bitmap.recycle();
            }
        }
        return new ImageEntity(filePath,width,height);
    }

    public static final int getExifOrientation(String filePath){
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getExifOrientationDegrees(String filePath) {
        int exifOrientation = -1;
        exifOrientation = getExifOrientation(filePath);
        short degreesToRotate;
        switch(exifOrientation) {
            case 3:
            case 4:
                degreesToRotate = 180;
                break;
            case 5:
            case 6:
                degreesToRotate = 90;
                break;
            case 7:
            case 8:
                degreesToRotate = 270;
                break;
            default:
                degreesToRotate = 0;
        }

        return degreesToRotate;
    }

    public static boolean initializeMatrixForRotation(int exifOrientation, Matrix matrix) {
        switch(exifOrientation) {
            case 2:
                matrix.setScale(-1.0F, 1.0F);
                return true;
            case 3:
                matrix.setRotate(180.0F);
                return true;
            case 4:
                matrix.setRotate(180.0F);
                matrix.postScale(-1.0F, 1.0F);
                return true;
            case 5:
                matrix.setRotate(90.0F);
                matrix.postScale(-1.0F, 1.0F);
                return true;
            case 6:
                matrix.setRotate(90.0F);
                return true;
            case 7:
                matrix.setRotate(-90.0F);
                matrix.postScale(-1.0F, 1.0F);
                return true;
            case 8:
                matrix.setRotate(-90.0F);
                return true;
            default:
                return false;
        }
    }

}
