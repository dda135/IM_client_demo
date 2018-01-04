package fanjh.mine.applibrary.network;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.MainThread;

import com.google.gson.Gson;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fanjh.mine.applibrary.R;
import fanjh.mine.applibrary.bean.Codes;
import fanjh.mine.applibrary.encrypt.GetKey;
import fanjh.mine.applibrary.encrypt.Worker;
import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.utils.FileUtils;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.applibrary.utils.ThreadUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author fanjh
 * @date 2017/11/29 10:12
 * @description
 * @note
 **/
public class NetworkWorker {
    public static final int CONNECT_TIMEOUT = 60;
    public static final String OCTET_MEDIA_TYPE = "application/octet-stream";
    public static OkHttpClient client;
    private String url;
    private String content;
    private Map<String, String> files;
    private boolean isShowDialog;
    private String tag;
    private LoadingDialog ld;
    private boolean callInMainThread;

    static {
        client = new OkHttpClient.Builder().
                connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS).
                followRedirects(false).
                followSslRedirects(false).
                retryOnConnectionFailure(false).
                build();
    }

    private NetworkWorker(Builder builder) {
        url = builder.url;
        content = builder.content;
        files = builder.files;
        isShowDialog = builder.isShowDialog;
        tag = builder.tag;
        callInMainThread = builder.callInMainThread;
    }


    @MainThread
    public void execute(final Context context, final Consumer<NetworkResultBean> consumer) {
        if (!ThreadUtils.isMainThread()) {
            throw new IllegalArgumentException("必须在UI线程使用！");
        }
        if (isShowDialog && context instanceof Activity) {
            ld = new LoadingDialog(context);
            ld.setInterceptBack(true)
                    .setLoadSpeed(LoadingDialog.Speed.SPEED_TWO)
                    .show();
        }
        Observable.create(new ObservableOnSubscribe<NetworkResultBean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<NetworkResultBean> e) {
                String desKey = GetKey.getKey(8);
                MultipartBody.Builder builder = new MultipartBody.Builder().
                        addFormDataPart("content", Worker.getContent(desKey, content));
                if (null != files) {
                    for (Map.Entry<String, String> entry : files.entrySet()) {
                        String fileKey = entry.getKey();
                        String filePath = entry.getValue();
                        String fileName = FileUtils.getFileName(filePath);
                        if (null == fileName) {
                            return;
                        }
                        File file = new File(filePath);
                        if (!file.exists()) {
                            return;
                        }
                        builder.addFormDataPart(fileKey, fileName, RequestBody.create(MediaType.parse(OCTET_MEDIA_TYPE), file));
                    }
                }
                Request request = new Request.Builder().
                        post(builder.build()).
                        url(url).
                        tag(tag).
                        build();
                try {
                    Response response = client.newCall(request).execute();
                    ResponseBody responseBody = response.body();
                    if (null == responseBody) {
                        NetworkResultBean bean = new NetworkResultBean();
                        bean.status = Codes.CLIENT_ERROR;
                        bean.hint = context.getString(R.string.network_error);
                        e.onNext(bean);
                    } else {
                        String cipherText = responseBody.string();
                        NetworkResultBean resultBean = GsonUtils.getInstance().fromJson(cipherText, NetworkResultBean.class);
                        resultBean.data = Worker.getDecrypt(desKey, resultBean.data);
                        e.onNext(resultBean);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    NetworkResultBean bean = new NetworkResultBean();
                    bean.status = Codes.CLIENT_ERROR;
                    bean.hint = context.getString(R.string.network_error);
                    e.onNext(bean);
                } finally {
                    e.onComplete();
                }
            }
        }).subscribeOn(Schedulers.computation()).
                observeOn(callInMainThread ? AndroidSchedulers.mainThread() : Schedulers.io()).
                subscribe(new Consumer<NetworkResultBean>() {
                    @Override
                    public void accept(final NetworkResultBean networkResultBean) throws Exception {
                        Observable.just(true).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean result) throws Exception {
                                if (isShowDialog && null != ld && context instanceof Activity) {
                                    if (((Activity) context).isFinishing()) {
                                        return;
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                        if (((Activity) context).isDestroyed()) {
                                            return;
                                        }
                                    }
                                    if (networkResultBean.status == 1) {
                                        ld.loadSuccess();
                                    } else {
                                        ld.loadFailed();
                                    }
                                }
                            }
                        });
                        consumer.accept(networkResultBean);
                    }
                });
    }


    public static final class Builder {
        private String url;
        private String content;
        private Map<String, String> files;
        private boolean isShowDialog;
        private String tag;
        private boolean callInMainThread;

        public Builder() {
        }

        public Builder url(String val) {
            url = val;
            return this;
        }

        public Builder content(String val) {
            content = val;
            return this;
        }

        public Builder files(Map<String, String> val) {
            files = val;
            return this;
        }

        public Builder isShowDialog(boolean val) {
            isShowDialog = val;
            return this;
        }

        public Builder tag(String val) {
            tag = val;
            return this;
        }

        public Builder callInMainThread(boolean val) {
            callInMainThread = val;
            return this;
        }

        public NetworkWorker build() {
            return new NetworkWorker(this);
        }
    }
}
