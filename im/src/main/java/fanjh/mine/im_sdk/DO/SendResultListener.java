package fanjh.mine.im_sdk.DO;

import android.os.Parcelable;

/**
 * Created by faker on 2017/11/20.
 */

public interface SendResultListener{
    void onSuccess();
    void onError(Throwable ex);
}
