package com.rtk.dmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.realtek.Utils.ChannelAttr;
import com.rtk.dmp.ChannelInfo.ChannelAtrr;

import android.app.Activity;
import android.app.TvManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class LiveVideoPlayerActivity extends Activity {
	private final static String TAG = "LiveVideoPlayerActivity";
	private boolean loading = false;
	private boolean prepared = false;

	private final int MSG_STARTANIMATION = 0;
	private final int MSG_DISSMISSANIMATION = 1;
	private final int HIDE_MOVIE_BANNER = 2;
	private final int DELAY_SHOW_BANNER = 3;
	private final int HIDE_DOLBY = 4;

	private SurfaceView sView = null;
	protected MediaPlayer mPlayer = null;

	private MediaApplication mediaApp = null;
	private ArrayList<ChannelAttr> attrilist = null;

	private Timer timer = null;
	private TimerTask task_hide_controler = null;
	private TimerTask task_hide_dolby = null;

	private View MovieBannerView = null;
	private ImageView play = null;
	private ImageView slowFF = null;
	private ImageView slowRew = null;
	private ImageView fastFF = null;
	private ImageView fastRew = null;
	private ImageView programInfo = null;
	private TextView programInfo_txt = null;
	private ImageView programList = null;
	private ImageView pictureSize = null;
	private ImageView picturequality = null;
	private ImageView setting = null;
	private ImageView loadingIcon = null;
	private ImageView dolby = null;

	private static final float banner_h = 75f;
	private static final long bannerAnimTime = 200;

	/******** Set Subtitle info *******/
	private int[] SubtitleInfo = null;
	private int SPU_ENABLE = 0;
	private int subtitle_num_Stream = 1;
	private int curr_subtitle_stream_num = 0;
	private int textEncoding = 1000;
	private int textColor = 0;
	private int fontSize = 19;

	/******** Set Audio info *******/
	private int[] AudioInfo = null;
	private int audio_num_stream = 0;
	private int curr_audio_stream_num = 0;

	private PictureQuality picQuality = null;
	private FileListAdapter pictureAdapter = null;
	private setListAdapter menuAdapter = null;
	private Menu menu = null;
	private Animation ad = null;
	private Handler handler;
	private TvManager mTVService = null;
	private Activity mContext = this;
	private int selected_idx = 0;
	private Display display = null;
	private ArrayList<ChannelAtrr> curr_channel = null;
	private int channel_index = -1;

	public class SurfaceListener implements SurfaceHolder.Callback {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {

			try {
				// mPlayer.reset();
				handler.sendEmptyMessage(MSG_STARTANIMATION);
				mPlayer.setOnPreparedListener(videoPreparedListener);
				mPlayer.setOnCompletionListener(videoCompletionListener);
				mPlayer.setPlayerType(6);// using rtkplayer
				mPlayer.setDataSource(attrilist.get(0).getUri());
				//mPlayer.setDataSource("/mnt/udisk/sda1/video/Amazon_720_MPEG2_HD_NTSC.mpg");
				mPlayer.prepareAsync();
				mPlayer.setDisplay(sView.getHolder());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		setContentView(R.layout.live_video_player);
		super.onCreate(savedInstanceState);
		
		captureIntent();

		mTVService = (TvManager) getSystemService("tv");
		mediaApp = (MediaApplication) getApplication();
		mPlayer = mediaApp.getMediaPlayer();
		attrilist = mediaApp.getChannelAttrList();
		curr_channel = mediaApp.getChannelAtrrList();

		init_view();

		pictureAdapter = new FileListAdapter(this);
		picQuality = new PictureQuality(this, pictureAdapter);
		picQuality.setOnItemClickListener(picQualityItemClickListener);

		menuAdapter = new setListAdapter(this);
		menu = new Menu(this, menuAdapter);
		menu.setOnItemClickListener(menuItemClickListener);

		timer = new Timer();
		display = getWindowManager().getDefaultDisplay();
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
	}

	@Override
	protected void onPause() {
		Log.v(TAG, "onPause");
		if (mPlayer != null) {
			try {
				mPlayer.reset();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.v(TAG, "onDestroy");
		mediaApp.releaseMediaPlayer();
		attrilist.clear();
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.v(TAG, "onConfigurationChanged");
		/*switch (display.getRotation()) {
		case Surface.ROTATION_0:
			mTVService.Scaler_Rotate_Set(0, 1);
			break;
		case Surface.ROTATION_90:
			mTVService.Scaler_Rotate_Set(1, 1);
			break;
		case Surface.ROTATION_180:
			mTVService.Scaler_Rotate_Set(2, 1);
			break;
		case Surface.ROTATION_270:
			mTVService.Scaler_Rotate_Set(3, 1);
			break;
		default:
			break;
		}*/
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.e(TAG, "keyCode = " + keyCode);
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			Intent intent = new Intent();
			setResult(0, intent);
			break;
		}
		case KeyEvent.KEYCODE_ENTER: {
			if (MovieBannerView.isShown()) {
				animateHideBanner();
			} else {
				statusBannerShowControl(TimerDelay.delay_6s);
			}
			break;
		}
		}
		return super.onKeyDown(keyCode, event);
	}

	private OnPreparedListener videoPreparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer arg0) {
			// TODO Auto-generated method stub
			PlayFile();
		}

	};

	private OnCompletionListener videoCompletionListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer arg0) {
			// TODO Auto-generated method stub

		}

	};

	private OnItemClickListener picQualityItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			pictureAdapter.notifyDataSetChanged(position);
			media_play_init(attrilist.get(position).getUri());
		}

	};

	private OnItemClickListener menuItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			if (position == 0) {
				if (prepared && audio_num_stream > 0) {
					setAudioTrack();
				}
			} else if (position == 1) {
				menu.dismiss();
				ComponentName componetName = new ComponentName("com.rtk.dmp",
						"com.rtk.dmp.LiveDeviceSelect");
				Intent intent = new Intent();
				intent.setComponent(componetName);
				startActivity(intent);
			} else if (position == 2) {
				menu.dismiss();
				ComponentName componetName = new ComponentName(
						"com.android.emanualreader",
						"com.android.emanualreader.MainActivity");
				Intent intent = new Intent();
				intent.setComponent(componetName);
				startActivity(intent);
			}
		}

	};
	
	private void captureIntent(){
		Intent intent = getIntent();
		channel_index = intent.getIntExtra("channel_index", -1);
	}
	
	private void getProgramInfo(){
		if(channel_index >= 0){
			String channelNum = curr_channel.get(channel_index).broadType;
			String broadType = curr_channel.get(channel_index).broadType;
			if(broadType.contains("CS")){
				broadType = mContext.getResources().getString(R.string.channel_cs);
			}else if(broadType.contains("BS")){
				broadType = mContext.getResources().getString(R.string.channel_bs);
			}else{
				broadType = mContext.getResources().getString(R.string.channel_land);
			}
			String info = broadType+" "+
					channelNum+" "+curr_channel.get(channel_index).channelName;
			
			Log.e(TAG, "info = "+info);
			programInfo_txt.setText(info);
		}
	}

	private void initHandler() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_STARTANIMATION:
					startLoading();
					break;
				case MSG_DISSMISSANIMATION:
					dismissLoading();
					break;
				case HIDE_MOVIE_BANNER:
					animateHideBanner();
					break;
				case DELAY_SHOW_BANNER:
					statusBannerShowControl(TimerDelay.delay_6s);
					break;
				case HIDE_DOLBY:
					dolby.setVisibility(View.INVISIBLE);
					break;
				default:
					break;
				}
			}
		};
	}

	private void init_view() {
		MovieBannerView = (View) findViewById(R.id.movie_banner);
		MovieBannerView.setBackgroundColor(Color.TRANSPARENT);
		MovieBannerView.setVisibility(View.INVISIBLE);

		play = (ImageView) MovieBannerView.findViewById(R.id.play);
		slowFF = (ImageView) MovieBannerView.findViewById(R.id.slowforward);
		slowRew = (ImageView) MovieBannerView.findViewById(R.id.slowbackward);
		fastFF = (ImageView) MovieBannerView.findViewById(R.id.fastforward);
		fastRew = (ImageView) MovieBannerView.findViewById(R.id.fastbackward);
		programInfo = (ImageView) MovieBannerView
				.findViewById(R.id.program_info);
		programInfo_txt = (TextView) MovieBannerView
				.findViewById(R.id.program_info_txt);
		programList = (ImageView) MovieBannerView
				.findViewById(R.id.program_list);
		pictureSize = (ImageView) MovieBannerView
				.findViewById(R.id.picturesize);
		picturequality = (ImageView) MovieBannerView
				.findViewById(R.id.picturequality);
		setting = (ImageView) findViewById(R.id.main_menu);
		loadingIcon = (ImageView) findViewById(R.id.loadingIcon);
		dolby = (ImageView) findViewById(R.id.dolby);

		sView = (SurfaceView) findViewById(R.id.surfaceView);
		sView.getHolder().setKeepScreenOn(true);
		sView.getHolder().addCallback(new SurfaceListener());

		setOnClickListener(click);
		setOnFocusChangeListener(focus);
		
		getProgramInfo();
	}

	public void setOnClickListener(OnClickListener click) {
		play.setOnClickListener(click);
		slowFF.setOnClickListener(click);
		slowRew.setOnClickListener(click);
		fastFF.setOnClickListener(click);
		fastRew.setOnClickListener(click);
		programInfo.setOnClickListener(click);
		programInfo_txt.setOnClickListener(click);
		programList.setOnClickListener(click);
		pictureSize.setOnClickListener(click);
		picturequality.setOnClickListener(click);
		setting.setOnClickListener(click);
		sView.setOnClickListener(click);
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

	private void animateShowBanner() {
		if (!MovieBannerView.isShown()) {
			MovieBannerView.setVisibility(View.VISIBLE);

			MovieBannerView.clearAnimation();
			TranslateAnimation TransAnim;
			TransAnim = new TranslateAnimation(0.0f, 0.0f, banner_h, 0.0f);
			TransAnim.setDuration(bannerAnimTime);
			MovieBannerView.startAnimation(TransAnim);
		}
	}

	private void animateHideBanner() {
		MovieBannerView.clearAnimation();
		TranslateAnimation TransAnim;
		TransAnim = new TranslateAnimation(0.0f, 0.0f, 0.0f, banner_h);
		TransAnim.setDuration(bannerAnimTime);
		TransAnim.setAnimationListener(new hiderBannerListener());
		MovieBannerView.startAnimation(TransAnim);
	}

	private class hiderBannerListener implements AnimationListener {
		public void onAnimationEnd(Animation animation) {
			// TODO Auto-generated method stub
			MovieBannerView.clearAnimation();
			MovieBannerView.setVisibility(View.INVISIBLE);
		}

		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub
		}

		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub
		}

	}

	private void statusBannerShowControl(int delay) {
		if (task_hide_controler != null) {
			task_hide_controler.cancel();
			task_hide_controler = null;
		}

		animateShowBanner();

		task_hide_controler = new TimerTask() {
			public void run() {
				handler.sendEmptyMessage(HIDE_MOVIE_BANNER);
			}
		};

		if (timer != null)
			timer.schedule(task_hide_controler, delay);
	}

	private OnClickListener click = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v == play) {
				/*
				 * if(mPlayer.isPlaying()) { mPlayer.pause();
				 * play.setImageResource(R.drawable.gui_play); } else {
				 * mPlayer.start(); play.setImageResource(R.drawable.gui_pause);
				 * } statusBannerShowControl(TimerDelay.delay_6s);
				 */
			} else if (v == slowFF) {
				// statusBannerShowControl(TimerDelay.delay_6s);
			} else if (v == slowRew) {
				// statusBannerShowControl(TimerDelay.delay_6s);
			} else if (v == slowRew) {
				// statusBannerShowControl(TimerDelay.delay_6s);
			} else if (v == fastFF) {
				// statusBannerShowControl(TimerDelay.delay_6s);
			} else if (v == fastRew) {
				// statusBannerShowControl(TimerDelay.delay_6s);
			} else if (v == programInfo) {
				// statusBannerShowControl(TimerDelay.delay_6s);
			} else if (v == programList) {
				/*
				 * Intent intent = new Intent(); setResult(0, intent);
				 * mContext.finish();
				 */
			} else if (v == pictureSize) {
				 statusBannerShowControl(TimerDelay.delay_6s);
				 setPicSize();
			} else if (v == picturequality) {
				picQuality.showPictureQuality(82, 300);
				statusBannerShowControl(TimerDelay.delay_6s);
			} else if (v == sView) {
				if (MovieBannerView.isShown()) {
					animateHideBanner();
				} else {
					statusBannerShowControl(TimerDelay.delay_6s);
				}
			} else {
				menu.showMenu(82, 138);
			}
		}

	};

	public void setOnFocusChangeListener(OnFocusChangeListener focus) {
		play.setOnFocusChangeListener(focus);
		slowFF.setOnFocusChangeListener(focus);
		slowRew.setOnFocusChangeListener(focus);
		fastFF.setOnFocusChangeListener(focus);
		fastRew.setOnFocusChangeListener(focus);
		programInfo.setOnFocusChangeListener(focus);
		programInfo_txt.setOnFocusChangeListener(focus);
		programList.setOnFocusChangeListener(focus);
		pictureSize.setOnFocusChangeListener(focus);
		picturequality.setOnFocusChangeListener(focus);
		setting.setOnFocusChangeListener(focus);
	}

	public OnFocusChangeListener focus = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (v == play) {
				if (mPlayer.isPlaying()) {
					if (hasFocus) {
						play.setImageResource(R.drawable.gui_play);
						statusBannerShowControl(TimerDelay.delay_6s);
					} else {
						play.setImageResource(R.drawable.dnla_music_controll_stop_n);
					}
				} else {
					if (hasFocus) {
						play.setImageResource(R.drawable.gui_pause);
						statusBannerShowControl(TimerDelay.delay_6s);
					} else {
						play.setImageResource(R.drawable.dnla_music_controll_play_n);
					}
				}
			} else if (v == slowFF) {
				if (hasFocus) {
					slowFF.setImageResource(R.drawable.dnla_video_controll_slow_f_f);
					statusBannerShowControl(TimerDelay.delay_6s);
				} else {
					slowFF.setImageResource(R.drawable.dnla_video_controll_slow_f_n);
				}
			} else if (v == slowRew) {
				if (hasFocus) {
					slowRew.setImageResource(R.drawable.dnla_video_controll_slow_b_f);
					statusBannerShowControl(TimerDelay.delay_6s);
				} else {
					slowRew.setImageResource(R.drawable.dnla_video_controll_slow_b_n);
				}
			} else if (v == fastFF) {
				if (hasFocus) {
					fastFF.setImageResource(R.drawable.dnla_music_controll_ff_f);
					statusBannerShowControl(TimerDelay.delay_6s);
				} else {
					fastFF.setImageResource(R.drawable.dnla_music_controll_ff_n);
				}
			} else if (v == fastRew) {
				if (hasFocus) {
					fastRew.setImageResource(R.drawable.dnla_music_controll_rew_f);
					statusBannerShowControl(TimerDelay.delay_6s);
				} else {
					fastRew.setImageResource(R.drawable.dnla_music_controll_rew_n);
				}
			} else if (v == programInfo) {
				if (hasFocus) {
					programInfo
							.setImageResource(R.drawable.dlna_livestream_icon_program_information_f);
					statusBannerShowControl(TimerDelay.delay_6s);
				} else {
					programInfo
							.setImageResource(R.drawable.dlna_livestream_icon_program_information);
				}
			} else if (v == programList) {
				if (hasFocus) {
					programList
							.setImageResource(R.drawable.dlna_livestream_icon_program_list_f);
					Intent intent = new Intent();
					setResult(0, intent);
					mContext.finish();
				} else {
					programList
							.setImageResource(R.drawable.dlna_livestream_icon_program_list);
				}
			} else if (v == pictureSize) {
				if (hasFocus) {
					pictureSize.setImageResource(R.drawable.dnla_video_size_f);
					setPicSize();
					statusBannerShowControl(TimerDelay.delay_6s);
				} else {
					pictureSize
							.setImageResource(R.drawable.dlna_livestream_size_n);
				}
			} else if (v == picturequality) {
				if (hasFocus) {
					picturequality
							.setImageResource(R.drawable.dlna_livestream_icon_picture_quality_f);
					picQuality.showPictureQuality(82, 300);
					statusBannerShowControl(TimerDelay.delay_6s);
				} else {
					picturequality
							.setImageResource(R.drawable.dlna_livestream_icon_picture_quality);
				}
			} else {
			}
		}

	};

	public void PlayFile() {
		prepared = true;
		handler.sendEmptyMessage(MSG_DISSMISSANIMATION);
		mPlayer.start();
		handler.sendEmptyMessage(DELAY_SHOW_BANNER);
		getAudioTrackInfo();
		SetDolby();
	}

	private void getAudioTrackInfo() {
		AudioInfo = mPlayer.getAudioTrackInfo(-1);
		audio_num_stream = AudioInfo[1];
		curr_audio_stream_num = AudioInfo[2];
	}

	private void setAudioTrack() {
		if (audio_num_stream > 0) {
			if (curr_audio_stream_num < audio_num_stream)
				curr_audio_stream_num++;
			else
				curr_audio_stream_num = 1;

			mPlayer.setAudioTrackInfo(curr_audio_stream_num);

			SetDolby();
		}
	}

	private void getSubtitleInfo() {
		SubtitleInfo = mPlayer.getSubtitleInfo();

		subtitle_num_Stream = SubtitleInfo[1];
		curr_subtitle_stream_num = SubtitleInfo[2];

		if (subtitle_num_Stream > 0) {
			if (SubtitleInfo[2] < 0)
				curr_subtitle_stream_num = 1;
		}
	}

	private void setSubtitle() {
		getSubtitleInfo();

		if (SPU_ENABLE == 0)
			curr_subtitle_stream_num = 0;
		curr_subtitle_stream_num++;
		if (curr_subtitle_stream_num <= subtitle_num_Stream) {
			SPU_ENABLE = 1;
			mPlayer.setSubtitleInfo(curr_subtitle_stream_num, SPU_ENABLE,
					textEncoding, textColor, fontSize);
		} else {
			SPU_ENABLE = 0;
			curr_subtitle_stream_num = subtitle_num_Stream;
			mPlayer.setSubtitleInfo(curr_subtitle_stream_num, SPU_ENABLE,
					textEncoding, textColor, fontSize);
		}

		// meta_data_query_delay(DivxParser.DIVX_METADATA_SUBTITLE,
		// TimerDelay.delay_100ms);
	}

	public void SetDolby() {
		int[] currAudioInfo = mPlayer.getAudioTrackInfo(-1);
		String mAudioType = Utility.AUDIO_TYPE_TABLE(currAudioInfo[3]);
		if (mAudioType.compareTo("Dolby AC3") == 0) {
			dolby.setVisibility(View.VISIBLE);
			dolby.setImageResource(R.drawable.dolby);
			hide_dolby_delay();
		} else if (mAudioType.compareTo("Dolby Digital Plus") == 0) {
			dolby.setVisibility(View.VISIBLE);
			dolby.setImageResource(R.drawable.dolby_plus);
			hide_dolby_delay();
		} else {
			dolby.setVisibility(View.INVISIBLE);
		}
	}

	private void hide_dolby_delay() {
		if (task_hide_dolby != null) {
			task_hide_dolby.cancel();
			task_hide_dolby = null;
		}

		task_hide_dolby = new TimerTask() {

			@Override
			public void run() {
				handler.sendEmptyMessage(HIDE_DOLBY);
			}

		};
		if (timer != null)
			timer.schedule(task_hide_dolby, TimerDelay.delay_4s);
	}

	private class FileListAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private int selected = -1;

		public FileListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public void notifyDataSetChanged(int id) {
			selected = id;
			super.notifyDataSetChanged();
		}

		public void clearSelected() {
			selected = -1;
		}

		@Override
		public int getCount() {

			return attrilist.size();
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
			TextView resolution;
			TextView bitrate;
			ImageView checkbox;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder tag = null;
			boolean isSelected = false;
			if (selected == position) {
				isSelected = true;
			}
			if (convertView == null) {
				tag = new ViewHolder();
				convertView = mInflater.inflate(R.layout.picture_quality_cell,
						null);
				tag.checkbox = (ImageView) convertView.findViewById(R.id.check);
				tag.resolution = (TextView) convertView
						.findViewById(R.id.resolution);
				tag.bitrate = (TextView) convertView.findViewById(R.id.bitrate);
				convertView.setTag(tag);
			} else {
				tag = (ViewHolder) convertView.getTag();
			}

			tag.resolution.setText(attrilist.get(position).getResolution());
			tag.bitrate.setText(attrilist.get(position).getBitrate());

			if (isSelected) {
				tag.checkbox
						.setImageDrawable(getResources()
								.getDrawable(
										R.drawable.dlna_livestream_icon_picture_quality_radio_f));
			} else {
				tag.checkbox
						.setImageDrawable(getResources()
								.getDrawable(
										R.drawable.dlna_livestream_icon_picture_quality_radio_n));
			}

			return convertView;
		}

	}

	public class setListAdapter extends BaseAdapter {
		public final class ViewHolder {
			TextView item_name;
		}

		int[] menu_name = new int[] { R.string.menu_audio_switch,
				R.string.menu_setting, R.string.menu_help };

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
		public boolean isEnabled(int position) {
			if (prepared && audio_num_stream > 0) {
				return super.isEnabled(position);
			} else {
				if (position == 0) {
					return false;
				} else {
					return super.isEnabled(position);
				}
			}
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

			if (position == 0) {
				if (prepared && audio_num_stream > 0) {
					convertView.setBackgroundResource(R.drawable.menu_focus_select);
				} else {
					convertView.setBackgroundResource(R.drawable.menu_gray_out);
				}
			} else {
				convertView.setBackgroundResource(R.drawable.menu_focus_select);
			}

			return convertView;
		}

	}

	private void media_play_init(String uri) {
		prepared = false;
		try {
			handler.sendEmptyMessage(MSG_STARTANIMATION);
			mPlayer.reset();
			mPlayer.setOnPreparedListener(videoPreparedListener);
			mPlayer.setOnCompletionListener(videoCompletionListener);
			mPlayer.setPlayerType(6);// using rtkplayer
			mPlayer.setDataSource(uri);
			mPlayer.prepareAsync();
			mPlayer.setDisplay(sView.getHolder());
			getProgramInfo();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setPicSize() {
		switch (selected_idx) {
		case 0:
			mTVService.setAspectRatio(TvManager.SCALER_RATIO_PANORAMA);
			selected_idx++;
			break;
		case 1:
			mTVService.setAspectRatio(TvManager.SCALER_RATIO_POINTTOPOINT);
			selected_idx++;
			break;
		case 2:
			mTVService.setAspectRatio(TvManager.SCALER_RATIO_BBY_AUTO);
			selected_idx++;
			break;
		case 3:
			mTVService.setAspectRatio(TvManager.SCALER_RATIO_BBY_ZOOM);
			selected_idx = 0;
			break;
		default:
			break;
		}
	}
}
