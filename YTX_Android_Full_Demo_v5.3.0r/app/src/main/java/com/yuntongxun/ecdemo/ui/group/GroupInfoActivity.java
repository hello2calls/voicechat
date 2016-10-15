/*
 *  Copyright (c) 2013 The CCP project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a Beijing Speedtong Information Technology Co.,Ltd license
 *  that can be found in the LICENSE file in the root of the web site.
 *
 *   http://www.yuntongxun.com
 *
 *  An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package com.yuntongxun.ecdemo.ui.group;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yuntongxun.ecdemo.R;
import com.yuntongxun.ecdemo.common.CCPAppManager;
import com.yuntongxun.ecdemo.common.dialog.ECAlertDialog;
import com.yuntongxun.ecdemo.common.dialog.ECListDialog;
import com.yuntongxun.ecdemo.common.dialog.ECProgressDialog;
import com.yuntongxun.ecdemo.common.utils.DemoUtils;
import com.yuntongxun.ecdemo.common.utils.ECPreferenceSettings;
import com.yuntongxun.ecdemo.common.utils.ECPreferences;
import com.yuntongxun.ecdemo.common.utils.LogUtil;
import com.yuntongxun.ecdemo.common.utils.ToastUtil;
import com.yuntongxun.ecdemo.common.view.SettingItem;
import com.yuntongxun.ecdemo.core.ClientUser;
import com.yuntongxun.ecdemo.storage.ContactSqlManager;
import com.yuntongxun.ecdemo.storage.GroupMemberSqlManager;
import com.yuntongxun.ecdemo.storage.GroupSqlManager;
import com.yuntongxun.ecdemo.storage.IMessageSqlManager;
import com.yuntongxun.ecdemo.ui.ECSuperActivity;
import com.yuntongxun.ecdemo.ui.SDKCoreHelper;
import com.yuntongxun.ecdemo.ui.chatting.ChattingActivity;
import com.yuntongxun.ecdemo.ui.chatting.base.EmojiconTextView;
import com.yuntongxun.ecdemo.ui.contact.ContactDetailActivity;
import com.yuntongxun.ecdemo.ui.contact.ContactLogic;
import com.yuntongxun.ecdemo.ui.contact.ECContacts;
import com.yuntongxun.ecdemo.ui.contact.MobileContactSelectActivity;
import com.yuntongxun.ecdemo.ui.settings.EditConfigureActivity;
import com.yuntongxun.ecdemo.ui.settings.SettingsActivity;
import com.yuntongxun.ecsdk.ECError;
import com.yuntongxun.ecsdk.ECGroupManager;
import com.yuntongxun.ecsdk.ECVoIPCallManager;
import com.yuntongxun.ecsdk.SdkErrorCode;
import com.yuntongxun.ecsdk.im.ECGroup;
import com.yuntongxun.ecsdk.im.ECGroupMember;
import com.yuntongxun.ecsdk.im.ECGroupOption;
import com.yuntongxun.ecsdk.im.ESpeakStatus;
import com.yuntongxun.ecsdk.platformtools.ECHandlerHelper;

import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.List;

/**
 * 群组详情界面
 * 
 * @author Jorstin Chan@容联•云通讯
 * @since 2014-12-29
 * @version 4.0
 */
public class GroupInfoActivity extends ECSuperActivity implements
		View.OnClickListener, GroupMemberService.OnSynsGroupMemberListener,
		GroupService.Callback {

	private static final String TAG = "ECDemo.GroupInfoActivity";
	public final static String GROUP_ID = "group_id";
	public static final String EXTRA_RELOAD = "com.yuntongxun.ecdemo_reload";
	public static final String EXTRA_QUEIT = "com.yuntongxun.ecdemo_quit";

	private ScrollView mScrollView;
	private TextView mGroupCountTv;
	/** 群组ID */
	private ECGroup mGroup;
	/** 群组公告 */
	private EditText mNotice;
	/** 群组成员列表 */
	private ListView mListView;
	/** 群组成员适配器 */
	private GroupInfoAdapter mAdapter;
	private ECProgressDialog mPostingdialog;
	private boolean mClearChatmsg = false;
	private SettingItem mNoticeItem;
	private SettingItem mGroupCardItem;
	private SettingItem mNameItem;
	private SettingItem mNewMsgNotify;
	private SettingItem mRoomDisplayname;
	private int mEditMode = -1;

	private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			ECGroupMember item = mAdapter.getItem(position);
			if (item == null) {
				return;
			}

			if ("add@yuntongxun.com".equals(item.getVoipAccount())) {
				Intent intent = new Intent(GroupInfoActivity.this, MobileContactSelectActivity.class);
				intent.putExtra("group_select_need_result", true);
				intent.putExtra("select_type", false);
				startActivityForResult(intent, 0x2a);
				return;
			}
			// GroupMemberService.queryGroupMemberCard(mGroup.getGroupId() ,
			// item.getVoipAccount());
			// GroupMemberService.modifyGroupMemberCard(mGroup.getGroupId(),
			// item.getVoipAccount());
			ECContacts contact = ContactSqlManager.getContact(item .getVoipAccount());
			if (contact == null || contact.getId() == -1) {
				ToastUtil.showMessage(R.string.contact_none);
				return;
			}
			Intent intent = new Intent(GroupInfoActivity.this, ContactDetailActivity.class);
			intent.putExtra(ContactDetailActivity.RAW_ID, contact.getId());
			startActivity(intent);
		}
	};

	private AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			return doShowMoreMenu(position);
		}
	};


	private String groupId;
	private  TextView buttonQuit;

	@Override
	protected int getLayoutId() {
		return R.layout.group_info_activity;
	}

	@Override
	protected boolean isEnableSwipe() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		groupId = getIntent().getStringExtra(GROUP_ID);
		mGroup = GroupSqlManager.getECGroup(groupId);
		if (mGroup == null) {
			finish();
			return;
		}
		isLocalDiscussion = GroupSqlManager.isDiscussionGroup(mGroup.getGroupId());
		initView();
		refreshGroupInfo();

		GroupService.syncGroupInfo(mGroup.getGroupId());
		GroupMemberService.synsGroupMember(mGroup.getGroupId());

		registerReceiver(new String[]{IMessageSqlManager.ACTION_GROUP_DEL});
		
		isFromDiscussionInviteClick=false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mRole = ECGroupMember.Role.values()[GroupMemberSqlManager.getSelfRoleWithGroupId(groupId,CCPAppManager.getUserId()) - 1];
		GroupService.addListener(this);
		GroupMemberService.addListener(this);
	}

	/**
     *
     */
	private void initView() {
		mNotice = (EditText) findViewById(R.id.group_notice);
		mGroupCountTv = (TextView) findViewById(R.id.group_count);
		mNoticeItem = (SettingItem) findViewById(R.id.group_notice2);
		mGroupCardItem = (SettingItem) findViewById(R.id.group_card);
		tvShowMoreMember = (TextView) findViewById(R.id.group_showmember_btn);
		tvShowMoreMember.setText("查看更多");
		tvShowMoreMember.setOnClickListener(this);
		
		mGroupCardItem.setOnClickListener(this);
		mNoticeItem.getCheckedTextView().setSingleLine(false);
		// mNoticeItem.getCheckedTextView().setMaxLines(5);
		mNameItem = (SettingItem) findViewById(R.id.group_name);
		mNameItem.getCheckedTextView().setSingleLine(false);
		mListView = (ListView) findViewById(R.id.member_lv);
		mListView.setOnItemClickListener(mItemClickListener);
		setMemberLongCLick();
		mScrollView = (ScrollView) findViewById(R.id.info_content);
		mAdapter = new GroupInfoAdapter(this);
		mListView.setAdapter(mAdapter);

		mNewMsgNotify = (SettingItem) findViewById(R.id.settings_new_msg_notify);
		mRoomDisplayname = (SettingItem) findViewById(R.id.settings_room_my_displayname);
		mNewMsgNotify.getCheckedTextView().setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						updateGroupNewMsgNotify();
					}
				});
		
	  mShowChatName = (SettingItem) findViewById(R.id.settings_group_show_nickname);
		mShowChatName.getCheckedTextView().setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						updateShowChatName();
					}
				});
		
		boolean isShowChatName = ECPreferences.getSharedPreferences().getBoolean(ECPreferenceSettings.SETTINGS_SHOW_CHATTING_NAME.getId(), false);
		
		mShowChatName.setChecked(isShowChatName);

		findViewById(R.id.red_btn).setOnClickListener(this);
		findViewById(R.id.clear_msg).setOnClickListener(this);
//		mNotice.setEnabled(isOwner());
		button = (TextView) findViewById(R.id.red_btn);
		buttonQuit = (TextView) findViewById(R.id.red_btn_quit);
		buttonQuit.setOnClickListener(this);
		buttonQuit.setText(R.string.str_group_quit);

		if (GroupSqlManager.isDiscussionGroup(groupId)) {

			button.setText(R.string.quit_discussion);
			mNameItem.setTitleText(getString(R.string.dis_name));
			mNoticeItem.setTitleText(getString(R.string.dis_notice));

		} else {

			button.setText(isOwner() ? R.string.str_group_dissolution
					: R.string.str_group_quit);
		}
//		onSynsGroupMember(mGroup.getGroupId());
		
//		if(isLocalDiscussion){
			mNameItem.setOnClickListener(new OnItemClickListener(
					SettingsActivity.CONFIG_TYPE_GROUP_NAME));
			
			mNoticeItem.setOnClickListener(new OnItemClickListener(
					SettingsActivity.CONFIG_TYPE_GROUP_NOTICE));
//		}
	}

	private void setMemberLongCLick() {
		if(!isLocalDiscussion && isOwner()) {
			mListView.setOnItemLongClickListener(mOnItemLongClickListener);
			return ;
		}
		mListView.setOnItemLongClickListener(null);
	}

	/**
	 * 处理长按操作
	 * @param position 选择位置
	 * @return 返回
	 */
	private boolean doShowMoreMenu(int position) {
		ECGroupMember item = mAdapter.getItem(position);
		if(item == null || item.getVoipAccount().equals(CCPAppManager.getUserId())) {
			return true;
		}
		if (!"add@yuntongxun.com".equals(item.getVoipAccount())) {
			showMoreMenu(this, item);
		}
		return true;
	}


	/**
	 * 多选菜单
	 * @param ctx 上下文
	 * @param member 账号
	 */
	public void showMoreMenu(final Context ctx , final ECGroupMember member) {
		int resource = (member.getMemberRole() == ECGroupMember.Role.MEMBER) ?
				R.array.role_controller_one : R.array.role_controller_multi;
		ECListDialog dialog = new ECListDialog(ctx , resource);
		dialog.setOnDialogItemClickListener(new ECListDialog.OnDialogItemClickListener() {
			@Override
			public void onDialogItemClick(Dialog d, int position) {
				if(position == 0) {
					// 任命新群组
					transferGroupController(member);
					return ;
				}
				doSetMemberRole(member);
			}
		});
		dialog.setTitle(R.string.ec_group_controller_mode_select);
		dialog.show();
	}

	/**
	 * 转让群组控制权限
	 * @param member 群组成员
	 */
	private void transferGroupController(final ECGroupMember member) {
		String msg = getString(R.string.group_controller_transfer , member.getDisplayName());
		ECAlertDialog buildAlert = ECAlertDialog.buildAlert(this, msg,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						showProcessDialog(getString(R.string.login_posting_submit));
						doLogicSetMemberRole(mGroup.getGroupId(), member.getVoipAccount(), ECGroupManager.ECGroupMemberRole.TRANSFER);
					}
				});

		buildAlert.setTitle(R.string.app_tip);
		buildAlert.show();
	}

	private void updateGroupNewMsgNotify() {
		if (mGroup == null || mGroup.getGroupId() == null) {
			return;
		}
		// 处理消息免打扰
		try {
			if (mNewMsgNotify == null) {
				return;
			}
			mNewMsgNotify.toggle();
			boolean checked = mNewMsgNotify.isChecked();
			showProcessDialog(getString(R.string.login_posting_submit));
			ECGroupOption option = new ECGroupOption();
			option.setGroupId(mGroup.getGroupId());
			option.setRule(checked ? ECGroupOption.Rule.SILENCE
					: ECGroupOption.Rule.NORMAL);
			GroupService.setGroupMessageOption(option);
			LogUtil.d(TAG, "updateGroupNewMsgNotify: " + checked);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void updateShowChatName() {
		if (mGroup == null || mGroup.getGroupId() == null) {
			return;
		}
		try {
			if (mShowChatName == null) {
				return;
			}
			mShowChatName.toggle();
			showCommonProcessDialog("");
			boolean checked = mShowChatName.isChecked();
			GroupService.setGroupIsAnonymity(groupId, checked);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
     *
     */
	private void refreshGroupInfo() {
		if (mGroup == null) {
			return;
		}
		setMemberLongCLick();
		mNotice.setText(mGroup.getDeclare());
		mNoticeItem.setCheckText(mGroup.getDeclare());
		mNotice.setSelection(mNotice.getText().length());
		if (mGroup.getName() != null && mGroup.getName().endsWith("@priategroup.com")) {
			ArrayList<String> member = GroupMemberSqlManager.getGroupMemberID(mGroup.getGroupId());
			if (member != null) {
				ArrayList<String> contactName = ContactSqlManager.getContactName(member.toArray(new String[] {}));
				String chatroomName = DemoUtils.listToString(contactName, ",");
				mGroup.setName(chatroomName);
			}
		}
		getTopBarView().setTopBarToStatus(1, R.drawable.topbar_back_bt, -1,
				mGroup.getName(), this);
		mNameItem.setCheckText(mGroup.getName());
		mNewMsgNotify.setChecked(!mGroup.isNotice());
		
		if (GroupSqlManager.isDiscussionGroup(groupId)) {
			button.setText(R.string.quit_discussion);
			mNameItem.setTitleText(getString(R.string.dis_name));
			mNoticeItem.setTitleText(getString(R.string.dis_notice));

		} else {
			button.setText(isOwner() ? R.string.str_group_dissolution : R.string.str_group_quit);
		}
		if (isOwner() && !isLocalDiscussion) {
			buttonQuit.setVisibility(View.VISIBLE);
			button.setVisibility(View.VISIBLE);
		}else {
			buttonQuit.setVisibility(View.VISIBLE);
			button.setVisibility(View.GONE);
		}

		if(isLocalDiscussion){
			buttonQuit.setVisibility(View.VISIBLE);
			button.setVisibility(View.GONE);
		}
	}

	/**
	 * 是否是群组创建者
	 * 
	 * @return
	 */
	private boolean isOwner() {
		return CCPAppManager.getUserId() .equals(mGroup.getOwner());
	}

	private String getSelfVoip(){
		return  CCPAppManager.getUserId();
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_left:
			goBack();
			break;
		case R.id.group_showmember_btn:
			
			if(tvShowMoreMember.getText().equals("查看更多")){
				tvShowMoreMember.setText("收起");
				mAdapter.setData(members);
				mAdapter.notifyDataSetChanged();
			   setListViewHeightBasedOnChildren(mListView);
			}else {
				tvShowMoreMember.setText("查看更多");
				if(subMemberList!=null){
				mAdapter.setData(subMemberList);
			   setListViewHeightBasedOnChildren(mListView);
				}
				mAdapter.notifyDataSetChanged();
			}
			
			
			
			break;
		case R.id.red_btn:
			mPostingdialog = new ECProgressDialog(this,
					"请稍后...");
			mPostingdialog.show();

			if (GroupSqlManager.isDiscussionGroup(groupId)) {
				GroupService.quitGroup(mGroup.getGroupId());
				return;
			}

			if (isOwner()) {
				GroupService.disGroup(mGroup.getGroupId());
				return;
			}
			GroupService.quitGroup(mGroup.getGroupId());
			break;
			case R.id.red_btn_quit:

				if(isLocalDiscussion){
					GroupService.quitGroup(mGroup.getGroupId());
			}else {
				doOwnerQuitGroup();
			}

			break;
		case R.id.clear_msg:
			ECAlertDialog buildAlert = ECAlertDialog.buildAlert(
					GroupInfoActivity.this,
					R.string.fmt_delcontactmsg_confirm_group, null,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							mPostingdialog = new ECProgressDialog(
									GroupInfoActivity.this, R.string.clear_chat);
							mPostingdialog.show();
							ECHandlerHelper handlerHelper = new ECHandlerHelper();
							handlerHelper.postRunnOnThead(new Runnable() {
								@Override
								public void run() {
									IMessageSqlManager .deleteChattingMessage(mGroup .getGroupId());
									ToastUtil .showMessage(R.string.clear_msg_success);
									mClearChatmsg = true;
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											dismissPostingDialog();
										}
									});
								}
							});

						}

					});
			buildAlert.setTitle(R.string.app_tip);
			buildAlert.show();
			break;
		case R.id.btn_middle:
			if (mScrollView != null) {
				getTopBarView().post(new Runnable() {
					@Override
					public void run() {
						mScrollView.fullScroll(View.FOCUS_UP);
					}
				});
			}
			break;
			
		case R.id.group_card:
			startActivity(new Intent(this, GroupMemberCardActivity.class).putExtra("groupId", groupId));
			
			
			break;
		default:
			break;
		}
	}

	private boolean isHasManager=false;

	private void doOwnerQuitGroup() {
		String msg=null;

		final boolean isHasManagerInner = GroupMemberSqlManager.isGroupHasManager(groupId);
		if(!isHasManagerInner){
			msg="当前没有管理员、选择取消去设置管理员";
		}else {
			msg="确定是否退出群组";
		}


		if(mRole== ECGroupMember.Role.OWNER&&isOwner()) {

			ECAlertDialog buildAlert = ECAlertDialog.buildAlert(this, msg,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (isHasManagerInner) {
								showProcessDialog(getString(R.string.login_posting_submit));
								GroupService.quitGroup(groupId);
							}


						}
					});


			buildAlert.setTitle(R.string.app_tip);
			buildAlert.show();
		}else {
			GroupService.quitGroup(groupId);
		}





	}

	void showProcessDialog(String tips) {
		mPostingdialog = new ECProgressDialog(GroupInfoActivity.this,
				R.string.login_posting_submit);
		mPostingdialog.show();
	}

	/**
	 * 关闭对话框
	 */
	private void dismissPostingDialog() {
		if (mPostingdialog == null || !mPostingdialog.isShowing()) {
			return;
		}
		mPostingdialog.dismiss();
		mPostingdialog = null;
	}
	
	public static boolean isFromDiscussionInviteClick=false;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		LogUtil.d(TAG, "onActivityResult: requestCode=" + requestCode
				+ ", resultCode=" + resultCode + ", data=" + data);

		// If there's no data (because the user didn't select a picture and
		// just hit BACK, for example), there's nothing to do.
		if (requestCode == 0x2a || requestCode == 0xa) {
			if (data == null) {
				return;
			}
		} else if (resultCode != RESULT_OK) {
			LogUtil.d("onActivityResult: bail due to resultCode=" + resultCode);
			return;
		}
		if (requestCode == 0x2a) {
			String[] selectUser = data.getStringArrayExtra("Select_Conv_User");
			if (selectUser != null && selectUser.length > 0) {
				mPostingdialog = new ECProgressDialog(this,
						R.string.invite_join_group_posting);
				mPostingdialog.show();
				String reason = getString(R.string.group_invite_reason,
						CCPAppManager.getClientUser().getUserName(),
						mGroup.getName());

				if (GroupSqlManager.isDiscussionGroup(groupId)) {
					isFromDiscussionInviteClick=true;
					GroupMemberService.inviteMembers(mGroup.getGroupId(),
							reason, ECGroupManager.InvitationMode.FORCE_PULL,
							selectUser);
				} else {
					GroupMemberService.inviteMembers(mGroup.getGroupId(),
							reason, ECGroupManager.InvitationMode.NEED_CONFIRM,
							selectUser);
				}
			}
		} else if (requestCode == 0xa) {
			String result_data = data.getStringExtra("result_data");
			if (mGroup == null) {
				return;
			}
			if (TextUtils.isEmpty(result_data)) {
				ToastUtil.showMessage("不允许为空");
				return;
			}
			if (mEditMode == SettingsActivity.CONFIG_TYPE_GROUP_NAME) {
				mGroup.setName(result_data);
			} else {
				mGroup.setDeclare(result_data);
			}

			doModifyGroup();
		}
	}

	private void doModifyGroup() {
		// 修改群组信息请求
		showProcessDialog(getString(R.string.login_posting_submit));
		GroupService.modifyGroup(mGroup);
		LogUtil.e(mGroup.getPermission().name());
	}

	private ECGroupMember.Role mRole = ECGroupMember.Role.MEMBER;//自己角色权限

	@Override
	public void onSynsGroupMember(String groupId) {
		
		if(isFromDiscussionInviteClick){
			finish();
			return;
		}

		
		dismissPostingDialog();
		if (mGroup == null || !mGroup.getGroupId().equals(groupId)) {
			return;
		}
		int count = mAdapter.getCount();
		members = GroupMemberSqlManager.getGroupMemberWithName(mGroup.getGroupId());
		
		if (members == null) {
			members = new ArrayList<ECGroupMember>();
		}
		boolean hasSelf = false;
		for (ECGroupMember member : members) {
			if (CCPAppManager.getUserId().equals(member.getVoipAccount())) {
				hasSelf = true;
				mRole = member.getMemberRole();
				break;
			}
		}
		if (!hasSelf) {
			ECContacts contact = ContactSqlManager.getContact(CCPAppManager .getUserId());
			if (contact != null) {
				ECGroupMember member = new ECGroupMember();
				member.setVoipAccount(contact.getContactid());
				member.setRemark(contact.getRemark());
				member.setDisplayName(contact.getNickname());
				members.add(member);
			}
		}


		if (members != null && members.size() > 8) {
			subMemberList = members.subList(0, 8);
			mAdapter.setData(subMemberList);
			tvShowMoreMember.setVisibility(View.VISIBLE);
			tvShowMoreMember.setText("查看更多");
		} else {
			mAdapter.setData(members);
			tvShowMoreMember.setVisibility(View.GONE);
		}

		int memCount = members.size();
		if (isLocalDiscussion) {
			mGroupCountTv.setText(getString(R.string.str_discussion_members_tips,
					memCount));
		} else {
			mGroupCountTv.setText(getString(R.string.str_group_members_tips,
					memCount));
		}

		if (members != null && count <= members.size()) {
			setListViewHeightBasedOnChildren(mListView);
		}
		mAdapter.notifyDataSetChanged();
		getTopBarView().setTitle(mGroup.getName() + getString(R.string.str_group_members_titletips,
				memCount));
		setListViewHeightBasedOnChildren(mListView);

		if(mRole!= ECGroupMember.Role.OWNER){
			button.setVisibility(View.INVISIBLE);
		}

		mNameItem.setOnClickListener(new OnItemClickListener(
				SettingsActivity.CONFIG_TYPE_GROUP_NAME));

		mNoticeItem.setOnClickListener(new OnItemClickListener(
				SettingsActivity.CONFIG_TYPE_GROUP_NOTICE));

	}
	
	public boolean isRefresh=false;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			goBack();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void goBack() {
		Intent intent = new Intent(this, ChattingActivity.class);
		intent.putExtra(EXTRA_RELOAD, mClearChatmsg);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mListView != null) {
			mListView.setOnItemLongClickListener(null);
			mListView.setAdapter(null);
		}
		mItemClickListener = null;
		isHasManager=false;
		mRole = ECGroupMember.Role.MEMBER;
		isFromDiscussionInviteClick=false;
		if (mAdapter != null) {
			mAdapter.setData(null);
			mAdapter.clear();
			mAdapter = null;
		}
		GroupService.addListener(null);
		GroupMemberService.addListener(null);
		mGroup = null;
		mPostingdialog = null;
		System.gc();
	}

	private boolean isGroupManager(ECGroupMember.Role role) {
		return mRole != null && mRole.ordinal() < role.ordinal();
	}

	private boolean isGroupOwnerRole(ECGroupMember.Role role) {
		return role == ECGroupMember.Role.OWNER;
	}


	/**
	 * 动态改变ListView 高度
	 * 
	 * @param listView 列表
	 */
	public void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			return;
		}
		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}
		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight
				+ (listView.getDividerHeight() * (listAdapter.getCount() + 2));
		((MarginLayoutParams) params).setMargins(10, 10, 10, 10);
		listView.setLayoutParams(params);

		getActivityLayoutView().invalidate();
	}
    boolean isManager=false;
	private TextView button;
	private boolean isLocalDiscussion;
	private List<ECGroupMember> subMemberList;
	private TextView tvShowMoreMember;
	private ArrayList<ECGroupMember> members;
	private SettingItem mShowChatName;
	public class GroupInfoAdapter extends ArrayAdapter<ECGroupMember> {
		Context mContext;
		boolean mHandler;

		public GroupInfoAdapter(Context context) {
			super(context, 0);
			mContext = context;
		}

		public void setData(List<ECGroupMember> data) {
			mHandler = false;
			clear();
			if (data != null) {
				for (ECGroupMember appEntry : data) {
					
					if(CCPAppManager.getUserId().equals(appEntry.getVoipAccount() )) {
						mHandler = (appEntry.getMemberRole() == ECGroupMember.Role.OWNER
								|| appEntry.getMemberRole() == ECGroupMember.Role.MANAGER);
					}
					add(appEntry);
				}
			}

			// 是否是群组/或者有修改权限
			boolean changePermission = mHandler || isLocalDiscussion;
			if (changePermission) {
				isManager = mHandler;
				ECGroupMember add = new ECGroupMember();
				add.setVoipAccount("add@yuntongxun.com");
				add(add);

			}
//			changeModifyPermission(changePermission);

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view;
			ViewHolder mViewHolder;
			if (convertView == null || convertView.getTag() == null) {
				if (!isLocalDiscussion) {
					view = View.inflate(mContext, R.layout.group_member_item, null);
				} else {
					view = View.inflate(mContext, R.layout.discussion_member_item, null);
				}

				mViewHolder = new ViewHolder();
				mViewHolder.mAvatar = (ImageView) view .findViewById(R.id.group_card_item_avatar_iv);
				mViewHolder.name_tv = (EmojiconTextView) view .findViewById(R.id.group_card_item_nick);
				mViewHolder.account = (TextView) view .findViewById(R.id.account);
				mViewHolder.operationRm = (Button) view .findViewById(R.id.remove_btn);
				mViewHolder.operationSpeak = (Button) view .findViewById(R.id.speak_btn);
				mViewHolder.operationLy = (LinearLayout) view .findViewById(R.id.operation_ly);
				mViewHolder.setRole = (Button) view.findViewById(R.id.setrole_btn);
				view.setTag(mViewHolder);
			} else {
				view = convertView;
				mViewHolder = (ViewHolder) view.getTag();
			}
			if(isLocalDiscussion){
				mViewHolder.operationSpeak.setVisibility(View.GONE);//讨论组不显示禁言
			}
			final ECGroupMember item = getItem(position);
			if(item == null) {
				return view;
			}
			if(!isLocalDiscussion) {
				// 不是讨论组
				if (isGroupManager(item.getMemberRole()) && !item.getVoipAccount().equals(getSelfVoip())) {
					// 判断对当前成员是否有管理权限
					mViewHolder.operationSpeak.setVisibility(View.VISIBLE);
					mViewHolder.operationRm.setVisibility(View.VISIBLE);
				} else {
					mViewHolder.operationSpeak.setVisibility(View.GONE);
					mViewHolder.operationRm.setVisibility(View.GONE);
				}
			}
			if (isGroupOwnerRole(item.getMemberRole()) && !getSelfVoip().equals(item.getVoipAccount())) {
				mViewHolder.operationSpeak.setVisibility(View.VISIBLE);
				mViewHolder.operationRm.setVisibility(View.VISIBLE);
				mViewHolder.setRole.setVisibility(View.VISIBLE);
				if (isLocalDiscussion) {
					mViewHolder.operationSpeak.setVisibility(View.GONE);
					mViewHolder.operationRm.setVisibility(View.VISIBLE);
					mViewHolder.setRole.setVisibility(View.GONE);
				}
			}

			item.setDisplayName(TextUtils.isEmpty(item.getDisplayName()) ? item .getVoipAccount() : item.getDisplayName());
			mViewHolder.operationSpeak .setText(item.isBan() ? R.string.str_group_speak_enable : R.string.str_group_speak_disenable);
			mViewHolder.operationSpeak.setTextColor(item.isBan() ? Color
					.parseColor("#ffff5454") : Color
					.parseColor("#ff00B486"));

			mViewHolder.setRole.setVisibility(View.GONE);
			mViewHolder.account.setText(item.getVoipAccount());
			if (item.getVoipAccount().equals("add@yuntongxun.com") && position == getCount() - 1) {
				mViewHolder.mAvatar.setImageResource(R.drawable.add_contact_selector);
				mViewHolder.name_tv.setText(R.string.str_group_invite);//邀请
				mViewHolder.operationLy.setVisibility(View.INVISIBLE);
				mViewHolder.account.setVisibility(View.GONE);
			} else {
				mViewHolder.account.setVisibility(View.VISIBLE);
				mViewHolder.mAvatar.setImageBitmap(ContactLogic.getPhoto(item.getRemark()));
				String creator;
				if (item.getMemberRole() == ECGroupMember.Role.OWNER) {
					creator = "[创建者]";
					isHasManager = true;
				} else if (item.getMemberRole() == ECGroupMember.Role.MANAGER) {
					creator = "[管理员]";
					isHasManager = true;
				} else {
					creator = "[成员]";
					isHasManager = false;
				}
				mViewHolder.name_tv.setText(item.getDisplayName() + creator);
				if (mHandler && !CCPAppManager.getUserId().equals(item.getVoipAccount())) {
					mViewHolder.operationLy.setVisibility(View.VISIBLE);

					isManager = true;
					mViewHolder.operationRm.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							// 处理成员移除
							doRemoveMember(item);
						}
					});

					mViewHolder.operationSpeak.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// 处理成员禁言
							doSetMemberSpeakStatus(item);
						}
					});

					mViewHolder.setRole.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							doSetMemberRole(item);
						}
					});

				} else {
					mViewHolder.operationLy.setVisibility(View.INVISIBLE);
				}

				if (isGroupManager(item.getMemberRole()) && !item.getVoipAccount().equals(getSelfVoip())) {
					// 判断对当前成员是否有管理权限
					mViewHolder.operationSpeak.setVisibility(View.VISIBLE);
					mViewHolder.operationRm.setVisibility(View.VISIBLE);
				} else {
					mViewHolder.operationSpeak.setVisibility(View.GONE);
					mViewHolder.operationRm.setVisibility(View.GONE);
				}

				if(isLocalDiscussion){
					mViewHolder.operationSpeak.setVisibility(View.GONE);
				}

			}
			return view;
		}

		class ViewHolder {
			/** 头像 */
			ImageView mAvatar;
			/** 名称 */
			EmojiconTextView name_tv;
			TextView account;
			/** 踢出按钮 */
			Button operationRm;
			/** 禁言 */
			Button operationSpeak;
			LinearLayout operationLy;
			Button setRole;

		}
	}

	private void changeModifyPermission(boolean  permission) {
		if (permission && (isManager() || isLocalDiscussion)) {
            mNameItem.setOnClickListener(new OnItemClickListener( SettingsActivity.CONFIG_TYPE_GROUP_NAME));
            mNoticeItem.setOnClickListener(new OnItemClickListener( SettingsActivity.CONFIG_TYPE_GROUP_NOTICE));
        } else {
			mNameItem.setOnClickListener(null);
			mNoticeItem.setOnClickListener(null);
		}
	}


	/**
	 * 设置群组成员禁言状态
	 * 
	 * @param item
	 */
	private void doSetMemberSpeakStatus(final ECGroupMember item) {
		String msg = getString(R.string.str_group_member_speak_tips,
				item.getDisplayName());
		if (item.isBan()) {
			msg = getString(R.string.str_group_member_unspeak_tips,
					item.getDisplayName());
		}
		ECAlertDialog buildAlert = ECAlertDialog.buildAlert(this, msg,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						showProcessDialog(getString(R.string.login_posting_submit));
						forbidMemberSpeakStatus(mGroup.getGroupId(), item.getVoipAccount(),
								!item.isBan());
						
						
					}
				});

		buildAlert.setTitle(R.string.app_tip);
		buildAlert.show();
	}
	private void doSetMemberRole(final ECGroupMember item) {
		String msg = getString(R.string.str_group_member_setas_manager, item.getDisplayName());
		if (item.getMemberRole() == ECGroupMember.Role.MANAGER) {
			msg = getString(R.string.str_group_member_cancel_setas_manager, item.getDisplayName());
		}
		ECAlertDialog buildAlert = ECAlertDialog.buildAlert(this, msg,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						showProcessDialog(getString(R.string.login_posting_submit));
						ECGroupManager.ECGroupMemberRole roleRule = null;
						if (item.getMemberRole() == ECGroupMember.Role.MANAGER) {
							roleRule = ECGroupManager.ECGroupMemberRole.MEMBER;
						} else if (item.getMemberRole() == ECGroupMember.Role.MEMBER) {
							roleRule = ECGroupManager.ECGroupMemberRole.MANAGER;
						}
						if(roleRule != null) doLogicSetMemberRole(mGroup.getGroupId(), item.getVoipAccount(), roleRule);


					}
				});

		buildAlert.setTitle(R.string.app_tip);
		buildAlert.show();
	}

	private boolean isSuccess(ECError error) {
        if(error.errorCode == SdkErrorCode.REQUEST_SUCCESS)  {
            return true;
        }
        return false;
    }
	
	public  void forbidMemberSpeakStatus(final String groupId , final String member ,final boolean enabled ) {
       
		
        ESpeakStatus speakStatus = new ESpeakStatus();
        speakStatus.setOperation(enabled ? 2 : 1);
        SDKCoreHelper.getECGroupManager().forbidMemberSpeakStatus(groupId, member, speakStatus, new ECGroupManager.OnForbidMemberSpeakStatusListener() {
            @Override
            public void onForbidMemberSpeakStatusComplete(ECError error, String groupId, String member) {
                
            	isRefresh=true;
            	dismissPostingDialog();
            	if (isSuccess(error)) {
                    GroupMemberSqlManager.updateMemberSpeakState(groupId, member, enabled);
                    GroupMemberService.synsGroupMember(mGroup.getGroupId());
                    
                } else {
                    ToastUtil.showMessage("设置失败[" + error.errorCode + "]");
                }
            }

        });
    }

	public void doLogicSetMemberRole(final String groupId, final String member, final ECGroupManager.ECGroupMemberRole enRole) {

		SDKCoreHelper.getECGroupManager().setGroupMemberRole(groupId, member, enRole, new ECGroupManager.OnSetGroupMemberRoleListener() {

			@Override
			public void onSetGroupMemberRoleComplete(ECError error, String groupId) {
				isRefresh = true;
				dismissPostingDialog();
				if (isSuccess(error)) {
					GroupMemberService.synsGroupMember(mGroup.getGroupId());
					boolean result = (enRole == ECGroupManager.ECGroupMemberRole.TRANSFER);
					button.setVisibility(result ? View.GONE : View.VISIBLE);
					if(result) mListView.setOnItemLongClickListener(null);
				} else {
					ToastUtil.showMessage("设置失败[" + error.errorCode + "]");
				}

			}
		});


    }

	/**
	 * 移除群组成员
	 * 
	 * @param item
	 */
	private void doRemoveMember(final ECGroupMember item) {
		ECAlertDialog buildAlert = ECAlertDialog.buildAlert(
				this,
				getString(R.string.str_group_member_remove_tips,
						item.getDisplayName()),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						showProcessDialog(getString(R.string.group_remove_member_posting));
						GroupMemberService.removerMember(mGroup.getGroupId(),
								item.getVoipAccount());
						mClearChatmsg = true;
					}
				});

		buildAlert.setTitle(R.string.app_tip);
		buildAlert.show();
	}
	
	public  void removerMember(String groupid , String member) {
      
        SDKCoreHelper.getECGroupManager().deleteGroupMember(groupid, member, new ECGroupManager.OnDeleteGroupMembersListener() {

			@Override
			public void onDeleteGroupMembersComplete(ECError error, String groupId, String members) {
				dismissPostingDialog();
				isRefresh = true;
				if (isSuccess(error)) {
					GroupMemberSqlManager.delMember(groupId, members);
					int position = 0;
					for (int i = 0; i < GroupInfoActivity.this.members.size(); i++) {

						ECGroupMember item = GroupInfoActivity.this.members.get(i);
						if (item != null && members.equalsIgnoreCase(item.getVoipAccount())) {
							position = i;
							break;
						}
					}
					GroupInfoActivity.this.members.remove(position);
					mAdapter.notifyDataSetChanged();


				} else {
					ToastUtil.showMessage("移除成员失败[" + error.errorCode + "]");
				}
			}
		});

    }

	@Override
	public void onSyncGroup() {

	}

	@Override
	public void onSyncGroupInfo(String groupId) {
		dismissPostingDialog();
		if (mGroup == null || !mGroup.getGroupId().equals(groupId)) {
			return;
		}
		mGroup = GroupSqlManager.getECGroup(groupId);
		isLocalDiscussion = GroupSqlManager.isDiscussionGroup(groupId);
		refreshGroupInfo();
	}

	@Override
	public void onGroupDel(String groupId) {
		if (mGroup == null || !mGroup.getGroupId().equals(groupId)) {
			return;
		}
		dismissPostingDialog();
		ECGroup ecGroup = GroupSqlManager.getECGroup(mGroup.getGroupId());
		Intent intent = new Intent(this, ChattingActivity.class);
		intent.putExtra(EXTRA_QUEIT, true);
		setResult(RESULT_OK, intent);
		if (ecGroup == null) {
			// 群组被解散
			finish();
			return;
		}
		finish();
		// 更新群组界面 已经退出群组
	}

	@Override
	public void onError(ECError error) {
		dismissPostingDialog();
	}
	
	@Override
	public void onUpdateGroupAnonymitySuccess(String groupId,boolean isAnonymity) {
		// TODO Auto-generated method stub
		dismissCommonPostingDialog();
		ToastUtil.showMessage(R.string.modify_success);
		try {
			ECPreferences.savePreference(ECPreferenceSettings.SETTINGS_SHOW_CHATTING_NAME, isAnonymity, true);
		} catch (InvalidClassException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean isManager() {
		int selfRole = GroupMemberSqlManager.getSelfRoleWithGroupId(groupId, getSelfVoip());
		return selfRole == 1 || selfRole == 2;

	}

	private final class OnItemClickListener implements View.OnClickListener {

		private int mType;

		public OnItemClickListener(int type) {
			this.mType = type;
		}

		@Override
		public void onClick(View v) {
			
			if(EditConfigureActivity.isTop){
				return;
			}
			if (!isManager()&&!isLocalDiscussion) {
				return;
			}

			
			mEditMode = this.mType;
			Intent intent = new Intent(GroupInfoActivity.this,
					EditConfigureActivity.class);
			if (mEditMode == SettingsActivity.CONFIG_TYPE_GROUP_NAME) {
				if (!isLocalDiscussion) {
					intent.putExtra("edit_title", getString(R.string.edit_group_name));
				} else {
					intent.putExtra("edit_title", getString(R.string.edit_discussion_name));
				}
				intent.putExtra("edit_default_data", mGroup.getName());
			} else {
				if (isLocalDiscussion) {
					intent.putExtra("edit_title", getString(R.string.edit_discussion_notice));
				} else {
					intent.putExtra("edit_title", getString(R.string.edit_group_notice));
				}
				intent.putExtra("edit_default_data", mGroup.getDeclare());
			}
			startActivityForResult(intent, 0xa);
		}
	}

	@Override
	protected void handleReceiver(Context context, Intent intent) {
		super.handleReceiver(context, intent);
		if (IMessageSqlManager.ACTION_GROUP_DEL.equals(intent.getAction())
				&& intent.hasExtra("group_id")) {
			String id = intent.getStringExtra("group_id");
			if (id != null && id.equals(mGroup.getGroupId())) {
				finish();
			}
		}
	}

	
}