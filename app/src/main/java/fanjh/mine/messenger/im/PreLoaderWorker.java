package fanjh.mine.messenger.im;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import fanjh.mine.applibrary.bean.Codes;
import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.bean.friend.FriendApplyBean;
import fanjh.mine.applibrary.bean.friend.FriendBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.db.friend.FriendDBHelper;
import fanjh.mine.applibrary.network.NetworkWorker;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.messenger.BuildConfig;
import io.reactivex.functions.Consumer;

/**
 * @author fanjh
 * @date 2017/12/12 10:53
 * @description
 * @note
 **/
public class PreLoaderWorker {
    private Context context;
    private int countDown = 2;


    public PreLoaderWorker(Context context) {
        this.context = context;
    }

    public void execute(final Runnable runnable) {
        /*FriendDBHelper.getInstance().queryFriendApplyMaxID(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                loadFriendApply(integer,runnable);
            }
        });*/
        loadFriendApply(0,runnable);
        loadFriend(0,runnable);
    }

    private void loadFriendApply(int integer,final Runnable runnable){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id", UserConfig.getID());
            jsonObject.put("token", UserConfig.getToken());
            if (integer > 0) {
                jsonObject.put("min_id", integer);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new NetworkWorker.Builder().url(BuildConfig.URL_PREFIX + "friend/getFirendApply").
                content(jsonObject.toString()).
                callInMainThread(true).
                isShowDialog(true).
                build().execute(context, new Consumer<NetworkResultBean>() {
            @Override
            public void accept(NetworkResultBean networkResultBean) throws Exception {
                if (networkResultBean.status == Codes.SUCCESS) {
                    List<FriendApplyBean> list = GsonUtils.getInstance().fromJson(networkResultBean.data, new TypeToken<List<FriendApplyBean>>() {
                    }.getType());
                    FriendDBHelper.getInstance().mergeFriendApplyBeanByApplyID(list);
                } else {
                    Toast.makeText(context.getApplicationContext(), "网络异常！", Toast.LENGTH_LONG).show();
                }
                checkCountdown(runnable);
            }
        });
    }

    private void loadFriend(int integer,final Runnable runnable){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id", UserConfig.getID());
            jsonObject.put("token", UserConfig.getToken());
            if (integer > 0) {
                jsonObject.put("min_id", integer);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new NetworkWorker.Builder().url(BuildConfig.URL_PREFIX + "friend/getFriendRelationship").
                content(jsonObject.toString()).
                callInMainThread(true).
                isShowDialog(true).
                build().execute(context, new Consumer<NetworkResultBean>() {
            @Override
            public void accept(NetworkResultBean networkResultBean) throws Exception {
                if (networkResultBean.status == Codes.SUCCESS) {
                    List<FriendBean> list = GsonUtils.getInstance().fromJson(networkResultBean.data, new TypeToken<List<FriendBean>>() {
                    }.getType());
                    FriendDBHelper.getInstance().mergeFriendBeanByFriendID(list);
                } else {
                    Toast.makeText(context.getApplicationContext(), "网络异常！", Toast.LENGTH_LONG).show();
                }
                checkCountdown(runnable);
            }
        });
    }

    private void checkCountdown(Runnable runnable){
        countDown--;
        if(countDown <= 0){
            if(null != runnable){
                runnable.run();
            }
        }
    }

}

