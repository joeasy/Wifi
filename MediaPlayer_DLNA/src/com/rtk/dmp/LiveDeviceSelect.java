package com.rtk.dmp;

import java.util.ArrayList;

import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.Utils.DLNAFileInfo;
import com.rtk.dmp.LiveVideoPlayerActivity.setListAdapter;

import android.app.Activity;
import android.app.TvManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class LiveDeviceSelect extends Activity{
	private String TAG = "DeviceSetting";
	
	private final static String rootPath = "/http://upnp/";
	
	private GridView device_list = null;
	private View bt_right = null;
	private View bt_left = null;
	private TextView tx_right = null;
	private TextView tx_left = null;
	private ImageButton setting = null;
	private FileListAdapter liveAdapter = null;
	private View device_setting = null;
	private DeviceInfo deviceInfo = null;
	private ArrayList<DLNAFileInfo> listItems = null;
	private MediaApplication mediaApp = null;
	private Activity mcontext = this;
	private setListAdapter menuAdapter = null;
	private Menu menu = null;
	private TvManager mTv = null;

	private int selectItemNum = 0;
	
	public class LiveDevice {
		String deviceName;
		String ip;
		String mac;
		String port;
		String userName;
		String password;
		boolean isRegister;
	}
	
	public ArrayList<LiveDevice> livelist = new ArrayList<LiveDevice>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "DeviceSetting onCreate");
		setContentView(R.layout.device_select);
		super.onCreate(savedInstanceState);
		
		mTv = (TvManager) getSystemService("tv");
		
		mediaApp = (MediaApplication) getApplication();
		String path = getFilesDir().getPath();
        String fileName = path.concat("/DeviceInfo.bin");
        deviceInfo = mediaApp.getDeviceInfo(fileName);
        listItems = mediaApp.getDeviceList();
        getLiveDeviceList();
        
        device_setting_view_init();
        
        menuAdapter = new setListAdapter(this);
		menu = new Menu(this, menuAdapter);
		menu.setOnItemClickListener(menuItemClickListener);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		getFileList(rootPath);
		getLiveDeviceList();
		liveAdapter.notifyDataSetChanged();
	}
	
	private void device_setting_view_init() {
		device_setting = (View) findViewById(R.id.device_setting);
		bt_right = (View) device_setting.findViewById(R.id.v_right);
		bt_left = (View) device_setting.findViewById(R.id.v_left);
		tx_right = (TextView) device_setting.findViewById(R.id.v_right_txt);
		tx_left = (TextView) device_setting.findViewById(R.id.v_left_txt);
		device_list = (GridView) device_setting.findViewById(R.id.device_list);
		liveAdapter = new FileListAdapter(this);
		device_list.setAdapter(liveAdapter);
		setting = (ImageButton) findViewById(R.id.setting);
		setting.setOnClickListener(buttonClicklistener);
		bt_right.setOnClickListener(buttonClicklistener);
		bt_left.setOnClickListener(buttonClicklistener);
		
		device_list.setOnItemClickListener(deviceItemClickListener);
	}
	
	
	private void getLiveDeviceList(){
		
		livelist.clear();
        int listSize = listItems.size();
        LiveDevice list = new LiveDevice();
		for(int i = 0; i < listSize; i++){
	        	if(listItems.get(i).getIsLive() == 1){
	        		list.deviceName = listItems.get(i).getFileName();
	        		list.ip = listItems.get(i).getIp();
	        		list.mac = mTv.getMacAddrByIP(list.ip);
	        		/*while(list.mac == null){
	        			list.mac = mTv.getMacAddrByIP(list.ip);
	        		}*/
	        		list.port = listItems.get(i).getPort();
	        		list.userName = listItems.get(i).getUserName();
	        		list.password = listItems.get(i).getPassWord();
	        		list.isRegister = listItems.get(i).getIsRegister();
	        		livelist.add(list);
	        	}
	     }
	}
	
	private OnClickListener buttonClicklistener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(v == bt_right){
				if(tx_right.getText().toString().equals(mcontext.getString(R.string.delete))){
					if(selectItemNum < livelist.size() && selectItemNum >= 0){
						livelist.remove(selectItemNum);
						liveAdapter.notifyDataSetChanged(false);
						deviceInfo.removeDevice(selectItemNum);
						deviceInfo.writeDeviceInfo();
					}else{
						liveAdapter.notifyDataSetChanged(false);
						tx_right.setText(mcontext.getString(R.string.update));
						tx_left.setText(mcontext.getString(R.string.delete));
					}
				}else if(tx_right.getText().toString().equals(mcontext.getString(R.string.update))){
					getFileList(rootPath);
					getLiveDeviceList();
					liveAdapter.notifyDataSetChanged();
				}
			}else if(v == bt_left){
				if(tx_left.getText().toString().equals(mcontext.getString(R.string.delete))){
					liveAdapter.notifyDataSetChanged(true);
				}else if(tx_left.getText().toString().equals(mcontext.getString(R.string.cancel))){
					liveAdapter.notifyDataSetChanged(false);
					tx_right.setText(mcontext.getString(R.string.update));
					tx_left.setText(mcontext.getString(R.string.delete));
				}
			}else if (v == setting) {
				menu.showMenu(82, 138);
			}
		}
		
	};
	
	private OnItemClickListener deviceItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			selectItemNum = position;
			liveAdapter.notifyDataSetChanged(position, true);
			tx_right.setText(mcontext.getString(R.string.delete));
			tx_left.setText(mcontext.getString(R.string.cancel));
		}
		
	};
	
	private void getFileList(String path) {
		if (listItems == null) {
			listItems = mediaApp.getDeviceList();
		}
		listItems.clear();

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
				url = url.substring(7);
				ip = url.substring(0, url.indexOf(":"));
				if(url.contains("/"))
					port = url.substring(url.indexOf(":") + 1, url.indexOf("/"));
				else
					port = url.substring(url.indexOf(":") + 1);
				//mac = mTv.getMacAddrByIP(ip);
				if (DLNADataProvider.getMediaServerRegzaApps(i) != null
						&& DLNADataProvider.getMediaServerRegzaApps(i).equals(
								"APPSCAP=8800"))
					isLive = 1;
				else
					isLive = 0;
				
				DLNAFileInfo finfo = new DLNAFileInfo(
						DLNADataProvider.getServerTitle(i), ip, port,
						isLive, true, false, "", "");
				listItems.add(finfo);
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
								deviceInfo.getIp(i),
								deviceInfo.getPort(i), deviceInfo.getIsLive(i),
								false, true, deviceInfo.getUserName(i), deviceInfo.getpassword(i));
						listItems.add(finfo);
					}else{
						listItems.get(j).setIsRegister(true);
						listItems.get(j).setUserName(deviceInfo.getUserName(i));
						listItems.get(j).setPassWord(deviceInfo.getpassword(i));
					}

				}
			}
		}
	}
	
	private void callLiveDeviceSetting(int position){
		ComponentName componetName = null;
		componetName = new ComponentName("com.rtk.dmp",
				"com.rtk.dmp.LiveDeviceSetting");
		Intent intent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putString("deviceName", livelist.get(position).deviceName);
		bundle.putString("ip", livelist.get(position).ip);
		bundle.putString("mac", livelist.get(position).mac);
		bundle.putString("port", livelist.get(position).port);
		bundle.putString("userName", livelist.get(position).userName);
		bundle.putString("password", livelist.get(position).password);
		intent.putExtras(bundle);
		intent.setComponent(componetName);
		
		startActivity(intent);
	}
	
	private OnItemClickListener menuItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if (position == 0) {
				menu.dismiss();
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
	
	private class FileListAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private int selected = -1;
		private boolean show_check = false;

		public FileListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}
		
		public void notifyDataSetChanged(boolean show) {
			show_check = show;
			super.notifyDataSetChanged();
		}

		public void notifyDataSetChanged(int id, boolean show) {
			selected = id;
			show_check = show;
			super.notifyDataSetChanged();
		}

		public void clearSelected() {
			selected = -1;
		}

		@Override
		public int getCount() {

			return livelist.size();
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
			TextView device_name;
			ImageView register_status;
			boolean isSelected;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder tag = null;
			boolean isSelected = false;
			if (selected == position) {
				isSelected = true;
			}
			if (convertView == null) {
				tag = new ViewHolder();
				convertView = mInflater.inflate(R.layout.live_dms_cell,
						null);
				tag.checkbox = (ImageView) convertView.findViewById(R.id.check);
				tag.device_name = (TextView) convertView
						.findViewById(R.id.device_name);
				tag.register_status = (ImageView) convertView.findViewById(R.id.register);
				tag.isSelected = false;
				convertView.setTag(tag);
				
				tag.register_status.setOnClickListener(new OnClickListener() {  
                    @Override  
                    public void onClick(View v) {  
                        callLiveDeviceSetting(position);
                        return ;  
                    }  
                });  
			} else {
				tag = (ViewHolder) convertView.getTag();
			}
			
			tag.device_name.setText(livelist.get(position).deviceName);
			
			if(livelist.get(position).isRegister){
				tag.register_status.setImageResource(R.drawable.dlna_livestream_icon_device_setting_f);
			}else{
				tag.register_status.setImageResource(R.drawable.dlna_livestream_icon_device_setting_n);
			}
			
			if (show_check) {
				tag.checkbox.setVisibility(View.VISIBLE);
				if (isSelected) {
					if(tag.isSelected){
						tag.isSelected = false;
						tag.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_off));
					}else{
						tag.isSelected = true;
						tag.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_on));
					}
				} else {
					if(tag.isSelected){
						tag.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_on));
					}else{
						tag.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_off));
					}
				}
			} else {
				tag.checkbox.setVisibility(View.INVISIBLE);
			}                                                                                                                                                                                                        

			return convertView;
		}

	}

}
