package fanjh.mine.messenger.conversation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.Gson;
import com.hwangjr.rxbus.RxBus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseActivity;
import fanjh.mine.applibrary.bean.ImageEntity;
import fanjh.mine.applibrary.bean.message.BaseMessage;
import fanjh.mine.applibrary.bean.message.CommonMessage;
import fanjh.mine.applibrary.bean.ConversationListBean;
import fanjh.mine.applibrary.bean.message.ImageMessage;
import fanjh.mine.applibrary.bean.message.TextMessage;
import fanjh.mine.applibrary.bean.UserBean;
import fanjh.mine.applibrary.bean.rxbus.ConversationListRefreshBean;
import fanjh.mine.applibrary.config.UserConfig;
import fanjh.mine.applibrary.db.conversation.ConversationListDBHelper;
import fanjh.mine.applibrary.db.conversation.ConversationMessageDBHelper;
import fanjh.mine.applibrary.utils.GsonUtils;
import fanjh.mine.applibrary.utils.ImageChooser;
import fanjh.mine.client.IMConnector;
import fanjh.mine.im_sdk.InstantMessengerClient;
import fanjh.mine.messenger.R;
import fanjh.mine.messenger.conversation.adapter.ConversationAdapter;
import fanjh.mine.messenger.im.CommonReceiver;
import fanjh.mine.messenger.im.MessageArrivedListener;
import fanjh.mine.messenger.im.SendMessageResultListener;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author fanjh
 * @date 2017/11/30 9:23
 * @description
 * @note
 **/
public class ConversationActivity extends BaseActivity {
    public static final String EXTRA_RECEIVER_ID = "rid";
    public static final String EXTRA_RECEIVER_NICKNAME = "rname";
    public static final String EXTRA_RECEIVER_AVATOR = "ravator";

    @BindView(R.id.iv_back)
    SimpleDraweeView ivBack;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.iv_right)
    SimpleDraweeView ivRight;
    @BindView(R.id.rv_content)
    RecyclerView rvContent;
    @BindView(R.id.iv_record)
    SimpleDraweeView ivRecord;
    @BindView(R.id.et_input)
    EditText etInput;
    @BindView(R.id.iv_appendix)
    SimpleDraweeView ivAppendix;
    @BindView(R.id.ll_input_component)
    LinearLayout llInputComponent;
    @BindView(R.id.btn_send)
    Button btnSend;

    private ConversationAdapter adapter;
    private List<CommonMessage> messages = new ArrayList<>();
    private MessageArrivedListener messageArrivedListener = new MessageArrivedListener() {
        @Override
        public void onMessageArrived(final CommonMessage message) {
            if(UserConfig.getID() != message.receiver_id){
                return;
            }
            int type = message.getMessage().type;
            if(type != BaseMessage.TYPE_IMAGE && type != BaseMessage.TYPE_TEXT){
                return;
            }
            Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                    if (messages.contains(message)) {
                        return;
                    }
                    messages.add(message);
                    e.onNext(messages.size() - 1);
                    e.onComplete();
                }
            }).subscribeOn(Schedulers.single()).
                    observeOn(AndroidSchedulers.mainThread()).
                    subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            adapter.notifyDataSetChanged();
                            rvContent.scrollToPosition(messages.size() - 1);
                        }
                    });
        }
    };

    public int receiverID;
    public String receiverAvator;
    public String receiverNickname;

    private ImageChooser imageChooser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        ButterKnife.bind(this);
        receiverID = getIntent().getIntExtra(EXTRA_RECEIVER_ID, 0);
        receiverAvator = getIntent().getStringExtra(EXTRA_RECEIVER_AVATOR);
        receiverNickname = getIntent().getStringExtra(EXTRA_RECEIVER_NICKNAME);
        tvTitle.setText(receiverNickname);
        rvContent.setLayoutManager(new LinearLayoutManager(context));
        adapter = new ConversationAdapter(messages);
        rvContent.setAdapter(adapter);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                btnSend.setVisibility(s.length() > 0?View.VISIBLE:View.GONE);
                ivAppendix.setVisibility(s.length() >0?View.GONE:View.VISIBLE);
            }
        });
        CommonReceiver.getInstance().registerArrivedListener(messageArrivedListener);
        ConversationMessageDBHelper.getInstance().queryAll(receiverID,new Consumer<List<CommonMessage>>() {
            @Override
            public void accept(List<CommonMessage> commonMessages) throws Exception {
                messages.clear();
                messages.addAll(commonMessages);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommonReceiver.getInstance().unregisterArrivedListener(messageArrivedListener);
    }

    @OnClick({R.id.iv_back, R.id.tv_title, R.id.iv_record, R.id.et_input, R.id.iv_appendix})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.tv_title:
                rvContent.smoothScrollToPosition(rvContent.getAdapter().getItemCount() - 1);
                break;
            case R.id.iv_record:
                break;
            case R.id.iv_appendix:
                sendImage();
                break;
            default:
                break;
        }
    }

    private void sendImage(){
        if(null == imageChooser){
            imageChooser = new ImageChooser(this);
            imageChooser.setResultListener(new ImageChooser.OnResultListener() {
                @Override
                public void onError() {
                    Toast.makeText(getApplicationContext(),"上传失败！",Toast.LENGTH_LONG).show();
                }

                @Override
                public void onSuccess(ImageEntity entity) {
                    final CommonMessage commonMessage = new CommonMessage();
                    commonMessage.sender_id = UserConfig.getID();
                    commonMessage.receiver_id = receiverID;
                    commonMessage.sender_avator = UserConfig.getPortrait();
                    commonMessage.sender_name = UserConfig.getNickname();
                    commonMessage.message_id = UUID.randomUUID().toString();

                    String filePath = entity.getFilePath();
                    ImageMessage imageMessage = new ImageMessage();
                    imageMessage.width = entity.getWidth();
                    imageMessage.height = entity.getHeight();
                    imageMessage.fileName = filePath.substring(filePath.lastIndexOf('/'));
                    imageMessage.imageUrl = "file://" + filePath;
                    commonMessage.content = GsonUtils.getInstance().toJson(imageMessage);

                    messages.add(commonMessage);

                    adapter.notifyDataSetChanged();
                    rvContent.scrollToPosition(messages.size() - 1);

                    ConversationListRefreshBean refreshBean = new ConversationListRefreshBean();
                    ConversationListBean conversationListBean = new ConversationListBean();
                    conversationListBean.userID = receiverID;
                    conversationListBean.avator = receiverAvator;
                    conversationListBean.nickname = receiverNickname;
                    BaseMessage baseMessage = commonMessage.getMessage();
                    if(baseMessage instanceof TextMessage){
                        conversationListBean.message = ((TextMessage)baseMessage).text;
                    }
                    conversationListBean.time = commonMessage.time;
                    refreshBean.conversationListBean = conversationListBean;
                    ConversationListDBHelper.getInstance().updateConversationList(refreshBean.conversationListBean,false);
                    RxBus.get().post(refreshBean);

                    ConversationMessageDBHelper.getInstance().insertMessage(commonMessage);

                    InstantMessengerClient.getInstance().sendFileMessage(GsonUtils.getInstance().toJson(commonMessage), filePath, new IMConnector.ResultListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(),"发送成功！",Toast.LENGTH_LONG).show();
                            commonMessage.send_status = CommonMessage.SEND_SUCCESS;
                            if(null != adapter && !isFinishing()) {
                                adapter.notifyDataSetChanged();
                            }
                            ConversationMessageDBHelper.getInstance().updateMessageStatus(commonMessage);
                        }

                        @Override
                        public void onError(Throwable ex) {
                            Toast.makeText(getApplicationContext(),"发送失败！",Toast.LENGTH_LONG).show();
                            commonMessage.send_status = CommonMessage.SEND_SUCCESS;
                            if(null != adapter && !isFinishing()) {
                                adapter.notifyDataSetChanged();
                            }
                            ConversationMessageDBHelper.getInstance().updateMessageStatus(commonMessage);
                        }
                    });
                }
            });
        }
        imageChooser.chooseGallery();
    }

    public static void start(Context context, int receiverID, String recevierAvator, String receiverNickname) {
        if(receiverID == 0){
            return;
        }
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra(EXTRA_RECEIVER_ID, receiverID);
        intent.putExtra(EXTRA_RECEIVER_AVATOR, recevierAvator);
        intent.putExtra(EXTRA_RECEIVER_NICKNAME, receiverNickname);
        context.startActivity(intent);
    }

    @OnClick(R.id.btn_send)
    public void onClick() {
        String text = etInput.getText().toString();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(getApplicationContext(), "请输入消息内容！", Toast.LENGTH_LONG).show();
        } else {
            final CommonMessage message = new CommonMessage();
            message.message_id = UUID.randomUUID().toString();
            message.receiver_id = receiverID;
            message.sender_avator = UserConfig.getPortrait();
            message.sender_id = UserConfig.getID();
            message.sender_name = UserConfig.getNickname();
            message.send_status = CommonMessage.SEND_UPLOADING;
            message.time = System.currentTimeMillis();
            TextMessage textMessage = new TextMessage();
            textMessage.text = text;
            message.content = GsonUtils.getInstance().toJson(textMessage);

            messages.add(message);

            adapter.notifyDataSetChanged();
            rvContent.scrollToPosition(messages.size() - 1);

            InstantMessengerClient.getInstance().sendTextMessage(GsonUtils.getInstance().toJson(message), new IMConnector.ResultListener() {
                @Override
                public void onSuccess() {
                    message.send_status = CommonMessage.SEND_SUCCESS;
                    if(null != adapter && !isFinishing()) {
                        adapter.notifyDataSetChanged();
                    }
                    ConversationMessageDBHelper.getInstance().updateMessageStatus(message);
                }

                @Override
                public void onError(Throwable ex) {
                    message.send_status = CommonMessage.SEND_FAILURE;
                    if(null != adapter && !isFinishing()) {
                        adapter.notifyDataSetChanged();
                    }
                    ConversationMessageDBHelper.getInstance().updateMessageStatus(message);
                }
            });

            ConversationListRefreshBean refreshBean = new ConversationListRefreshBean();
            ConversationListBean conversationListBean = new ConversationListBean();
            conversationListBean.userID = receiverID;
            conversationListBean.avator = receiverAvator;
            conversationListBean.nickname = receiverNickname;
            BaseMessage baseMessage = message.getMessage();
            if(baseMessage instanceof TextMessage){
                conversationListBean.message = ((TextMessage)baseMessage).text;
            }
            conversationListBean.time = message.time;
            refreshBean.conversationListBean = conversationListBean;
            ConversationListDBHelper.getInstance().updateConversationList(refreshBean.conversationListBean,false);
            RxBus.get().post(refreshBean);

            ConversationMessageDBHelper.getInstance().insertMessage(message);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(null != imageChooser){
            imageChooser.onActivityResult(requestCode, resultCode, data);
        }
    }
}
