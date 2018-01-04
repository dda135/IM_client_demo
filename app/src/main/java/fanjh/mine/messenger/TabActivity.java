package fanjh.mine.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fanjh.mine.applibrary.base.BaseActivity;
import fanjh.mine.messenger.conversation.list.ConversationListFragment;
import fanjh.mine.messenger.friend.FriendFragment;
import fanjh.mine.messenger.mine.MineFragment;

/**
 * @author fanjh
 * @date 2017/11/29 14:42
 * @description
 * @note
 **/
public class TabActivity extends BaseActivity {
    public static final int TAB_CONVERSATION = 0;
    public static final int TAB_FRIEND = 1;
    public static final int TAG_MINE = 2;
    @BindView(R.id.vp_content)
    ViewPager vpContent;
    @BindView(R.id.tab_iv_conversation)
    ImageView tabIvConversation;
    @BindView(R.id.tab_conversation)
    LinearLayout tabConversation;
    @BindView(R.id.tab_iv_friend)
    ImageView tabIvFriend;
    @BindView(R.id.tab_friend)
    LinearLayout tabFriend;
    @BindView(R.id.tab_iv_mine)
    ImageView tabIvMine;
    @BindView(R.id.tab_mine)
    LinearLayout tabMine;

    @IntDef({TAB_CONVERSATION, TAB_FRIEND, TAG_MINE})
    public @interface Tabindex {
    }

    @Tabindex
    private int selectIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);
        ButterKnife.bind(this);
        vpContent.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position){
                    case TAB_CONVERSATION:
                        return new ConversationListFragment();
                    case TAB_FRIEND:
                        return new FriendFragment();
                    case TAG_MINE:
                        return new MineFragment();
                    default:
                        break;
                }
                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        vpContent.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setTabSelect(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabConversation.performClick();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TabActivity.class);
        context.startActivity(intent);
    }

    @OnClick({R.id.vp_content, R.id.tab_conversation, R.id.tab_friend, R.id.tab_mine})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.vp_content:
                break;
            case R.id.tab_conversation:
                vpContent.setCurrentItem(TAB_CONVERSATION);
                setTabSelect(TAB_CONVERSATION);
                break;
            case R.id.tab_friend:
                vpContent.setCurrentItem(TAB_FRIEND);
                setTabSelect(TAB_FRIEND);
                break;
            case R.id.tab_mine:
                vpContent.setCurrentItem(TAG_MINE);
                setTabSelect(TAG_MINE);
                break;
            default:
                break;
        }
    }

    private void setTabSelect(@Tabindex int index) {
        selectIndex = index;
        switch (index) {
            case TAB_CONVERSATION:
                setConversationTabSelect(true);
                setFriendTabSelect(false);
                setMineTabSelect(false);
                break;
            case TAB_FRIEND:
                setConversationTabSelect(false);
                setFriendTabSelect(true);
                setMineTabSelect(false);
                break;
            case TAG_MINE:
                setConversationTabSelect(false);
                setFriendTabSelect(false);
                setMineTabSelect(true);
                break;
            default:
                break;
        }
    }

    private void setConversationTabSelect(boolean select){
        tabConversation.getChildAt(0).setSelected(select);
        tabConversation.getChildAt(1).setSelected(select);
    }

    private void setFriendTabSelect(boolean select){
        tabFriend.getChildAt(0).setSelected(select);
        tabFriend.getChildAt(1).setSelected(select);
    }

    private void setMineTabSelect(boolean select){
        tabMine.getChildAt(0).setSelected(select);
        tabMine.getChildAt(1).setSelected(select);
    }

}
