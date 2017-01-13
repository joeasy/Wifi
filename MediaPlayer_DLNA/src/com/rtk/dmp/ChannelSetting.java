package com.rtk.dmp;

import java.util.ArrayList;

import com.realtek.DLNA_DMP_1p5.HttpRequest;
import com.rtk.dmp.ChannelInfo.ChannelAtrr;
import com.rtk.dmp.LiveDeviceSetting.setListAdapter;
import com.rtk.dmp.LiveDeviceSetting.setListAdapter.ViewHolder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ChannelSetting extends Activity {
	private String TAG = "ChannelSetting";

	private View bt_right = null;
	private View bt_bottom = null;
	private GridView channel_info = null;
	private FileListAdapter broadcastTypeAdapter = null;
	private FileListAdapter channelAdapter = null;
	private Activity mcontext = this;
	
	String ip = "";
	String userName = "";
	String password = "";
	
	private ChannelInfo channelInfo = null;
	private ArrayList<ChannelAtrr> channel = null;
	private ArrayList<TickIndex> tick = null;
	private MediaApplication mediaApp = null;
	private setListAdapter menuAdapter = null;
	private Menu menu = null;
	private class TickIndex{
		int index;
	}

	private enum viewMode {
		BROADCAST_TYPE, CHANNEL_LIST;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "ChannelSetting onCreate");
		setContentView(R.layout.channel_setting);
		super.onCreate(savedInstanceState);
		
		captureIntent();

		bt_right = (View)findViewById(R.id.v_right);
		bt_bottom = (View)findViewById(R.id.v_bottom);
		channel_info = (GridView) findViewById(R.id.channel_info);
		broadcastTypeAdapter = new FileListAdapter(this, viewMode.BROADCAST_TYPE);
		channelAdapter = new FileListAdapter(this, viewMode.CHANNEL_LIST);
		channel_info.setAdapter(broadcastTypeAdapter);

		bt_right.setOnClickListener(buttonClicklistener);
		bt_bottom.setOnClickListener(buttonClicklistener);
		channel_info.setOnItemClickListener(ItemClickListener);
		
		mediaApp = (MediaApplication) getApplication();
		String path = getFilesDir().getPath();
		String fileName = path.concat("/ChannelInfo.bin");
		channelInfo = mediaApp.getChannelInfo(fileName);
		channel = new ArrayList<ChannelAtrr>();
		tick = new ArrayList<TickIndex>();
		updateChannel();
		
		menuAdapter = new setListAdapter(this);
		menu = new Menu(this, menuAdapter);
		menu.setOnItemClickListener(menuItemClickListener);
	}
	
	private void captureIntent(){
		Intent intent= getIntent();
		ip = intent.getStringExtra("ip");
		userName = intent.getStringExtra("userName");
		password = intent.getStringExtra("password");
		
		Log.e(TAG, "captureIntent()  userName = "+userName+" password ="+password);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK: {
				if(!bt_bottom.isShown()){
					channel_info.setAdapter(broadcastTypeAdapter);
					bt_bottom.setVisibility(View.VISIBLE);
					return true;
				}
			}
			default:
				break;
		}
		return super.onKeyDown(keyCode, event);
	}


	private OnClickListener buttonClicklistener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(v == bt_bottom){
				updateChannel();
			}else{
				if(bt_bottom.isShown()){
					mcontext.finish();
				}else{
					channelInfo.cleanChannelList();
					channel_info.setAdapter(broadcastTypeAdapter);
					bt_bottom.setVisibility(View.VISIBLE);
					for(int i = 0; i< tick.size(); i++){
						int index = tick.get(i).index;
						channelInfo.addChannelInfo(mediaApp.getMediaServerName(), channel.get(index).channelName, channel.get(index).broadType, channel.get(index).sid, channel.get(index).nid);
					}
					channelInfo.writeChannelInfo();
				}
			}
		}
		
	};

	private OnItemClickListener ItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if(bt_bottom.isShown()){
				channel_info.setAdapter(channelAdapter);
				channelAdapter.notifyDataSetChanged();
				bt_bottom.setVisibility(View.GONE);
			}else{
				channelAdapter.notifyDataSetChanged(position);
			}
		}

	};
	
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
	
	private void updateChannel(){
		new Thread(new Runnable() {

			@Override
			public void run() {
				String url = "http://"+ip+"/setup/s_chset.htm";
				String response = HttpRequest.sentGetMethod(url, userName,
						password);
				Log.e(TAG, "userName = "+userName+" password ="+password);
				if(response != null){
					channel.clear();
					channelInfo.extractString(response, channel);
					Log.e(TAG, "channel size = "+channel.size());
				}
			}
			
		}).start();
	}
	
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
		
		private viewMode vmode = viewMode.CHANNEL_LIST;

		String[] broadcastType = new String[] {
				mcontext.getResources().getString(R.string.channel_land),
				mcontext.getResources().getString(R.string.channel_bs),
				mcontext.getResources().getString(R.string.channel_cs) };

		public FileListAdapter(Context context, viewMode mode) {
			mInflater = LayoutInflater.from(context);
			vmode = mode;
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
			if(vmode == viewMode.BROADCAST_TYPE){
				return broadcastType.length;
			}else{
				
				return channel.size();
			}
		}

		@Override
		public Object getItem(int position) {

			return null;

		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		private class ViewBroadcastType{
			TextView itemname;
		}
		
		private class ViewChannel{
			ImageView checkbox;
			TextView itemname;
			boolean isSelected;
		}
		
		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			ViewBroadcastType tag1 = null;
			ViewChannel tag2 = null;
			boolean isSelected = false;
			if (selected == position) {
				isSelected = true;
			}
			if (convertView == null) {
				if(vmode == viewMode.BROADCAST_TYPE){
					tag1 = new ViewBroadcastType();
					convertView = mInflater.inflate(R.layout.one_tx_item,
							null); 
					tag1.itemname = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag1.itemname.setText(broadcastType[position]);
					convertView.setTag(tag1);
				}else{
					tag2 = new ViewChannel();
					convertView = mInflater.inflate(R.layout.channelcell,
							null);
					tag2.itemname = (TextView)convertView.findViewById(R.id.ItemTitle);
					tag2.itemname.setText(channel.get(position).channelName);
					tag2.checkbox = (ImageView)convertView.findViewById(R.id.ItemSelect);
					tag2.isSelected = false;
					convertView.setTag(tag2);
				}
			} else {
				if(vmode == viewMode.BROADCAST_TYPE){
					tag1 = (ViewBroadcastType) convertView.getTag();
				}else{
					tag2 = (ViewChannel) convertView.getTag();
				}
			}
			
			if(vmode == viewMode.CHANNEL_LIST){
				if(isSelected){
					if(tag2.isSelected){
						tag2.isSelected = false;
						tag2.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_off));
					}else{
						tag2.isSelected = true;
						tag2.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_on));
					}
				}else{
					if(tag2.isSelected){
						tag2.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_on));
					}else{
						tag2.checkbox.setImageDrawable(getResources().getDrawable(
								R.drawable.dnla_device_check_off));
					}
				}
				
				TickIndex ti = new TickIndex();
				int count = tick.size();
				if(count > 0){
					boolean isExist = false;
					for(int i = 0; i < count; i++){
						if(tick.get(i).index == position){
							if(!tag2.isSelected){
								tick.remove(i);
							}
							isExist = true;
							break;
						}
						isExist = false;
					}
					if(!isExist && tag2.isSelected){
						ti.index = position;
						tick.add(ti);
					}
				}else{
					if(tag2.isSelected){
						ti.index = position;
						tick.add(ti);
					}
				}
				
				
			}

			return convertView;
		}

	}
}
