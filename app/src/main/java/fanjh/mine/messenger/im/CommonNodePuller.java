package fanjh.mine.messenger.im;

import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import fanjh.mine.applibrary.bean.NetworkResultBean;
import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.encrypt.GetKey;
import fanjh.mine.applibrary.encrypt.Worker;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.im_sdk.aidl.Node;
import fanjh.mine.im_sdk.core.NodePuller;
import fanjh.mine.messenger.BuildConfig;
import fanjh.mine.applibrary.config.UserConfig;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static fanjh.mine.applibrary.network.NetworkWorker.client;

/**
* @author fanjh
* @date 2017/11/29 14:22
* @description
* @note
**/
public class CommonNodePuller implements NodePuller{
    public static class Holder{
        static CommonNodePuller INSTANCE = new CommonNodePuller();
    }

    private CommonNodePuller() {
    }

    public static CommonNodePuller getInstance(){
        return Holder.INSTANCE;
    }

    @Override
    public List<Node> getNodes() {
        int id = UserConfig.getID();
        if(id <= 0){
            return null;
        }
        JSONObject json = new JSONObject();
        try {
            json.put("user_id",id);
            json.put("token",UserConfig.getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String desKey = GetKey.getKey(8);
        MultipartBody.Builder builder = new MultipartBody.Builder().
                addFormDataPart("content", Worker.getContent(desKey,json.toString()));
        Request request = new Request.Builder().
                post(builder.build()).
                url(BuildConfig.URL_PREFIX + "node/getnode").
                build();
        try {
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (null != responseBody) {
                String cipherText = responseBody.string();
                NetworkResultBean resultBean = GsonUtils.getInstance().fromJson(cipherText, NetworkResultBean.class);
                return GsonUtils.getInstance().fromJson(Worker.getDecrypt(desKey, resultBean.data),new TypeToken<List<Node>>(){}.getType());
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }
}
