package com.rtk.dmp;

import com.realtek.DLNA_DMP_1p5.HttpRequest;
import com.rtk.dmp.DMSListActivity.setListAdapter.ViewHolder;
import com.rtk.dmp.LiveVideoPlayerActivity.setListAdapter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class LiveDeviceSetting extends Activity {
	private String TAG = "LiveDeviceSetting";
	private DeviceInfo deviceInfo = null;
	private MediaApplication mediaApp = null;
	private PopupMessage msg_hint = null;
	
	private final int HIDE_POPUP_MESSAGE = 0;
	private final int SHOW_POPUP_MESSAGE = 1;

	private View device_setting = null;
	private View bt_right = null;
	private View bt_left = null;
	private View bt_bottom = null;
	private TextView tx_right = null;
	private GridView device_info = null;
	private DeviceInfoAdapter deviceinfoAdapter = null;
	private setListAdapter menuAdapter = null;
	private Menu menu = null;
	private Handler handler;
	
	String deviceName = "";
	String ip = "";
	String mac = "";
	String port = "";
	String userName = "";
	String password = "";

	private Activity mcontext = this;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "LiveDeviceSetting onCreate");
		setContentView(R.layout.devicesetting);
		super.onCreate(savedInstanceState);

		captureIntent();
		device_setting_view_init();

		mediaApp = (MediaApplication) getApplication();
		String path = getFilesDir().getPath();
		String fileName = path.concat("/DeviceInfo.bin");
		deviceInfo = mediaApp.getDeviceInfo(fileName);
		init_handler();
		msg_hint = new PopupMessage(mcontext);
		
		menuAdapter = new setListAdapter(this);
		menu = new Menu(this, menuAdapter);
		menu.setOnItemClickListener(menuItemClickListener);
	}

	private void device_setting_view_init() {
		device_setting = (View) findViewById(R.id.device_setting);
		bt_right = (View) device_setting.findViewById(R.id.v_right);
		bt_left = (View) device_setting.findViewById(R.id.v_left);
		bt_bottom = (View) device_setting.findViewById(R.id.v_bottom);
		tx_right = (TextView) device_setting.findViewById(R.id.v_right_txt);
		device_info = (GridView) device_setting.findViewById(R.id.device_info);
		deviceinfoAdapter = new DeviceInfoAdapter(this);
		device_info.setAdapter(deviceinfoAdapter);

		bt_right.setOnClickListener(buttonClicklistener);
		bt_left.setOnClickListener(buttonClicklistener);
		bt_bottom.setOnClickListener(buttonClicklistener);
	}
	
	private void init_handler(){
		handler = new Handler(){
        	@Override
    		public void handleMessage(Message msg) {
        		switch(msg.what){
        		case HIDE_POPUP_MESSAGE:
					if(msg_hint!=null && msg_hint.isShowing()){
						msg_hint.dismiss();
						msg_hint = null;
					}
        		break;
        		case SHOW_POPUP_MESSAGE:
        			if(msg_hint!=null && !msg_hint.isShowing()){
        				msg_hint.setMessage(mcontext.getResources().getString(R.string.network_connect_error));
            			msg_hint.show();
        			}
        			break;
        		default:
        			break;
        		}
        	}
        };
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.e(TAG, "keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK: {
				if(msg_hint!=null){
					msg_hint.dismiss();
					msg_hint = null;
				}
				Intent intent = new Intent();
				setResult(-1, intent);
			break;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private OnClickListener buttonClicklistener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == bt_right) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						String url = "http://"+ip+"/setup/s_chset.htm";
						String response = HttpRequest.sentGetMethod(url, userName,
								password);
						if(response == null){
							handler.sendEmptyMessage(SHOW_POPUP_MESSAGE);
		        			handler.sendEmptyMessageDelayed(HIDE_POPUP_MESSAGE, TimerDelay.delay_4s);
						}else{
							if(deviceInfo.findDevice(deviceName) < 0){
								deviceInfo.addDeviceInfo(deviceName, ip, mac, port, 1, userName, password);
								deviceInfo.writeDeviceInfo();
							}
							
							Intent intent = new Intent();
							setResult(0, intent);
							mcontext.finish();
						}
					}
					
				}).start();
			} else if (v == bt_left) {
				Intent intent = new Intent();
				setResult(-1, intent);
				mcontext.finish();
			} else if (v == bt_bottom) {
				
				ComponentName componetName = null;
				componetName = new ComponentName("com.rtk.dmp",
						"com.rtk.dmp.ChannelSetting");
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("ip", ip);
				bundle.putString("userName", userName);
				bundle.putString("password", password);
				intent.putExtras(bundle);
				intent.setComponent(componetName);
				startActivity(intent);
			}
		}

	};
	
	private void captureIntent(){
		Intent intent= getIntent();
		deviceName = intent.getStringExtra("deviceName");
		ip = intent.getStringExtra("ip");
		mac = intent.getStringExtra("mac");
		port = intent.getStringExtra("port");
		userName = intent.getStringExtra("userName");
		password = intent.getStringExtra("password");
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
	
	public class setListAdapter extends BaseAdapter {
		public final class ViewHolder {
			TextView item_name;
		}

		int[] menu_name = new int[] {R.string.menu_setting, R.string.menu_help};

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

	private class DeviceInfoAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private int selected = -1;
		private boolean focusable = false;

		public DeviceInfoAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public void notifyDataSetChanged(int id) {
			this.selected = id;
			super.notifyDataSetChanged();
		}

		public void clearSelected() {
			selected = -1;
		}

		@Override
		public int getCount() {
			return 7;
		}

		@Override
		public Object getItem(int position) {

			return null;

		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		public final class ViewHolder_TX1 {
			TextView item_name;
		}
		
		public final class ViewHolder_TX2 {
			TextView item_name;
			TextView item_value;
		}
		
		public final class ViewHolder_TX_ED {
			TextView item_name;
			EditText item_value;
		}
		
		ViewHolder_TX1 tag1 = new ViewHolder_TX1();
		ViewHolder_TX2 tag2 = new ViewHolder_TX2();
		ViewHolder_TX_ED tag3 = new ViewHolder_TX_ED();
		ViewHolder_TX_ED tag4 =new ViewHolder_TX_ED();
		
		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			if (convertView == null) {
				if (position == 0) {
					convertView = mInflater.inflate(R.layout.one_tx_item,
							null);
					tag1.item_name = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag1.item_name.setText(deviceName);
					convertView.setTag(tag1);
				} else if (position == 1) {	
					convertView = mInflater.inflate(R.layout.two_tx_item,
							null);
					tag2.item_name = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag2.item_value = (TextView)convertView.findViewById(R.id.ItemValue);
					tag2.item_name.setText(mcontext.getResources().getString(R.string.device_name));
					tag2.item_value.setText(deviceName);
					convertView.setTag(tag2);
				} else if (position == 2) {
					convertView = mInflater.inflate(R.layout.tx_edtx_item,
							null);
					tag3.item_name = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag3.item_value = (EditText)convertView.findViewById(R.id.ItemEdit);
					tag3.item_name.setText(mcontext.getResources().getString(R.string.user_name));
					
					tag3.item_value.addTextChangedListener(new TextWatcher() {

						@Override
						public void afterTextChanged(Editable edit) {
							userName = edit.toString();
							Log.e(TAG, "userName = "+userName);
						}

						@Override
						public void beforeTextChanged(CharSequence arg0,
								int arg1, int arg2, int arg3) {
						}

						@Override
						public void onTextChanged(CharSequence arg0, int arg1,
								int arg2, int arg3) {
						}
						
					});
					
					convertView.setTag(tag3);
				} else if (position == 3) {
					convertView = mInflater.inflate(R.layout.tx_edtx_item,
							null);
					tag4.item_name = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag4.item_value = (EditText)convertView.findViewById(R.id.ItemEdit);
					tag4.item_name.setText(mcontext.getResources().getString(R.string.password));
					tag4.item_value.setTransformationMethod(PasswordTransformationMethod.getInstance());
					tag4.item_value.addTextChangedListener(new TextWatcher() {

						@Override
						public void afterTextChanged(Editable edit) {
							password = edit.toString();
							Log.e(TAG, "password = "+password);
						}

						@Override
						public void beforeTextChanged(CharSequence arg0,
								int arg1, int arg2, int arg3) {
						}

						@Override
						public void onTextChanged(CharSequence arg0, int arg1,
								int arg2, int arg3) {
						}
						
					});
					convertView.setTag(tag4);
				} else if (position == 4) {
					convertView = mInflater.inflate(R.layout.two_tx_item,
							null);
					tag2.item_name = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag2.item_value = (TextView)convertView.findViewById(R.id.ItemValue);
					tag2.item_name.setText(mcontext.getResources().getString(R.string.ip_address));
					tag2.item_value.setText(ip);
					convertView.setTag(tag2);
				} else if (position == 5) {
					convertView = mInflater.inflate(R.layout.two_tx_item,
							null);
					tag2.item_name = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag2.item_value = (TextView)convertView.findViewById(R.id.ItemValue);
					tag2.item_name.setText(mcontext.getResources().getString(R.string.mac_address));
					tag2.item_value.setText(mac);
					convertView.setTag(tag2);
				} else if (position == 6) {
					convertView = mInflater.inflate(R.layout.two_tx_item,
							null);
					tag2.item_name = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag2.item_value = (TextView)convertView.findViewById(R.id.ItemValue);
					tag2.item_name.setText(mcontext.getResources().getString(R.string.port_no));
					tag2.item_value.setText(port);
					convertView.setTag(tag2);
				}
			}

			return convertView;
		}
	}
}
