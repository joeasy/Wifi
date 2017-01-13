package com.rtk.dmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5.DeviceStatusListener;
import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.realtek.wakeonlan.DeviceWakeUpper;
import com.rtk.dmp.DeviceInfo.atrribute;

import android.app.Activity;
import android.app.TvManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class DMSListActivity extends Activity implements Observer {
	private String TAG = "DMSListActivity";
	
	private boolean isChange = false;
	private boolean isActivityReady = false;
	private boolean loading = false;
	
	private final static String rootPath = "/http://upnp/";

	private ArrayList<DLNAFileInfo> listItems = null;
	private MediaApplication mediaApp = null;

	private final int MSG_DEVICE_REMOVED = 0;
	private final int MSG_NORMAL_REFRESH = 1;
	private final int MSG_STARTANIMATION = 2;
	private final int MSG_DISSMISSANIMATION = 3;
	private final int MSG_HINT_DISMISS = 4;
	private final int MSG_SELECT_ITEM_REFRESH = 5;
	private final int MSG_SHOW_WOL_SENDED_HINT = 6;
	
	private Timer timer = null;
	private TimerTask task_msg_show_timeout = null;

	private boolean isKeyBack = false;
	private String source = null;
	private int selectedItem = -1;

	private ImageButton setting = null;
	private GridView dmslist = null;
	private ImageView loadingIcon = null;
	private TextView sourceType = null;
	private PopupMessage msg_hint = null;
	private Handler handler;
	private Animation ad = null;
	private Activity mContext = this;
	private FileListAdapter simpleAdapter = null;
	private setListAdapter menuAdapter = null;
	private Menu menu = null;
	private TvManager mTv = null;
	private DeviceInfo deviceInfo = null;
	private ComfirmDailog dailog = null;

	private ArrayList<String> DMSName = new ArrayList<String>();
	private ArrayList<String> serverUDN = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "DMSListActivity onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dms_list);

		msg_hint = new PopupMessage(mContext);
		mediaApp = (MediaApplication) getApplication();
		mediaApp.addObserver(this);
		dmslist = (GridView) findViewById(R.id.dms_list);
		dmslist.setOnItemClickListener(itemClickListener);
		dmslist.setOnItemLongClickListener(itemLongClickListener);

		setting = (ImageButton) findViewById(R.id.setting);
		setting.setOnClickListener(buttonClicklistener);

		sourceType = (TextView) findViewById(R.id.Top_Text);

		menuAdapter = new setListAdapter(this);
		menu = new Menu(this, menuAdapter);
		menu.setOnItemClickListener(menuItemClickListener);

		String path = getFilesDir().getPath();
		String fileName = path.concat("/DeviceInfo.bin");
		deviceInfo = mediaApp.getDeviceInfo(fileName);
		
		dailog = new ComfirmDailog(mContext);
		
		timer = new Timer(true);

		mTv = (TvManager) getSystemService("tv");
		loadingIcon = (ImageView) findViewById(R.id.loadingIcon);
		ad = AnimationUtils.loadAnimation(this, R.drawable.video_anim);
		ad.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				if (loading) {
					loadingIcon.startAnimation(ad);
				}
			}
		});
		initHandler();
		new Thread() {
			public void run() {
				loading = true;
				handler.sendEmptyMessage(MSG_STARTANIMATION);
				getFileList(rootPath);
				loading = false;
				handler.sendEmptyMessage(MSG_DISSMISSANIMATION);
				handler.sendEmptyMessage(MSG_NORMAL_REFRESH);
			}
		}.start();

		Intent intent = getIntent();
		captureIntent(intent);
	}
	
	@Override    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
	     	{
	     		if(selectedItem >= 0){
	     			simpleAdapter.notifyDataSetChanged();
	     			return true;
	     		}
	     		break;
	     	}
		}
		return super.onKeyDown(keyCode, event);
		
	}

	public void startLoading() {
		loadingIcon.setVisibility(View.VISIBLE);
		loadingIcon.startAnimation(ad);
	}

	public void dismissLoading() {
		ad.cancel();
		ad.reset();
		loadingIcon.setVisibility(View.INVISIBLE);
	}

	private void initHandler() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_DEVICE_REMOVED: {
					if (simpleAdapter != null) {
						simpleAdapter.clearSelected();
						isKeyBack = false;
						new Thread(new Runnable() {
							public void run() {
								firstShowInList();
								loading = true;
								handler.sendEmptyMessage(MSG_STARTANIMATION);
								getFileList(rootPath);
								loading = false;
								handler.sendEmptyMessage(MSG_DISSMISSANIMATION);
								if (!isKeyBack) {
									handler.sendEmptyMessage(MSG_NORMAL_REFRESH);
								}
							}
						}).start();
					}
				}
					break;
				case MSG_NORMAL_REFRESH:
					showInList();
					dmslist.setSelection(0);
					break;
				case MSG_STARTANIMATION:
					startLoading();
					break;
				case MSG_DISSMISSANIMATION:
					dismissLoading();
					break;
				case MSG_HINT_DISMISS:
					if(msg_hint.isShowing()){
						msg_hint.dismiss();
					}
					break;
				case MSG_SELECT_ITEM_REFRESH:
					simpleAdapter.notifyDataSetChanged(selectedItem);
					break;
				case MSG_SHOW_WOL_SENDED_HINT:
					if(dailog.isShowing()){
						dailog.dismiss();
						//showWOLSendingMsg();
					}
					break;
				}
			}
		};
	}

	private OnItemClickListener menuItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if (position == 0) // update
			{
				for(int i = 0; i < serverUDN.size(); i++){
					String chosenUDN = serverUDN.get(i);
					DLNADataProvider.mediaServerDelete(chosenUDN);
				}
				DLNADataProvider.mediaServerSearchScan();
				Message msg = new Message();
				msg.what = MSG_DEVICE_REMOVED;
				handler.sendMessage(msg);
				simpleAdapter.notifyDataSetChanged();
			} else if (position == 1) // register
			{
				// DeviceWakeUpper.send(macAddress);
				if(selectedItem > 0){
					final DLNAFileInfo info = listItems.get(selectedItem);
					if (deviceInfo.findDevice(info.getFileName()) < 0) {
						new Thread(new Runnable() {
							public void run() {
								String mac = null;
								while(mac == null){
									mac = mTv.getMacAddrByIP(info.getIp());
								}
								Log.e(TAG, "DEVICE NAME = "+info.getFileName());
								deviceInfo.addDeviceInfo(info.getFileName(), info.getIp(),
										mac, info.getPort(), info.getIsLive(),
										info.getUserName(), info.getPassWord());
								deviceInfo.writeDeviceInfo();
							}
						}).start();
					}
				}
			}else{
				ComponentName componetName = new ComponentName("com.android.emanualreader",
						"com.android.emanualreader.MainActivity");
				Intent intent = new Intent();
				intent.setComponent(componetName);
				startActivity(intent);
			}
			menu.dismiss();
		}

	};

	private OnClickListener buttonClicklistener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == setting) {
				menu.showMenu(82, 138);
			}
		}

	};

	private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v,
				final int position, long id) {
			selectedItem = position;
			handler.sendEmptyMessage(MSG_SELECT_ITEM_REFRESH);
			return true;
		}

	};

	private OnItemClickListener itemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v,
				final int position, long id) {
			if(listItems.get(position).getIsDeviceWake()){
				mediaApp.setSubRootPath(rootPath
						+ listItems.get(position).getFileName());
				mediaApp.setMediaServerName(listItems.get(position).getFileName());
				mediaApp.setMediaServerUUID(DLNADataProvider.getMediaServerUDN(position));
				String ip = DLNADataProvider.getServerUrl(position);
				ip = ip.substring(7);
				ip = ip.substring(0, ip.indexOf(":"));
				mediaApp.setMediaServerIP(ip);
				if (listItems.get(position).getIsLive() == 1) {
					if (source.compareTo("video") == 0) {
						ComponentName componetName = null;
						componetName = new ComponentName("com.rtk.dmp",
								"com.rtk.dmp.LiveDMSPlayBack");
						Intent intent = new Intent();
						Bundle bundle = new Bundle();
						bundle.putString("deviceName", listItems.get(position)
								.getFileName());
						bundle.putString("ip", listItems.get(position).getIp());
						bundle.putString("port", listItems.get(position).getPort());
						bundle.putString("userName", listItems.get(position)
								.getUserName());
						bundle.putString("password", listItems.get(position)
								.getPassWord());
						bundle.putBoolean("isRegister", listItems.get(position)
								.getIsRegister());
						intent.putExtras(bundle);
						intent.setComponent(componetName);
						startActivity(intent);
					} else {
						sendIntent();
					}
				} else {
					sendIntent();
				}
			}else{
				Log.e(TAG, "wakeOnLanQuery");
				wakeOnLanQuery(listItems.get(position).getFileName());
			}
		}
	};

	private void captureIntent(Intent intent) {
		source = intent.getStringExtra("source");
		if (source.compareTo("photo") == 0) {
			sourceType.setText(mContext.getResources().getString(
					R.string.title_photo));
		} else if (source.compareTo("audio") == 0) {
			sourceType.setText(mContext.getResources().getString(
					R.string.title_music));
		} else if (source.compareTo("video") == 0) {
			sourceType.setText(mContext.getResources().getString(
					R.string.title_movie));
		}
	}

	private void sendIntent() {
		ComponentName componetName = null;
		if (source.compareTo("photo") == 0) {
			componetName = new ComponentName("com.rtk.dmp",
					"com.rtk.dmp.GridViewActivity");
		} else if (source.compareTo("audio") == 0) {
			componetName = new ComponentName("com.rtk.dmp",
					"com.rtk.dmp.AudioBrowser");
		} else if (source.compareTo("video") == 0) {
			componetName = new ComponentName("com.rtk.dmp",
					"com.rtk.dmp.VideoBrowser");
		}
		if (componetName != null) {
			Intent intent = new Intent();
			intent.setComponent(componetName);
			startActivity(intent);
		}
	}
	
	private void wakeOnLanQuery(final String deviceName){
		dailog.setMessage(mContext.getResources().getString(R.string.wol_sending_confirmation));
		dailog.confirm_yes.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				new Thread() {
					public void run() {
						Log.v(TAG, deviceName+"  mac = "+deviceInfo.findDeviceMac(deviceName));
						DeviceWakeUpper.send(deviceInfo.findDeviceMac(deviceName));
						handler.sendEmptyMessage(MSG_SHOW_WOL_SENDED_HINT);					
					}
				}.start();
			}
			
		});
		
		dailog.confirm_yes.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					switch(keyCode)
					{
					case KeyEvent.KEYCODE_BACK:
					{
						dailog.dismiss();
					}
					break;
					default:
					break;
					}
				}
				return false;
			}
			
		});
		
		dailog.confirm_yes.setOnFocusChangeListener(new OnFocusChangeListener(){

			@Override
			public void onFocusChange(View arg0, boolean arg1) {
				new Thread() {
					public void run() {
						Log.v(TAG, deviceName+"  mac = "+deviceInfo.findDeviceMac(deviceName));
						DeviceWakeUpper.send(deviceInfo.findDeviceMac(deviceName));
						handler.sendEmptyMessage(MSG_SHOW_WOL_SENDED_HINT);					
					}
				}.start();
			}
		});
		
		dailog.confirm_no.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dailog.dismiss();
			}
			
		});
		
		dailog.confirm_no.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					switch(keyCode)
					{
					case KeyEvent.KEYCODE_BACK:
					{
						dailog.dismiss();
					}
					break;
					default:
					break;
					}
				}
				return false;
			}
			
		});
		dailog.show();
	}
	
	private void showWOLSendingMsg(){
		msg_hint.setMessage(mContext.getResources().getString(R.string.wol_sended_msg));
		msg_hint.show();
		
		if(task_msg_show_timeout != null)
		{
			task_msg_show_timeout.cancel();
			task_msg_show_timeout = null;
		}
		task_msg_show_timeout = new TimerTask(){

			@Override
			public void run() {
				handler.sendEmptyMessage(MSG_HINT_DISMISS);
			}
			
		};
		timer.schedule(task_msg_show_timeout, TimerDelay.delay_6s);
	}

	private synchronized void getFileList(String path) {
		if (listItems == null) {
			listItems = mediaApp.getDeviceList();
		}
		listItems.clear();
		
		if(serverUDN == null){
			serverUDN = new ArrayList<String>();
		}
		serverUDN.clear();
		if (path.equals(rootPath)) {
			int serversize = 0;
			// while(serversize < 1)
			// {
			serversize = DLNADataProvider.getServerSize();
			if (serversize == -1)
				return;
			// }

			for (int i = 0; i < serversize; i++) {
				String url = DLNADataProvider.getServerUrl(i);
				String ip = null;
				String port = null;
				int isLive = 0;
				if(url == null) {
					continue;
				}
				url = url.substring(7);
				ip = url.substring(0, url.indexOf(":"));
				if(url.contains("/"))
					port = url.substring(url.indexOf(":") + 1, url.indexOf("/"));
				else
					port = url.substring(url.indexOf(":") + 1);
				if (DLNADataProvider.getMediaServerRegzaApps(i) != null
						&& DLNADataProvider.getMediaServerRegzaApps(i).equals(
								"APPSCAP=8800"))
					isLive = 1;
				else
					isLive = 0;

				DLNAFileInfo finfo = new DLNAFileInfo(
						DLNADataProvider.getServerTitle(i), ip, port, isLive,
						true, false, "", "");
				listItems.add(finfo);
				serverUDN.add(DLNADataProvider.getMediaServerUDN(i));
			}

			int size = listItems.size();
			int deviceCount = deviceInfo.deviceList.size();
			boolean isExist = false;
			if (deviceCount > 0) {
				for (int i = 0; i < deviceCount; i++) {
					int j = 0;
					for (j = 0; j < size; j++) {
						String deviceName = listItems.get(j).getFileName();
						if (deviceInfo.getDeviceName(i).equals(deviceName)) {
							isExist = true;
							break;
						}
						isExist = false;
					}

					if (!isExist) {
						DLNAFileInfo finfo = new DLNAFileInfo(
								deviceInfo.getDeviceName(i),
								deviceInfo.getIp(i), deviceInfo.getPort(i),
								deviceInfo.getIsLive(i), false, true,
								deviceInfo.getUserName(i),
								deviceInfo.getpassword(i));
						listItems.add(finfo);
					} else {
						listItems.get(j).setIsRegister(true);
						listItems.get(j).setUserName(deviceInfo.getUserName(i));
						listItems.get(j).setPassWord(deviceInfo.getpassword(i));
					}

				}
			}
		}
	}

	private synchronized void firstShowInList() {
		if (listItems == null) {
			listItems = new ArrayList<DLNAFileInfo>();
		}
		listItems.clear();
		handler.sendEmptyMessage(MSG_NORMAL_REFRESH);
	}

	private void showInList() {
		if (simpleAdapter == null) {
			simpleAdapter = new FileListAdapter(this, listItems, dmslist);
			dmslist.setAdapter(simpleAdapter);
		} else
			simpleAdapter.notifyDataSetChanged();
	}

	public class setListAdapter extends BaseAdapter {
		public final class ViewHolder {
			TextView item_name;
		}

		int[] menu_name = new int[] { R.string.menu_update,
				R.string.menu_register, R.string.menu_help};

		private LayoutInflater mInflater;

		public setListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return menu_name.length;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder tag = null;

			if (convertView == null) {
				convertView = mInflater
						.inflate(R.layout.dms_setting_item, null);
				tag = new ViewHolder();
				tag.item_name = (TextView) convertView
						.findViewById(R.id.item_name);
				convertView.setTag(tag);
			} else {
				tag = (ViewHolder) convertView.getTag();
			}
			tag.item_name.setText(menu_name[position]);
			convertView.setBackgroundResource(R.drawable.menu_focus_select);

			return convertView;
		}

	}

	public class FileListAdapter extends BaseAdapter {
		private List<DLNAFileInfo> theItems;
		private GridView listView;
		private LayoutInflater mInflater;
		private int selected = -1;
		private int focused = -1;
		private boolean isLongClick = false;

		public FileListAdapter(Context context, List<DLNAFileInfo> mData,
				GridView listView) {
			mInflater = LayoutInflater.from(context);
			this.theItems = mData;
			this.listView = listView;
		}

		public void notifyDataSetChanged(int id) {
			isLongClick = true;
			selected = id;
			super.notifyDataSetChanged();
		}
		
		public void notifyDataSetChanged() {
			isLongClick = false;
			selectedItem = -1;
			super.notifyDataSetChanged();
		}

		public void notifyFocused(int id) {
			focused = id;
			super.notifyDataSetChanged();
		}

		public void clearSelected() {
			selected = -1;
		}

		@Override
		public int getCount() {

			return theItems.size();
		}

		@Override
		public Object getItem(int position) {

			return null;

		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		public final class ViewHolder {
			ImageView checkbox;
			ImageView imageView;
			TextView title;
			boolean isSelected;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(theItems.size() > 0){
				ViewHolder tag = null;
				final DLNAFileInfo info = theItems.get(position);
				boolean isSelected = false;
				boolean isFocus = false;
				if (selected == position) {
					isSelected = true;
				}
				if (focused == position) {
					isFocus = true;
				}
				if (convertView == null) {
					tag = new ViewHolder();
					convertView = mInflater.inflate(R.layout.dmscell, null);
					tag.checkbox = (ImageView) convertView
							.findViewById(R.id.ItemSelect);
					tag.imageView = (ImageView) convertView
							.findViewById(R.id.ItemImage);
					tag.title = (TextView) convertView.findViewById(R.id.ItemTitle);
					convertView.setTag(tag);
				} else {
					tag = (ViewHolder) convertView.getTag();
				}

				if (!info.getIsDeviceWake()) {
					convertView.setBackgroundResource(R.drawable.dnla_list_base);
					tag.imageView.setImageResource(R.drawable.dlna_device_icon_d);
				}

				if (isLongClick) {
					tag.checkbox.setVisibility(View.VISIBLE);
					if (isSelected) {
						tag.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_on));
					} else {
						tag.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_off));
					}
				} else {
					tag.checkbox.setVisibility(View.INVISIBLE);
				}

				if (isFocus) {
					tag.title.setEllipsize(TruncateAt.MARQUEE);
				} else {
					tag.title.setEllipsize(TruncateAt.END);
				}
				tag.title.setText(info.getFileName());
			}
			return convertView;
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		simpleAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mediaApp.deleteObserver(this);
	}

	@Override
	public synchronized void update(Observable o, Object arg) {
		Log.e(TAG, "update");
		ObserverContent content = (ObserverContent) arg;
		String serverName = content.getMsg();
		String act = content.getAction();
		if (act.equals(ObserverContent.ADD_DEVICE)) {
			if (DMSName.size() != 0) {
				for (int i = 0; i < DMSName.size(); i++) {
					if (DMSName.get(i).equals(serverName)) {
						DMSName.remove(i);
						break;
					}
				}
			}
			isChange = true;
			if(isActivityReady) {
				isChange = false;
				Message msg = new Message();
				msg.what = MSG_DEVICE_REMOVED;
				handler.sendMessage(msg);
			}
		} else if (act.equals(ObserverContent.REMOVE_DEVICE)) {
			isChange = true;
			if(isActivityReady) {
				isChange = false;
				Message msg = new Message();
				msg.what = MSG_DEVICE_REMOVED;
				handler.sendMessage(msg);
			}
		}else if(act.equals(ObserverContent.EXIT_APP)){
			finish();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus) {
			isActivityReady = true;
			if(isChange) {
				isChange = false;
				Message msg = new Message();
				msg.what = MSG_DEVICE_REMOVED;
				handler.sendMessage(msg);
			}
		} else {
			isActivityReady = false;
		}
	}
	
	
}
