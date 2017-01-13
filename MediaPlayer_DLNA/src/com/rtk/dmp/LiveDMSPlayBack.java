package com.rtk.dmp;

import java.util.ArrayList;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.TvManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.realtek.DLNA_DMP_1p5.HttpRequest;
import com.realtek.DataProvider.DLNADataProvider;
import com.rtk.dmp.ChannelInfo.ChannelAtrr;
import com.realtek.Utils.ChannelAttr;

public class LiveDMSPlayBack extends Activity {
	private String TAG = "LiveDMSPlayBack";
	private boolean loading = false;
	
	private final int UPDATE_CHANNEL = 0;
	private final int TIMER_CANCEL = 1;
	private final int START_LOADING = 2;
	private final int STOP_LOADING = 3;
	private final int HIDE_POPUP_MESSAGE = 4;
	private final int SHOW_POPUP_MESSAGE = 5;
	
	private final int SHOW_CHANNEL_INFO =0;

	private ImageView normal = null;
	private ImageView live = null;
	private ImageButton setting = null;
	private TextView device_Name = null;
	private GridView channel_list = null;
	private ImageView loadingIcon = null;

	private setListAdapter menuAdapter = null;
	private setListAdapter channelAdapter = null;
	private Menu menu = null;

	private View mode_select = null;
	private View channel_select = null;
	private View cs = null;
	private View bs = null;
	private View land = null;

	private enum viewMode {
		MENU, CHANNEL_SELETE;
	}
	
	String deviceName = "";
	String ip = "";
	String mac = "";
	String port = "";
	String userName = "";
	String password = "";
	boolean isRegister = false;
	
	private Timer timer = null;
	private TimerTask task_getMac = null; 

	private Handler handler = null;
	private ArrayList<ChannelAtrr> channel = null;
	private ArrayList<ChannelAtrr> curr_channel = null;
	private ChannelInfo channelInfo = null;
	private MediaApplication mediaApp = null;
	private Activity mcontext = this;
	private Animation ad = null;
	private DeviceInfo deviceInfo = null;
	private TvManager mTv = null;
	private PopupMessage msg_hint = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "LiveDMSPlayBack onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.livedmsplayback);

		mediaApp = (MediaApplication) getApplication();
		setting = (ImageButton) findViewById(R.id.setting);
		setting.setOnClickListener(buttonClicklistener);

		mTv = (TvManager) getSystemService("tv");
		String path = getFilesDir().getPath();
		String fileName = path.concat("/ChannelInfo.bin");
		channelInfo = mediaApp.getChannelInfo(fileName);
		channel = new ArrayList<ChannelAtrr>();
		curr_channel = mediaApp.getChannelAtrrList();
		msg_hint = new PopupMessage(mcontext);

		mode_select_view_init();
		channel_select = (View) findViewById(R.id.channel_select);

		initHandler();

		menuAdapter = new setListAdapter(this, viewMode.MENU);
		menu = new Menu(this, menuAdapter);
		menu.setOnItemClickListener(menuItemClickListener);
		
		fileName = path.concat("/DeviceInfo.bin");
		deviceInfo = mediaApp.getDeviceInfo(fileName);
		
		captureIntent();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 0) {
			if(resultCode == SHOW_CHANNEL_INFO) {
				if (channel_list == null)
					channel_select_view_init();
				startLoading();
				channel_list_show();
			}
		}else{
			
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(timer != null){
			timer.cancel();
			timer = null;
		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		if(deviceInfo.deviceListLength() > 0){
			for(int i = 0; i<deviceInfo.deviceListLength(); i++){
				if(deviceName.equals(deviceInfo.getDeviceName(i))){
					isRegister = true;
					break;
				}
				isRegister = false;
			}
		}else{
			isRegister = false;
		}
		
		if(mac == null || mac.equals("")){
			if(timer == null) {
				timer = new Timer();
			}
		}
		
		if(channel_select.isShown()){
			startLoading();
			channel_list_show();
		}
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.e(TAG, "keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK: {
				if(channel_select != null && channel_select.isShown()) {
					dismissLoading();
					channel_select.setVisibility(View.GONE);
					mode_select.setVisibility(View.VISIBLE);
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void captureIntent(){
		Intent intent = getIntent();
		deviceName = intent.getStringExtra("deviceName");
		ip = intent.getStringExtra("ip");
		port = intent.getStringExtra("port");
		userName = intent.getStringExtra("userName");
		password = intent.getStringExtra("password");
		isRegister = intent.getBooleanExtra("isRegister", false);
		
		if(timer == null)
			timer = new Timer();
		
		if(task_getMac == null){
			task_getMac = new TimerTask(){

				@Override
				public void run() {
					if(mac == null || mac.equals("")){
						mac = mTv.getMacAddrByIP(ip);
					}else{
						handler.sendEmptyMessage(TIMER_CANCEL);
					}
				}
				
			};
		}
		
		timer.schedule(task_getMac, 0, 500);
	}

	private void initHandler() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case UPDATE_CHANNEL:
					channelAdapter
							.notifyDataSetChanged(viewMode.CHANNEL_SELETE);
					break;
				case TIMER_CANCEL:
					if(timer != null){
						timer.cancel();
						timer = null;
					}
					break;
				case START_LOADING:
					startLoading();
					break;
				case STOP_LOADING:
					dismissLoading();
					break;
				case HIDE_POPUP_MESSAGE:
					if(msg_hint!=null && msg_hint.isShowing()){
						msg_hint.dismiss();
						msg_hint = null;
					}
        		break;
        		case SHOW_POPUP_MESSAGE:
        			msg_hint.setMessage(mcontext.getResources().getString(R.string.network_connect_error));
        			msg_hint.show();
        			break;
				default:
					break;
				}
			}
		};
	}

	private void mode_select_view_init() {
		mode_select = (View) findViewById(R.id.mode_select);
		normal = (ImageView) mode_select.findViewById(R.id.bt_normal);
		live = (ImageView) mode_select.findViewById(R.id.bt_live);
		device_Name = (TextView) mode_select.findViewById(R.id.device_name);
		device_Name.setText(mediaApp.getMediaServerName());

		live.setOnClickListener(buttonClicklistener);
		normal.setOnClickListener(buttonClicklistener);
	}

	private void channel_select_view_init() {
		channel_list = (GridView) channel_select
				.findViewById(R.id.channel_list);
		cs = (View)channel_select.findViewById(R.id.v_cs);
		bs = (View)channel_select.findViewById(R.id.v_bs);
		land = (View)channel_select.findViewById(R.id.v_land);
		channelAdapter = new setListAdapter(this, viewMode.CHANNEL_SELETE);
		channel_list.setAdapter(channelAdapter);
		channel_list.setOnItemClickListener(channelItemClickListener);
		loadingIcon = (ImageView) findViewById(R.id.loadingIcon);
		
		cs.setOnClickListener(buttonClicklistener);
		bs.setOnClickListener(buttonClicklistener);
		land.setOnClickListener(buttonClicklistener);
		
		ad = AnimationUtils.loadAnimation(this, R.drawable.video_anim);
		ad.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (loading) {
					loadingIcon.startAnimation(ad);
				}
			}
		});
	}
	
	public void startLoading() {
		loadingIcon.setVisibility(View.VISIBLE);
		loadingIcon.startAnimation(ad); 
		loading = true;
	}

	public void dismissLoading() {
		ad.cancel();
		ad.reset();
		loadingIcon.setVisibility(View.INVISIBLE);
		loading = false;
	}

	private OnClickListener buttonClicklistener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == live) {
				if(isRegister){
					if(channelInfo.channleListLength() > 0){
						Log.e(TAG, "channelInfo.channleListLength() is "+channelInfo.channleListLength());
						channel.clear();
						ChannelAtrr attr = channelInfo.attr;
						for(int i = 0; i < channelInfo.channleListLength(); i++){
							if(channelInfo.channleList.get(i).deviceName.equals(mediaApp.getMediaServerName())){
								attr.channelName = channelInfo.channleList.get(i).channelName;
								attr.broadType = channelInfo.channleList.get(i).broadType;
								attr.sid = channelInfo.channleList.get(i).sid;
								attr.nid = channelInfo.channleList.get(i).nid;
								channel.add(attr);
							}
						}
						if(channel.size() > 0){
							int len = channel.size();
							for(int i = 0; i < len; i++)
								curr_channel.add(channel.get(i));
							play_video(0);
						}else{
							play_default_video();
						}
					}else{
						Log.e(TAG, "channelInfo.channleListLength() < 0");
						play_default_video();
					}
				}else{
					callLiveDeviceSetting();
				}
			} else if (v == normal) {
				callVideoBrowser();
			} else if (v == setting) {
				menu.showMenu(82, 138);
			}else if(v == cs){
				cs.setBackgroundResource(R.drawable.dnla_sorting_base_up);
				bs.setBackgroundResource(R.drawable.dnla_sorting_base_down);
				land.setBackgroundResource(R.drawable.dnla_sorting_base_down);
				getChannelList("CS");
				handler.sendEmptyMessage(UPDATE_CHANNEL);
			}else if(v == bs){
				cs.setBackgroundResource(R.drawable.dnla_sorting_base_down);
				bs.setBackgroundResource(R.drawable.dnla_sorting_base_up);
				land.setBackgroundResource(R.drawable.dnla_sorting_base_down);
				getChannelList("BS");
				handler.sendEmptyMessage(UPDATE_CHANNEL);
			}else if(v == land){
				cs.setBackgroundResource(R.drawable.dnla_sorting_base_down);
				bs.setBackgroundResource(R.drawable.dnla_sorting_base_down);
				land.setBackgroundResource(R.drawable.dnla_sorting_base_up);
				getChannelList("");
				handler.sendEmptyMessage(UPDATE_CHANNEL);
			}
		}

	};

	private void callVideoBrowser() {
		ComponentName componetName = null;
		componetName = new ComponentName("com.rtk.dmp",
					"com.rtk.dmp.VideoBrowser");
		if (componetName != null) {
			Intent intent = new Intent();
			intent.setComponent(componetName);
			startActivity(intent);
		}
	}
	
	private void callLiveDeviceSetting(){
		ComponentName componetName = null;
		componetName = new ComponentName("com.rtk.dmp",
					"com.rtk.dmp.LiveDeviceSetting");
		if (componetName != null) {
			Intent intent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putString("deviceName", deviceName);
			bundle.putString("ip", ip); 
			bundle.putString("mac", mac);
			bundle.putString("port", port);
			bundle.putString("userName", userName);
			bundle.putString("password", password);
			intent.putExtras(bundle);
			intent.setComponent(componetName);
			startActivityForResult(intent, 0);
		}
	}

	private OnItemClickListener menuItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if (position == 0) {
				menu.dismiss();
				ComponentName componetName = new ComponentName("com.rtk.dmp",
						"com.rtk.dmp.LiveDeviceSelect");
				Intent intent = new Intent();
				intent.setComponent(componetName);
				startActivity(intent); 
			} else if (position == 1) {
				menu.dismiss();
				ComponentName componetName = new ComponentName("com.android.emanualreader",
						"com.android.emanualreader.MainActivity");
				Intent intent = new Intent();
				intent.setComponent(componetName);
				startActivity(intent); 
			}
		}

	};

	private OnItemClickListener channelItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, final int position,
				long id) {
			if(timer != null){
				timer.cancel();
				timer = null;
			}
			String service_id = Integer.toHexString(Integer.valueOf(curr_channel.get(position).nid));
			String network_id = Integer.toHexString(Integer.parseInt(curr_channel.get(position).sid));
			final String url = "http://" + mediaApp.getMediaServerIP()
					+ "/remote/livech.htm?channel=G" + network_id + service_id + "+0:00:00.00";
			Log.e(TAG, "url = "+url);
			new Thread(new Runnable() {

				@Override
				public void run() {
					HttpRequest.sentGetMethod(url);
					play_video(position);
				}
				
			}).start();
			
			// play this channel
		}
	};
	
	private void play_default_video(){
		if(curr_channel == null || curr_channel.isEmpty()){
			new Thread(new Runnable() {
				public void run() {
					String url = "http://"+ip+"/setup/s_chset.htm";
					Log.v(TAG, "userName = "+userName+" password ="+password);
					if(userName == null || password == null || userName.equals("") || password.equals("")){
						for(int i = 0; i<deviceInfo.deviceListLength(); i++){
							if(deviceName.equals(deviceInfo.getDeviceName(i))){
								userName = deviceInfo.getUserName(i);
								password = deviceInfo.getpassword(i);
							}
						}
					}
					Log.v(TAG, "userName = "+userName+" password ="+password);
					String response = HttpRequest.sentGetMethod(url, userName,
							password);
					if(response != null){
						channel.clear();
						channelInfo.extractString(response, channel);
						if(channel.size() > 0){
							for(ChannelAtrr attr: channel)
								curr_channel.add(attr);
							String service_id = Integer.toHexString(Integer.valueOf(curr_channel.get(0).nid));
							String network_id = Integer.toHexString(Integer.parseInt(curr_channel.get(0).sid));
							final String url1 = "http://" + mediaApp.getMediaServerIP()
									+ "/remote/livech.htm?channel=G" + network_id + service_id + "+0:00:00.00";
							Log.e(TAG, "url = "+url1);
							HttpRequest.sentGetMethod(url1);
							play_video(0);
						}else{
							Log.e(TAG, "live DMS has no channel!");
						}
					}else{
						Log.e(TAG, "live DMS has no reponding!");
					}
				}
			}).start();
		}else{
			new Thread(new Runnable() {
				public void run() {
					String service_id = Integer.toHexString(Integer.valueOf(curr_channel.get(0).nid));
					String network_id = Integer.toHexString(Integer.parseInt(curr_channel.get(0).sid));
					final String url1 = "http://" + mediaApp.getMediaServerIP()
							+ "/remote/livech.htm?channel=G" + network_id + service_id + "+0:00:00.00";
					Log.e(TAG, "url = "+url1);
					HttpRequest.sentGetMethod(url1);
					play_video(0);
				}
			}).start();
		}
		
	}
	
	private void play_video(int channel_index) {
		boolean upnpBrowserSuccess = false;
		int iItemSize = 0;
		upnpBrowserSuccess = DLNADataProvider.browseLiveServer(mediaApp.getMediaServerName(),"LIVE");
		Log.e(TAG, "upnpBrowserSuccess = "+upnpBrowserSuccess);
		if (upnpBrowserSuccess) {
			iItemSize = DLNADataProvider.getItemSize();
			Log.e(TAG, "iItemSize = "+iItemSize);
		} else {
			iItemSize = 0;
			return;
		}
		
		if (iItemSize > 0) {
			for (int i = 0; i < iItemSize; i++) {
				String itemCharID = DLNADataProvider.getItemCharID(i);
				Stack<ChannelAttr> attrStack = new Stack<ChannelAttr>();
				ArrayList<ChannelAttr> attrilist = mediaApp.getChannelAttrList();
				ArrayList<String>  uri = new ArrayList<String>();
				ArrayList<String>  res = new ArrayList<String>();
				ArrayList<String>  bitrate = new ArrayList<String>();
				ArrayList<String>  protolinfo = new ArrayList<String>();
				boolean bDTCP = false;
				
				uri = DLNADataProvider.queryResourceListByIndex(itemCharID, DLNADataProvider.UPNP_DMP_RES_URI);
				DLNADataProvider.queryResourceListByIndex(itemCharID, DLNADataProvider.UPNP_DMP_RES_PROTOCOLINFO);
				res = DLNADataProvider.queryResourceListByIndex(itemCharID, DLNADataProvider.UPNP_DMP_RES_RESOLUTION);
				bitrate = DLNADataProvider.queryResourceListByIndex(itemCharID, DLNADataProvider.UPNP_DMP_RES_BITRATE);
				protolinfo = DLNADataProvider.queryResourceListByIndex(itemCharID, DLNADataProvider.UPNP_DMP_RES_PROTOCOLINFO);
				int count = uri.size();
				attrilist.clear();
				for(int j = 0; j < count; j++) {
					ChannelAttr attri = new ChannelAttr();
					String tmpProtolinfo = protolinfo.get(j);
					String tmpFilePath = uri.get(j);
					if(tmpProtolinfo != null){
						bDTCP = tmpProtolinfo.contains("DTCP1HOST");
						if(!bDTCP){
							if(tmpFilePath != null && tmpFilePath.contains("?")){
								String tmpPath = tmpFilePath.substring(tmpFilePath.indexOf("?"));
								if(tmpPath.contains("CONTENTPROTECTIONTYPE=DTCP1")){
									bDTCP = true;
								}
							}
						}
						if(bDTCP){
							tmpProtolinfo = " protocolinfo=" + tmpProtolinfo.substring(0, tmpProtolinfo.lastIndexOf(":") + 1) + "*";
						}
					}
					
					if(DLNADataProvider.queryByteBasedSeekableofID(itemCharID))
						attri.setUri(tmpFilePath + " forceSeek forcerange" + tmpProtolinfo);
					else
						attri.setUri(tmpFilePath  + tmpProtolinfo);
					attri.setResolution(res.get(j));
					float rate = (float)Integer.valueOf(bitrate.get(j))*8/1000/1000;
					attri.setBitrate(String.valueOf(rate+"Mbps"));
					attri.setItemCharID(itemCharID);
					attrStack.push(attri);
				}
				
				while(!attrStack.isEmpty()){
					ChannelAttr attri = attrStack.pop();
					attrilist.add(attri);
				}
				
				ComponentName componetName = new ComponentName("com.rtk.dmp",
						"com.rtk.dmp.LiveVideoPlayerActivity");
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putInt("channel_index", channel_index);
				intent.putExtras(bundle);
				intent.setComponent(componetName);
				startActivityForResult(intent, 0);
			}
		}
	}

	private void channel_list_show() {
		mode_select.setVisibility(View.GONE);
		channel_select.setVisibility(View.VISIBLE);
		if(channelInfo.channleListLength() > 0){
			channel.clear();
			ChannelAtrr attr = channelInfo.attr;
			for(int i = 0; i < channelInfo.channleListLength(); i++){
				if(channelInfo.channleList.get(i).deviceName.equals(mediaApp.getMediaServerName())){
					attr.channelName = channelInfo.channleList.get(i).channelName;
					attr.broadType = channelInfo.channleList.get(i).broadType;
					attr.sid = channelInfo.channleList.get(i).sid;
					attr.nid = channelInfo.channleList.get(i).nid;
					channel.add(attr);
				}
			}
			if(channel.size() > 0){
				getChannelList("");
				dismissLoading();
				handler.sendEmptyMessage(UPDATE_CHANNEL);
			}else{
				httpGetChannelInfo();
			}
		}else{
			Log.e(TAG, "channelInfo.channleListLength() < 0");
			httpGetChannelInfo();
		}
	}

	private void httpGetChannelInfo() {
		new Thread(new Runnable() {
			public void run() {
				String url = "http://"+ip+"/setup/s_chset.htm";
				if(userName == null || password == null || userName.equals("") || password.equals("")){
					for(int i = 0; i<deviceInfo.deviceListLength(); i++){
						if(deviceName.equals(deviceInfo.getDeviceName(i))){
							userName = deviceInfo.getUserName(i);
							password = deviceInfo.getpassword(i);
						}
					}
				}
				Log.v(TAG, "userName = "+userName+" password ="+password);
				String response = HttpRequest.sentGetMethod(url, userName,
						password);
				if(response != null){
					channel.clear();
					channelInfo.extractString(response, channel);
					Log.v(TAG, "The size of Channel is "+channel.size());				
					getChannelList("");
					handler.sendEmptyMessage(STOP_LOADING);
					handler.sendEmptyMessage(UPDATE_CHANNEL);
				}
			}
		}).start();

	}
	
	private void getChannelList(String broadType){
		int length = channel.size();
		curr_channel.clear();
		if(broadType.contains("CS") || broadType.contains("BS")){
			for(int i = 0; i < length; i++){
				if(channel.get(i).broadType.contains(broadType)){
					curr_channel.add(channel.get(i));
				}
			}
		}else{
			for(int i = 0; i < length; i++){
				if(!channel.get(i).broadType.contains("CS") &&
					!channel.get(i).broadType.contains("BS")){
					curr_channel.add(channel.get(i));
				}
			}
		}
		
	}

	
	public class setListAdapter extends BaseAdapter {
		public final class ViewHolder {
			TextView item_name;
		}

		int[] menu_name = new int[] {R.string.menu_setting, R.string.menu_help };

		private LayoutInflater mInflater;
		private viewMode vmode;

		public setListAdapter(Context context, viewMode mode) {
			mInflater = LayoutInflater.from(context);
			vmode = mode;
		}

		public void notifyDataSetChanged(viewMode mode) {
			vmode = mode;
			super.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (vmode == viewMode.MENU)
				return menu_name.length;
			else if (vmode == viewMode.CHANNEL_SELETE)
				return curr_channel.size();
			else
				return 0;
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
				if (vmode == viewMode.MENU) {
					convertView = mInflater.inflate(R.layout.dms_setting_item,
							null);
					tag = new ViewHolder();
					tag.item_name = (TextView) convertView
							.findViewById(R.id.item_name);
					convertView.setTag(tag);
				} else if (vmode == viewMode.CHANNEL_SELETE) {
					convertView = mInflater
							.inflate(R.layout.channel_cell, null);
					tag = new ViewHolder();
					tag.item_name = (TextView) convertView
							.findViewById(R.id.item_name);
					convertView.setTag(tag);
				}

			} else {
				if (vmode == viewMode.MENU || vmode == viewMode.CHANNEL_SELETE) {
					tag = (ViewHolder) convertView.getTag();
				}
			}

			if (vmode == viewMode.MENU) {
				tag.item_name.setText(menu_name[position]);
				convertView.setBackgroundResource(R.drawable.menu_focus_select);
			} else if (vmode == viewMode.CHANNEL_SELETE) {
				if (curr_channel.size() > 0) {
					tag.item_name.setText(curr_channel.get(position).channelName);
				}
			}

			return convertView;
		}
	}
}
