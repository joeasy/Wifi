package com.rtk.dmp;

import java.lang.ref.WeakReference;

import android.R.integer;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.TvManager;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class FragmentMode3DSetting extends Fragment {
	public static final String TAG = "FragmentMode3DSetting";

	private static final int MODE_3D_SETTING_ITEM_MODE = 0;
	private static final int MODE_3D_SETTING_ITEM_LRSWAP = 1;
	private static final int MODE_3D_SETTING_ITEM_DEEP = 2;
	private static final int MODE_3D_SETTING_ITEM_STRENGTH = 3;
	private static final int MODE_3D_SETTING_ITEM_3DCVRT2D = 4;
	private static final int MODE_3D_SETTING_ITEM_RATIO = 5;
	private static final int MODE_3D_SETTING_ITEM_MUTE = 6;
	private static final int MODE_3D_SETTING_ITEM_COUNT = 7;
	private static final int MODE_3D_SETTING_ITEM_FIRST = MODE_3D_SETTING_ITEM_MODE;
	private static final int MODE_3D_SETTING_ITEM_LAST = MODE_3D_SETTING_ITEM_MUTE;

	private static final int MODE_3D_SETTING_ITEM_TYPE_SPINNER = 0;
	private static final int MODE_3D_SETTING_ITEM_TYPE_SEEKBAR = 1;
	private static final int MODE_3D_SETTING_ITEM_TYPE_COUNT = 2;

	private static final int MODE_3D_SETTING_ITEM_DEEP_MAX = 6;
	private static final int MODE_3D_SETTING_ITEM_DEEP_MIN = -6;
	private static final int MODE_3D_SETTING_ITEM_DEEP_RANGE = 12;
	private static final int MODE_3D_SETTING_ITEM_DEEP_OFFSET = 6;

	private static final int MODE_3D_SETTING_ITEM_STRENGTH_MAX = 32;
	private static final int MODE_3D_SETTING_ITEM_STRENGTH_MIN = 0;
	private static final int MODE_3D_SETTING_ITEM_STRENGTH_RANGE = 32;
	private static final int MODE_3D_SETTING_ITEM_STRENGTH_OFFSET = 0;

	private static final int MODE_3D_SETTING_ITEM_MODE_CHANGE = MODE_3D_SETTING_ITEM_MODE;
	private static final int MODE_3D_SETTING_ITEM_LRSWAP_CHANGE = MODE_3D_SETTING_ITEM_LRSWAP;
	private static final int MODE_3D_SETTING_ITEM_DEEP_CHANGE = MODE_3D_SETTING_ITEM_DEEP;
	private static final int MODE_3D_SETTING_ITEM_STRENGTH_CHANGE = MODE_3D_SETTING_ITEM_STRENGTH;
	private static final int MODE_3D_SETTING_ITEM_3DCVRT2D_CHANGE = MODE_3D_SETTING_ITEM_3DCVRT2D;
	private static final int MODE_3D_SETTING_ITEM_RATIO_CHANGE = MODE_3D_SETTING_ITEM_RATIO;
	private static final int MODE_3D_SETTING_ITEM_MUTE_CHANGE = MODE_3D_SETTING_ITEM_MUTE;
	private static final int MODE_3D_SETTING_ITEM_CHANGE_DELAY = 1000;

	private static final int POP_SELF_FROM_FRAGMENT_STACK = 10;
	private static final int POP_SELF_FROM_FRAGMENT_STACK_DELAY = 10000;

	private static final int[] MODE_3D_TYPES = { TvManager.SLR_3DMODE_2D,
			TvManager.SLR_3DMODE_3D_AUTO, TvManager.SLR_3DMODE_3D_SBS,
			TvManager.SLR_3DMODE_3D_TB, TvManager.SLR_3DMODE_3D_FP,
			TvManager.SLR_3DMODE_2D_CVT_3D, TvManager.SLR_3DMODE_3D_LBL,
			TvManager.SLR_3DMODE_3D_VSTRIP, TvManager.SLR_3DMODE_3D_CKB,
			TvManager.SLR_3DMODE_3D_REALID, TvManager.SLR_3DMODE_3D_SENSIO,
			TvManager.SLR_3DMODE_3D_AUTO_CVT_2D,
			TvManager.SLR_3DMODE_3D_SBS_CVT_2D,
			TvManager.SLR_3DMODE_3D_TB_CVT_2D,
			TvManager.SLR_3DMODE_3D_FP_CVT_2D,
			TvManager.SLR_3DMODE_3D_LBL_CVT_2D,
			TvManager.SLR_3DMODE_3D_VSTRIP_CVT_2D,
			TvManager.SLR_3DMODE_3D_CKB_CVT_2D,
			TvManager.SLR_3DMODE_3D_REALID_CVT_2D,
			TvManager.SLR_3DMODE_3D_SENSIO_CVT_2D, TvManager.SLR_3DMODE_DISABLE };

	// private static final int[] MODE_3D_RATIO_TYPES = { TvManager.PS_4_3,
	// TvManager.LB_4_3, TvManager.Wide_16_9, TvManager.Wide_16_10,
	// TvManager.SCALER_RATIO_AUTO, TvManager.SCALER_RATIO_4_3,
	// TvManager.SCALER_RATIO_16_9, TvManager.SCALER_RATIO_14_9,
	// TvManager.SCALER_RATIO_LETTERBOX, TvManager.SCALER_RATIO_PANORAMA,
	// TvManager.SCALER_RATIO_FIT, TvManager.SCALER_RATIO_POINTTOPOINT,
	// TvManager.SCALER_RATIO_BBY_AUTO, TvManager.SCALER_RATIO_BBY_NORMAL,
	// TvManager.SCALER_RATIO_BBY_ZOOM, TvManager.SCALER_RATIO_BBY_WIDE_1,
	// TvManager.SCALER_RATIO_BBY_WIDE_2,
	// TvManager.SCALER_RATIO_BBY_CINEMA, TvManager.SCALER_RATIO_CUSTOM,
	// TvManager.SCALER_RATIO_PERSON, TvManager.SCALER_RATIO_CAPTION,
	// TvManager.SCALER_RATIO_MOVIE, TvManager.SCALER_RATIO_100,
	// TvManager.SCALER_RATIO_SOURCE, TvManager.SCALER_RATIO_ZOOM_14_9,
	// TvManager.SCALER_RATIO_DISABLE };

	private static final int[] MODE_3D_RATIO_TYPES = {
			TvManager.SLR_RATIO_AUTO, TvManager.SLR_RATIO_4_3,
			TvManager.SLR_RATIO_16_9, TvManager.SLR_RATIO_14_9,
			TvManager.SLR_RATIO_LETTERBOX, TvManager.SLR_RATIO_PANORAMA,
			TvManager.SLR_RATIO_FIT, TvManager.SLR_RATIO_POINTTOPOINT,
			TvManager.SLR_RATIO_BBY_AUTO, TvManager.SLR_RATIO_BBY_NORMAL,
			TvManager.SLR_RATIO_BBY_ZOOM, TvManager.SLR_RATIO_BBY_WIDE_1,
			TvManager.SLR_RATIO_BBY_WIDE_2, TvManager.SLR_RATIO_BBY_CINEMA,
			TvManager.SLR_RATIO_CUSTOM, TvManager.SLR_ASPECT_RATIO_PERSON,
			TvManager.SLR_ASPECT_RATIO_CAPTION,
			TvManager.SLR_ASPECT_RATIO_MOVIE, TvManager.SLR_ASPECT_RATIO_100,
			TvManager.SLR_ASPECT_RATIO_SOURCE, TvManager.SLR_RATIO_ZOOM_14_9 };

	private static final int[] MODE_3D_ONOFF_TYPES = {
			R.string.TITLE_3DMODE_SETTING_OFF, R.string.TITLE_3DMODE_SETTING_ON };

	private static final int[] MODE_3D_3DCVRT2D_TYPES = {
			R.string.TITLE_3DMODE_SETTING_OFF,
			R.string.TITLE_3DMODE_SETTING_LFRAME,
			R.string.TITLE_3DMODE_SETTING_RFRAME };

	private Activity pActivity = null;
	private ListView listView;
	private Mode3DSettingListViewAdapter listViewAdapter;
	private int selectedMode3DTypeIndex = MODE_3D_TYPES.length - 1;
	private static int selectedTvManagerMode3DMode = TvManager.SLR_3DMODE_DISABLE;
	private static int selectedTvManagerMode3DDeep = -3;
	private static int selectedTvManagerMode3DStrength = 16;
	private static boolean selectedTvManagerMode3DLRSwap = false;
	private static boolean selectedTvManagerMode3DMute = false;
	private static int selectedTvManagerMode3DCVRT2D = MODE_3D_3DCVRT2D_TYPES[0];
	private int selectedMode3DRatioIndex;
	private static int selectedTvManagerMode3DRatio;
	private static boolean mode3DEnabled = false;
	private Mode3DSettingMessageHandler mode3DSettingMessageHandler;
	private TvManager tvManager;

	public FragmentMode3DSetting() {

	}

	public static FragmentMode3DSetting NewInstance(Activity activity) {
		FragmentMode3DSetting fragmentMode3DSetting = new FragmentMode3DSetting();
		fragmentMode3DSetting.setParentActivity(activity);
		return fragmentMode3DSetting;
	}

	public void setParentActivity(Activity activity) {
		pActivity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		View currentView = inflater.inflate(R.layout.fragment_mode_3d_setting,
				container, false);

		tvManager = (TvManager) (pActivity.getSystemService("tv"));
		selectedTvManagerMode3DMode = tvManager.get3dMode();
		selectedTvManagerMode3DLRSwap = tvManager.get3dLRSwap();
		selectedTvManagerMode3DMute = tvManager.getMute();
		selectedTvManagerMode3DDeep = tvManager.get3dDeep();
		// selectedTvManagerMode3DStrength = tvManager.getSomething();
		selectedTvManagerMode3DCVRT2D = tvManager.get3dCvrt2D() ? 1 : 0;
		selectedTvManagerMode3DRatio = tvManager.getAspectRatio(/*tvManager
				.getCurLiveSource()*/);
		selectedMode3DTypeIndex = Mode3DTypeIndexOfType(selectedTvManagerMode3DMode);
		selectedMode3DRatioIndex = Mode3DRatioIndexOfType(selectedTvManagerMode3DRatio);
		mode3DEnabled = tvManager.getIs3d();

		listView = (ListView) currentView
				.findViewById(R.id.mode_3d_setting_list);
		listViewAdapter = new Mode3DSettingListViewAdapter(pActivity);
		listViewAdapter.setSelectedModeIndex(selectedMode3DTypeIndex);
		listViewAdapter.setSelectedModeLRSwap(selectedTvManagerMode3DLRSwap ? 1
				: 0);
		listViewAdapter.setSelectedModeDeep(selectedTvManagerMode3DDeep);
		listViewAdapter
				.setSelectedModeStrength(selectedTvManagerMode3DStrength);
		listViewAdapter.setSelectedMode3DCVRT2D(selectedTvManagerMode3DCVRT2D);
		listViewAdapter.setSelectedModeRatio(selectedMode3DRatioIndex);
		listViewAdapter
				.setSelectedModeMute(selectedTvManagerMode3DMute ? 1 : 0);
		listView.setAdapter(listViewAdapter);
		listView.requestFocus();
		listView.setOnKeyListener(keyListener);
		// listView.setOnItemSelectedListener(itemSelectedListener);

		mode3DSettingMessageHandler = new Mode3DSettingMessageHandler(this);

		return currentView;
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();

		mode3DSettingMessageHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public void onDestroyView() {
		Log.d(TAG, "onDestroyView");
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	private void PopSelfFromFragmentStack() {
		pActivity.getFragmentManager().popBackStack(TAG,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);
	}

	OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {
		public void onItemSelected(android.widget.AdapterView<?> arg0,
				View arg1, int arg2, long arg3) {
		};

		public void onNothingSelected(android.widget.AdapterView<?> arg0) {
		};
	};

	OnKeyListener keyListener = new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				mode3DSettingMessageHandler
						.removeMessages(POP_SELF_FROM_FRAGMENT_STACK);
				mode3DSettingMessageHandler.sendMessageDelayed(
						mode3DSettingMessageHandler
								.obtainMessage(POP_SELF_FROM_FRAGMENT_STACK),
						POP_SELF_FROM_FRAGMENT_STACK_DELAY);
				return false;
			}
			boolean isStepToNext = true;
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
				if (CurrentListViewItemIndex() == MODE_3D_SETTING_ITEM_FIRST) {
					listView.setSelection(MODE_3D_SETTING_ITEM_LAST);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if (CurrentListViewItemIndex() == MODE_3D_SETTING_ITEM_LAST) {
					listView.setSelection(MODE_3D_SETTING_ITEM_FIRST);
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				isStepToNext = false;
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				isStepToNext = true;
				break;
			default:
				break;
			}

			if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
					|| keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
				switch (CurrentListViewItemIndex()) {
				case MODE_3D_SETTING_ITEM_MODE: {
					StepToMode3DType(isStepToNext);
					UpdateListView();
				}
					return true;
				case MODE_3D_SETTING_ITEM_LRSWAP: {
					if (mode3DEnabled) {
						ChangeMode3DLRSwap();
						UpdateListView();
					}
				}
					return true;
				case MODE_3D_SETTING_ITEM_3DCVRT2D: {
					if (mode3DEnabled) {
						StepToMode3DCVRT2D(isStepToNext);
						UpdateListView();
					}
				}
					return true;
				case MODE_3D_SETTING_ITEM_DEEP: {
					if (mode3DEnabled) {
						StepToMode3DDeep(isStepToNext);
						UpdateListView();
					}
				}
					return true;
				case MODE_3D_SETTING_ITEM_STRENGTH: {
					if (getIs2DTo3D()) {
						StepToMode3DStrength(isStepToNext);
						UpdateListView();
					}
				}
					return true;
				case MODE_3D_SETTING_ITEM_RATIO: {
					StepToMode3DRatio(isStepToNext);
					UpdateListView();
				}
					return true;
				case MODE_3D_SETTING_ITEM_MUTE: {
					ChangeMode3DMute();
					UpdateListView();
				}
					return true;
				default:
					break;
				}
			}
			return false;
		}
	};

	private void UpdateListView() {
		listViewAdapter.notifyDataSetChanged();
	}

	/**
	 * get the selected item index of listView
	 * 
	 * @return
	 */
	private int CurrentListViewItemIndex() {
		return listView.getSelectedItemPosition();
	}

	/**
	 * get the 3d mode type defined in tvManager from vector MODE_3D_TYPES
	 * 
	 * @param index
	 * @return
	 */
	private int TvManagerMode3DTypeOfIndex(int index) {
		int typeLength = MODE_3D_TYPES.length;
		while (index < 0)
			index += typeLength;
		while (index >= typeLength)
			index -= typeLength;
		return MODE_3D_TYPES[index];
	}

	/**
	 * get the 3d mode ratio defined in tvManager from vector
	 * MODE_3D_RATIO_TYPES
	 * 
	 * @param index
	 * @return
	 */
	private int TvManagerMode3DRatioOfIndex(int index) {
		int typeLength = MODE_3D_RATIO_TYPES.length;
		while (index < 0)
			index += typeLength;
		while (index >= typeLength)
			index -= typeLength;
		return MODE_3D_RATIO_TYPES[index];
	}

	/**
	 * change selectedMode3DTypeIndex, update listView and send message to set
	 * 3d mode type
	 * 
	 * @param isNext
	 */
	private void StepToMode3DType(boolean isNext) {
		if (isNext) {
			if (selectedMode3DTypeIndex == MODE_3D_TYPES.length - 1)
				selectedMode3DTypeIndex = 0;
			else
				selectedMode3DTypeIndex++;
		} else {
			if (selectedMode3DTypeIndex == 0)
				selectedMode3DTypeIndex = MODE_3D_TYPES.length - 1;
			else
				selectedMode3DTypeIndex--;
		}
		selectedTvManagerMode3DMode = MODE_3D_TYPES[selectedMode3DTypeIndex];
		listViewAdapter.setSelectedModeIndex(selectedMode3DTypeIndex);
		SendMessageDelayed(MODE_3D_SETTING_ITEM_MODE_CHANGE,
				MODE_3D_SETTING_ITEM_CHANGE_DELAY);
	}

	/**
	 * call tvManager to set 3d mode
	 * 
	 * @param mode
	 */
	private void set3DMode(int mode) {
		tvManager.set3dMode(mode);
		Log.d(TAG, "set3DMode "
				+ getString(Mode3DTypeStringOfIndex(selectedMode3DTypeIndex)));
	}

	/**
	 * change selectedMode3DLRSwap, update listView and send message to set 3d
	 * mode lrswap
	 * 
	 * @param isNext
	 */
	private void ChangeMode3DLRSwap() {
		selectedTvManagerMode3DLRSwap = !selectedTvManagerMode3DLRSwap;
		if (selectedTvManagerMode3DLRSwap)
			listViewAdapter.setSelectedModeLRSwap(1);
		else
			listViewAdapter.setSelectedModeLRSwap(0);
		SendMessageDelayed(MODE_3D_SETTING_ITEM_LRSWAP_CHANGE,
				MODE_3D_SETTING_ITEM_CHANGE_DELAY);
	}

	/**
	 * call tvManager to set 3d lrswap
	 * 
	 * @param value
	 */
	private void set3DLRSwap(boolean value) {
		tvManager.set3dLRSwap(value);
		Log.d(TAG,
				"set3DLRSwap "
						+ getString(Mode3DOnOffStringOfIndex(selectedTvManagerMode3DLRSwap ? 1
								: 0)));
	}

	/**
	 * change selectedMode3DMute, update listView and send message to set 3d
	 * mode mute
	 * 
	 * @param isNext
	 */
	private void ChangeMode3DMute() {
		selectedTvManagerMode3DMute = !selectedTvManagerMode3DMute;
		if (selectedTvManagerMode3DMute)
			listViewAdapter.setSelectedModeMute(1);
		else
			listViewAdapter.setSelectedModeMute(0);
		SendMessageDelayed(MODE_3D_SETTING_ITEM_MUTE_CHANGE,
				MODE_3D_SETTING_ITEM_CHANGE_DELAY);
	}

	/**
	 * call tvManager to set 3d mute
	 * 
	 * @param value
	 */
	private void set3DMute(boolean value) {
		tvManager.set3dModeAndChangeRatio(selectedTvManagerMode3DMode, value,
				selectedTvManagerMode3DRatio);
		Log.d(TAG,
				"set3DMute "
						+ getString(Mode3DOnOffStringOfIndex(selectedTvManagerMode3DMute ? 1
								: 0)));
	}

	/**
	 * change selectedTvManagerMode3DDeep, update listView and send message to
	 * set 3d mode deep
	 * 
	 * @param isIncrease
	 */
	private void StepToMode3DDeep(boolean isIncrease) {
		if (isIncrease) {
			if (selectedTvManagerMode3DDeep < MODE_3D_SETTING_ITEM_DEEP_MAX) {
				selectedTvManagerMode3DDeep++;
			} else
				return;
		} else {
			if (selectedTvManagerMode3DDeep > MODE_3D_SETTING_ITEM_DEEP_MIN) {
				selectedTvManagerMode3DDeep--;
			} else
				return;
		}
		listViewAdapter.setSelectedModeDeep(selectedTvManagerMode3DDeep);
		SendMessageDelayed(MODE_3D_SETTING_ITEM_DEEP_CHANGE,
				MODE_3D_SETTING_ITEM_CHANGE_DELAY);
	}

	/**
	 * call tvManager to set 3d deep
	 * 
	 * @param value
	 */
	private void set3DDeep(int value) {
		tvManager.set3dDeep(value);
		Log.d(TAG, "set3DDeep " + value);
	}

	/**
	 * change selectedTvManagerMode3DStrength, update listView and send message
	 * to set 3d mode strength
	 * 
	 * @param isIncrease
	 */
	private void StepToMode3DStrength(boolean isIncrease) {
		if (isIncrease) {
			if (selectedTvManagerMode3DStrength < MODE_3D_SETTING_ITEM_STRENGTH_MAX) {
				selectedTvManagerMode3DStrength++;
			} else
				return;
		} else {
			if (selectedTvManagerMode3DStrength > MODE_3D_SETTING_ITEM_STRENGTH_MIN) {
				selectedTvManagerMode3DStrength--;
			} else
				return;
		}
		listViewAdapter
				.setSelectedModeStrength(selectedTvManagerMode3DStrength);
		SendMessageDelayed(MODE_3D_SETTING_ITEM_STRENGTH_CHANGE,
				MODE_3D_SETTING_ITEM_CHANGE_DELAY);
	}

	/**
	 * call tvManager to set 3d strength
	 * 
	 * @param value
	 */
	private void set3DStrength(int value) {
		tvManager.set3dStrength(value);
		Log.d(TAG, "set3dStrength " + value);
	}

	/**
	 * change selectedMode3DCVRT2D, update listView and send message to set 3d
	 * mode 3d cvrt 2d
	 * 
	 * @param isNext
	 */
	private void StepToMode3DCVRT2D(boolean isNext) {
		if (isNext) {
			if (selectedTvManagerMode3DCVRT2D == MODE_3D_3DCVRT2D_TYPES.length - 1)
				selectedTvManagerMode3DCVRT2D = 0;
			else
				selectedTvManagerMode3DCVRT2D++;
		} else {
			if (selectedTvManagerMode3DCVRT2D == 0)
				selectedTvManagerMode3DCVRT2D = MODE_3D_3DCVRT2D_TYPES.length - 1;
			else
				selectedTvManagerMode3DCVRT2D--;
		}
		listViewAdapter.setSelectedMode3DCVRT2D(selectedTvManagerMode3DCVRT2D);
		SendMessageDelayed(MODE_3D_SETTING_ITEM_3DCVRT2D_CHANGE,
				MODE_3D_SETTING_ITEM_CHANGE_DELAY);
	}

	/**
	 * call tvManager to set 3d cvrt 2d
	 * 
	 * @param value
	 */
	private void set3DCVRT2D(int value) {
		int type = MODE_3D_3DCVRT2D_TYPES[value];
		// 0:R, 1:L
		switch (type) {
		case R.string.TITLE_3DMODE_SETTING_OFF:
			tvManager.set3dCvrt2D(false, 0);
			break;
		case R.string.TITLE_3DMODE_SETTING_RFRAME:
			tvManager.set3dCvrt2D(true, 0);
			break;
		case R.string.TITLE_3DMODE_SETTING_LFRAME:
			tvManager.set3dCvrt2D(true, 1);
			break;
		default:
			break;
		}
		Log.d(TAG,
				"set3DCVRT2D "
						+ getString(Mode3DCVRT2DStringOfIndex(selectedTvManagerMode3DCVRT2D)));
	}

	/**
	 * change selectedMode3DRatio, update listView and send message to set 3d
	 * mode ratio
	 * 
	 * @param isNext
	 */
	private void StepToMode3DRatio(boolean isNext) {
		if (isNext) {
			if (selectedMode3DRatioIndex == MODE_3D_RATIO_TYPES.length - 1)
				selectedMode3DRatioIndex = 0;
			else
				selectedMode3DRatioIndex++;
		} else {
			if (selectedMode3DRatioIndex == 0)
				selectedMode3DRatioIndex = MODE_3D_RATIO_TYPES.length - 1;
			else
				selectedMode3DRatioIndex--;
		}
		selectedTvManagerMode3DRatio = MODE_3D_RATIO_TYPES[selectedMode3DRatioIndex];
		listViewAdapter.setSelectedModeRatio(selectedMode3DRatioIndex);
		SendMessageDelayed(MODE_3D_SETTING_ITEM_RATIO_CHANGE,
				MODE_3D_SETTING_ITEM_CHANGE_DELAY);
	}

	/**
	 * call tvManager to set 3d ratio
	 * 
	 * @param value
	 */
	private void set3DRatio(int value) {
		tvManager.set3dModeAndChangeRatio(selectedTvManagerMode3DMode,
				selectedTvManagerMode3DMute, value);
		Log.d(TAG, "set3DRatio "
				+ getString(Mode3DRatioStringOfIndex(selectedMode3DRatioIndex)));
	}

	/**
	 * call tvManager to get is 3d
	 * 
	 * @return
	 */
	private boolean getIs3D() {
		return tvManager.getIs3d();
	}

	/**
	 * if 2d to 3d mode
	 * 
	 * @return
	 */
	private boolean getIs2DTo3D() {
		if (selectedTvManagerMode3DMode == TvManager.SLR_3DMODE_2D_CVT_3D)
			return true;
		return false;
	}

	/**
	 * get the index of vector MODE_3D_TYPES from 3d mode type
	 * 
	 * @param type
	 * @return
	 */
	private int Mode3DTypeIndexOfType(int type) {
		for (int i = 0; i < MODE_3D_TYPES.length; i++) {
			if (MODE_3D_TYPES[i] == type) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * get the display title of 3d mode type
	 * 
	 * @param index
	 * @return
	 */
	private int Mode3DTypeStringOfIndex(int index) {
		int type = TvManagerMode3DTypeOfIndex(index);
		switch (type) {
		case TvManager.SLR_3DMODE_2D:
			return R.string.SLR_3DMODE_2D;
		case TvManager.SLR_3DMODE_3D_AUTO:
			return R.string.SLR_3DMODE_3D_AUTO;
		case TvManager.SLR_3DMODE_3D_SBS:
			return R.string.SLR_3DMODE_3D_SBS;
		case TvManager.SLR_3DMODE_3D_TB:
			return R.string.SLR_3DMODE_3D_TB;
		case TvManager.SLR_3DMODE_3D_FP:
			return R.string.SLR_3DMODE_3D_FP;
		case TvManager.SLR_3DMODE_2D_CVT_3D:
			return R.string.SLR_3DMODE_2D_CVT_3D;
		case TvManager.SLR_3DMODE_3D_LBL:
			return R.string.SLR_3DMODE_3D_LBL;
		case TvManager.SLR_3DMODE_3D_VSTRIP:
			return R.string.SLR_3DMODE_3D_VSTRIP;
		case TvManager.SLR_3DMODE_3D_CKB:
			return R.string.SLR_3DMODE_3D_CKB;
		case TvManager.SLR_3DMODE_3D_REALID:
			return R.string.SLR_3DMODE_3D_REALID;
		case TvManager.SLR_3DMODE_3D_SENSIO:
			return R.string.SLR_3DMODE_3D_SENSIO;
		case TvManager.SLR_3DMODE_3D_AUTO_CVT_2D:
			return R.string.SLR_3DMODE_3D_AUTO_CVT_2D;
		case TvManager.SLR_3DMODE_3D_SBS_CVT_2D:
			return R.string.SLR_3DMODE_3D_SBS_CVT_2D;
		case TvManager.SLR_3DMODE_3D_TB_CVT_2D:
			return R.string.SLR_3DMODE_3D_TB_CVT_2D;
		case TvManager.SLR_3DMODE_3D_FP_CVT_2D:
			return R.string.SLR_3DMODE_3D_FP_CVT_2D;
		case TvManager.SLR_3DMODE_3D_LBL_CVT_2D:
			return R.string.SLR_3DMODE_3D_LBL_CVT_2D;
		case TvManager.SLR_3DMODE_3D_VSTRIP_CVT_2D:
			return R.string.SLR_3DMODE_3D_VSTRIP_CVT_2D;
		case TvManager.SLR_3DMODE_3D_CKB_CVT_2D:
			return R.string.SLR_3DMODE_3D_CKB_CVT_2D;
		case TvManager.SLR_3DMODE_3D_REALID_CVT_2D:
			return R.string.SLR_3DMODE_3D_REALID_CVT_2D;
		case TvManager.SLR_3DMODE_3D_SENSIO_CVT_2D:
			return R.string.SLR_3DMODE_3D_SENSIO_CVT_2D;
		case TvManager.SLR_3DMODE_DISABLE:
			return R.string.SLR_3DMODE_DISABLE;
		default:
			break;
		}
		return -1;
	}

	/**
	 * get the index of vector MODE_3D_RATIO_TYPES from 3d ratio type
	 * 
	 * @param type
	 * @return
	 */
	private int Mode3DRatioIndexOfType(int type) {
		for (int i = 0; i < MODE_3D_RATIO_TYPES.length; i++) {
			if (MODE_3D_RATIO_TYPES[i] == type) {
				return i;
			}
		}
		return -1;
	}

	// /**
	// * get the display title of 3d ratio type
	// *
	// * @param index
	// * @return
	// */
	// private int Mode3DRatioStringOfIndex(int index) {
	// int type = TvManagerMode3DRatioOfIndex(index);
	// switch (type) {
	// case TvManager.PS_4_3:
	// return R.string.PS_4_3;
	// case TvManager.LB_4_3:
	// return R.string.LB_4_3;
	// case TvManager.Wide_16_9:
	// return R.string.Wide_16_9;
	// case TvManager.Wide_16_10:
	// return R.string.Wide_16_10;
	// case TvManager.SCALER_RATIO_AUTO:
	// return R.string.SCALER_RATIO_AUTO;
	// case TvManager.SCALER_RATIO_4_3:
	// return R.string.SCALER_RATIO_4_3;
	// case TvManager.SCALER_RATIO_16_9:
	// return R.string.SCALER_RATIO_16_9;
	// case TvManager.SCALER_RATIO_14_9:
	// return R.string.SCALER_RATIO_14_9;
	// case TvManager.SCALER_RATIO_LETTERBOX:
	// return R.string.SCALER_RATIO_LETTERBOX;
	// case TvManager.SCALER_RATIO_PANORAMA:
	// return R.string.SCALER_RATIO_PANORAMA;
	// case TvManager.SCALER_RATIO_FIT:
	// return R.string.SCALER_RATIO_FIT;
	// case TvManager.SCALER_RATIO_POINTTOPOINT:
	// return R.string.SCALER_RATIO_POINTTOPOINT;
	// case TvManager.SCALER_RATIO_BBY_AUTO:
	// return R.string.SCALER_RATIO_BBY_AUTO;
	// case TvManager.SCALER_RATIO_BBY_NORMAL:
	// return R.string.SCALER_RATIO_BBY_NORMAL;
	// case TvManager.SCALER_RATIO_BBY_ZOOM:
	// return R.string.SCALER_RATIO_BBY_ZOOM;
	// case TvManager.SCALER_RATIO_BBY_WIDE_1:
	// return R.string.SCALER_RATIO_BBY_WIDE_1;
	// case TvManager.SCALER_RATIO_BBY_WIDE_2:
	// return R.string.SCALER_RATIO_BBY_WIDE_2;
	// case TvManager.SCALER_RATIO_BBY_CINEMA:
	// return R.string.SCALER_RATIO_BBY_CINEMA;
	// case TvManager.SCALER_RATIO_CUSTOM:
	// return R.string.SCALER_RATIO_CUSTOM;
	// case TvManager.SCALER_RATIO_PERSON:
	// return R.string.SCALER_RATIO_PERSON;
	// case TvManager.SCALER_RATIO_CAPTION:
	// return R.string.SCALER_RATIO_CAPTION;
	// case TvManager.SCALER_RATIO_MOVIE:
	// return R.string.SCALER_RATIO_MOVIE;
	// case TvManager.SCALER_RATIO_100:
	// return R.string.SCALER_RATIO_100;
	// case TvManager.SCALER_RATIO_SOURCE:
	// return R.string.SCALER_RATIO_SOURCE;
	// case TvManager.SCALER_RATIO_ZOOM_14_9:
	// return R.string.SCALER_RATIO_ZOOM_14_9;
	// case TvManager.SCALER_RATIO_DISABLE:
	// return R.string.SCALER_RATIO_DISABLE;
	// default:
	// break;
	// }
	// return -1;
	// }

	/**
	 * get the display title of 3d ratio type
	 * 
	 * @param index
	 * @return
	 */
	private int Mode3DRatioStringOfIndex(int index) {
		int type = TvManagerMode3DRatioOfIndex(index);
		switch (type) {
		case TvManager.SLR_RATIO_AUTO:
			return R.string.SLR_RATIO_AUTO;
		case TvManager.SLR_RATIO_4_3:
			return R.string.SLR_RATIO_4_3;
		case TvManager.SLR_RATIO_16_9:
			return R.string.SLR_RATIO_16_9;
		case TvManager.SLR_RATIO_14_9:
			return R.string.SLR_RATIO_14_9;
		case TvManager.SLR_RATIO_LETTERBOX:
			return R.string.SLR_RATIO_LETTERBOX;
		case TvManager.SLR_RATIO_PANORAMA:
			return R.string.SLR_RATIO_PANORAMA;
		case TvManager.SLR_RATIO_FIT:
			return R.string.SLR_RATIO_FIT;
		case TvManager.SLR_RATIO_POINTTOPOINT:
			return R.string.SLR_RATIO_POINTTOPOINT;
		case TvManager.SLR_RATIO_BBY_AUTO:
			return R.string.SLR_RATIO_BBY_AUTO;
		case TvManager.SLR_RATIO_BBY_NORMAL:
			return R.string.SLR_RATIO_BBY_NORMAL;
		case TvManager.SLR_RATIO_BBY_ZOOM:
			return R.string.SLR_RATIO_BBY_ZOOM;
		case TvManager.SLR_RATIO_BBY_WIDE_1:
			return R.string.SLR_RATIO_BBY_WIDE_1;
		case TvManager.SLR_RATIO_BBY_WIDE_2:
			return R.string.SLR_RATIO_BBY_WIDE_2;
		case TvManager.SLR_RATIO_BBY_CINEMA:
			return R.string.SLR_RATIO_BBY_CINEMA;
		case TvManager.SLR_RATIO_CUSTOM:
			return R.string.SLR_RATIO_CUSTOM;
		case TvManager.SLR_ASPECT_RATIO_PERSON:
			return R.string.SLR_ASPECT_RATIO_PERSON;
		case TvManager.SLR_ASPECT_RATIO_CAPTION:
			return R.string.SLR_ASPECT_RATIO_CAPTION;
		case TvManager.SLR_ASPECT_RATIO_MOVIE:
			return R.string.SLR_ASPECT_RATIO_MOVIE;
		case TvManager.SLR_ASPECT_RATIO_100:
			return R.string.SLR_ASPECT_RATIO_100;
		case TvManager.SLR_ASPECT_RATIO_SOURCE:
			return R.string.SLR_ASPECT_RATIO_SOURCE;
		case TvManager.SLR_RATIO_ZOOM_14_9:
			return R.string.SLR_RATIO_ZOOM_14_9;
		default:
			break;
		}
		return -1;
	}

	/**
	 * get the display title of 3d mode lrswap
	 * 
	 * @param index
	 * @return
	 */
	private int Mode3DOnOffStringOfIndex(int index) {
		int typeLength = MODE_3D_ONOFF_TYPES.length;
		while (index < 0)
			index += typeLength;
		while (index >= typeLength)
			index -= typeLength;
		return MODE_3D_ONOFF_TYPES[index];
	}

	/**
	 * get the display title of 3d cvrt 2d
	 * 
	 * @param index
	 * @return
	 */
	private int Mode3DCVRT2DStringOfIndex(int index) {
		int typeLength = MODE_3D_3DCVRT2D_TYPES.length;
		while (index < 0)
			index += typeLength;
		while (index >= typeLength)
			index -= typeLength;
		return MODE_3D_3DCVRT2D_TYPES[index];
	}

	private void SendMessageDelayed(int messageType, int delay) {
		mode3DSettingMessageHandler.removeMessages(messageType);
		mode3DSettingMessageHandler.sendMessageDelayed(
				mode3DSettingMessageHandler.obtainMessage(messageType), delay);
	}

	private static class Mode3DSettingMessageHandler extends Handler {
		final WeakReference<FragmentMode3DSetting> fragmentMode3DSettingReference;

		public Mode3DSettingMessageHandler(
				FragmentMode3DSetting fragmentMode3DSetting) {
			fragmentMode3DSettingReference = new WeakReference<FragmentMode3DSetting>(
					fragmentMode3DSetting);
		}

		public void handleMessage(android.os.Message msg) {
			FragmentMode3DSetting fragmentMode3DSetting = fragmentMode3DSettingReference
					.get();
			if (fragmentMode3DSetting == null)
				return;
			switch (msg.what) {
			case MODE_3D_SETTING_ITEM_MODE_CHANGE: {
				fragmentMode3DSetting.set3DMode(selectedTvManagerMode3DMode);
				mode3DEnabled = fragmentMode3DSetting.getIs3D();
			}
				break;
			case MODE_3D_SETTING_ITEM_LRSWAP_CHANGE: {
				fragmentMode3DSetting
						.set3DLRSwap(selectedTvManagerMode3DLRSwap);
			}
				break;
			case MODE_3D_SETTING_ITEM_DEEP_CHANGE: {
				fragmentMode3DSetting.set3DDeep(selectedTvManagerMode3DDeep);
			}
				break;
			case MODE_3D_SETTING_ITEM_STRENGTH_CHANGE: {
				fragmentMode3DSetting
						.set3DStrength(selectedTvManagerMode3DStrength);
			}
				break;
			case MODE_3D_SETTING_ITEM_3DCVRT2D_CHANGE: {
				fragmentMode3DSetting
						.set3DCVRT2D(selectedTvManagerMode3DCVRT2D);
			}
				break;
			case MODE_3D_SETTING_ITEM_RATIO_CHANGE: {
				fragmentMode3DSetting.set3DRatio(selectedTvManagerMode3DRatio);
			}
				break;
			case MODE_3D_SETTING_ITEM_MUTE_CHANGE: {
				fragmentMode3DSetting.set3DMute(selectedTvManagerMode3DMute);
			}
				break;
			case POP_SELF_FROM_FRAGMENT_STACK: {
				fragmentMode3DSetting.PopSelfFromFragmentStack();
			}
				break;
			default:
				break;
			}
			fragmentMode3DSetting.UpdateListView();
		};
	};

	private class Mode3DSettingListViewAdapter extends BaseAdapter {
		private float DEFAULT_MIN_TEXT_SIZE = 8;
		private float DEFAULT_MAX_TEXT_SIZE = 14;
		private int DEFAULT_TEXTVIEW_WIDTH = 100;

		private Context context;
		private LayoutInflater listContainer;
		private int selectedModeIndex;
		private int selectedModeLRSwap;
		private int selectedModeDeep;
		private int selectedModeStrength;
		private int selectedMode3DCVRT2D;
		private int selectedMode3DRatio;
		private int selectedModeMute;

		public Mode3DSettingListViewAdapter(Context context) {
			this.context = context;
			listContainer = LayoutInflater.from(this.context);
		}

		public void setSelectedModeIndex(int index) {
			selectedModeIndex = index;
		}

		public void setSelectedModeDeep(int value) {
			selectedModeDeep = value;
		}

		public void setSelectedModeStrength(int value) {
			selectedModeStrength = value;
		}

		public void setSelectedModeLRSwap(int value) {
			selectedModeLRSwap = value;
		}

		public void setSelectedMode3DCVRT2D(int value) {
			selectedMode3DCVRT2D = value;
		}

		public void setSelectedModeRatio(int value) {
			selectedMode3DRatio = value;
		}

		public void setSelectedModeMute(int value) {
			selectedModeMute = value;
		}

		@Override
		public int getCount() {
			return MODE_3D_SETTING_ITEM_COUNT;
		}

		@Override
		public int getViewTypeCount() {
			return MODE_3D_SETTING_ITEM_TYPE_COUNT;
		}

		@Override
		public int getItemViewType(int position) {
			switch (position) {
			case MODE_3D_SETTING_ITEM_MODE:
			case MODE_3D_SETTING_ITEM_LRSWAP:
			case MODE_3D_SETTING_ITEM_3DCVRT2D:
			case MODE_3D_SETTING_ITEM_RATIO:
			case MODE_3D_SETTING_ITEM_MUTE:
				return MODE_3D_SETTING_ITEM_TYPE_SPINNER;
			case MODE_3D_SETTING_ITEM_DEEP:
			case MODE_3D_SETTING_ITEM_STRENGTH:
				return MODE_3D_SETTING_ITEM_TYPE_SEEKBAR;
			default:
				break;
			}
			return -1;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			SpinnerItemViewHolder spinnerItemViewHolder = null;
			SeekbarItemViewHolder seekbarItemViewHolder = null;

			if (convertView == null) {

				switch (getItemViewType(position)) {
				case MODE_3D_SETTING_ITEM_TYPE_SPINNER: {
					convertView = listContainer.inflate(
							R.layout.item_mode_3d_setting_spinner, null);
					spinnerItemViewHolder = new SpinnerItemViewHolder();
					spinnerItemViewHolder.titleTextView = ((TextView) (convertView
							.findViewById(R.id.item_mode_3d_setting_spinner_title)));
					spinnerItemViewHolder.prevSpinnerTextView = ((TextView) (convertView
							.findViewById(R.id.item_mode_3d_setting_spinner_prev)));
					spinnerItemViewHolder.selectSpinnerTextView = ((TextView) (convertView
							.findViewById(R.id.item_mode_3d_setting_spinner_selected)));
					spinnerItemViewHolder.nextSpinnerTextView = ((TextView) (convertView
							.findViewById(R.id.item_mode_3d_setting_spinner_next)));
					convertView.setTag(spinnerItemViewHolder);
				}
					break;
				case MODE_3D_SETTING_ITEM_TYPE_SEEKBAR: {
					convertView = listContainer.inflate(
							R.layout.item_mode_3d_setting_seekbar, null);
					seekbarItemViewHolder = new SeekbarItemViewHolder();
					seekbarItemViewHolder.titleTextView = ((TextView) (convertView
							.findViewById(R.id.item_mode_3d_setting_seekbar_title)));
					seekbarItemViewHolder.seekBar = ((SeekBar) (convertView
							.findViewById(R.id.item_mode_3d_setting_seekbar_seekbar)));
					seekbarItemViewHolder.seekBarTextView = ((TextView) (convertView
							.findViewById(R.id.item_mode_3d_setting_seekbar_num)));
					convertView.setTag(seekbarItemViewHolder);
				}
					break;
				default:
					return new View(null);
				}
			}
			switch (position) {
			case MODE_3D_SETTING_ITEM_MODE: {
				spinnerItemViewHolder = (SpinnerItemViewHolder) convertView
						.getTag();
				spinnerItemViewHolder.titleTextView
						.setText(R.string.TITLE_3DMODE_MODE);
				spinnerItemViewHolder.prevSpinnerTextView
						.setText(Mode3DTypeStringOfIndex(selectedModeIndex - 1));
				spinnerItemViewHolder.selectSpinnerTextView
						.setText(Mode3DTypeStringOfIndex(selectedModeIndex));
				spinnerItemViewHolder.nextSpinnerTextView
						.setText(Mode3DTypeStringOfIndex(selectedModeIndex + 1));
				refitText(spinnerItemViewHolder.prevSpinnerTextView);
				refitText(spinnerItemViewHolder.selectSpinnerTextView);
				refitText(spinnerItemViewHolder.nextSpinnerTextView);
			}
				break;
			case MODE_3D_SETTING_ITEM_LRSWAP: {
				spinnerItemViewHolder = (SpinnerItemViewHolder) convertView
						.getTag();
				spinnerItemViewHolder.titleTextView
						.setText(R.string.TITLE_3DMODE_LRSWAP);
				spinnerItemViewHolder.prevSpinnerTextView
						.setText(Mode3DOnOffStringOfIndex(selectedModeLRSwap - 1));
				spinnerItemViewHolder.selectSpinnerTextView
						.setText(Mode3DOnOffStringOfIndex(selectedModeLRSwap));
				spinnerItemViewHolder.nextSpinnerTextView
						.setText(Mode3DOnOffStringOfIndex(selectedModeLRSwap + 1));
				// refitText(spinnerItemViewHolder.prevSpinnerTextView);
				// refitText(spinnerItemViewHolder.selectSpinnerTextView);
				// refitText(spinnerItemViewHolder.nextSpinnerTextView);

				spinnerItemViewHolder.setEnable(mode3DEnabled);
			}
				break;
			case MODE_3D_SETTING_ITEM_DEEP: {
				seekbarItemViewHolder = (SeekbarItemViewHolder) convertView
						.getTag();
				seekbarItemViewHolder.titleTextView
						.setText(R.string.TITLE_3DMODE_DEEP);
				seekbarItemViewHolder.seekBar
						.setMax(MODE_3D_SETTING_ITEM_DEEP_RANGE);
				seekbarItemViewHolder.seekBar.setProgress(selectedModeDeep
						+ MODE_3D_SETTING_ITEM_DEEP_OFFSET);
				seekbarItemViewHolder.seekBarTextView.setText(String
						.valueOf(selectedModeDeep));

				seekbarItemViewHolder.setEnable(mode3DEnabled);
			}
				break;
			case MODE_3D_SETTING_ITEM_STRENGTH: {
				seekbarItemViewHolder = (SeekbarItemViewHolder) convertView
						.getTag();
				seekbarItemViewHolder.titleTextView
						.setText(R.string.TITLE_3DMODE_STRENGTH);
				seekbarItemViewHolder.seekBar
						.setMax(MODE_3D_SETTING_ITEM_STRENGTH_RANGE);
				seekbarItemViewHolder.seekBar.setProgress(selectedModeStrength
						+ MODE_3D_SETTING_ITEM_STRENGTH_OFFSET);
				seekbarItemViewHolder.seekBarTextView.setText(String
						.valueOf(selectedModeStrength));

				seekbarItemViewHolder.setEnable(getIs2DTo3D());
			}
				break;
			case MODE_3D_SETTING_ITEM_3DCVRT2D: {
				spinnerItemViewHolder = (SpinnerItemViewHolder) convertView
						.getTag();
				spinnerItemViewHolder.titleTextView
						.setText(R.string.TITLE_3DMODE_3DCVRT2D);
				spinnerItemViewHolder.prevSpinnerTextView
						.setText(Mode3DCVRT2DStringOfIndex(selectedMode3DCVRT2D - 1));
				spinnerItemViewHolder.selectSpinnerTextView
						.setText(Mode3DCVRT2DStringOfIndex(selectedMode3DCVRT2D));
				spinnerItemViewHolder.nextSpinnerTextView
						.setText(Mode3DCVRT2DStringOfIndex(selectedMode3DCVRT2D + 1));
				// refitText(spinnerItemViewHolder.prevSpinnerTextView);
				// refitText(spinnerItemViewHolder.selectSpinnerTextView);
				// refitText(spinnerItemViewHolder.nextSpinnerTextView);

				spinnerItemViewHolder.setEnable(mode3DEnabled);
			}
				break;
			case MODE_3D_SETTING_ITEM_RATIO: {
				spinnerItemViewHolder = (SpinnerItemViewHolder) convertView
						.getTag();
				spinnerItemViewHolder.titleTextView
						.setText(R.string.TITLE_3DMODE_RATIO);
				spinnerItemViewHolder.prevSpinnerTextView
						.setText(Mode3DRatioStringOfIndex(selectedMode3DRatio - 1));
				spinnerItemViewHolder.selectSpinnerTextView
						.setText(Mode3DRatioStringOfIndex(selectedMode3DRatio));
				spinnerItemViewHolder.nextSpinnerTextView
						.setText(Mode3DRatioStringOfIndex(selectedMode3DRatio + 1));
				refitText(spinnerItemViewHolder.prevSpinnerTextView);
				refitText(spinnerItemViewHolder.selectSpinnerTextView);
				refitText(spinnerItemViewHolder.nextSpinnerTextView);
			}
				break;
			case MODE_3D_SETTING_ITEM_MUTE: {
				spinnerItemViewHolder = (SpinnerItemViewHolder) convertView
						.getTag();
				spinnerItemViewHolder.titleTextView
						.setText(R.string.TITLE_3DMODE_MUTE);
				spinnerItemViewHolder.prevSpinnerTextView
						.setText(Mode3DOnOffStringOfIndex(selectedModeMute - 1));
				spinnerItemViewHolder.selectSpinnerTextView
						.setText(Mode3DOnOffStringOfIndex(selectedModeMute));
				spinnerItemViewHolder.nextSpinnerTextView
						.setText(Mode3DOnOffStringOfIndex(selectedModeMute + 1));
				// refitText(spinnerItemViewHolder.prevSpinnerTextView);
				// refitText(spinnerItemViewHolder.selectSpinnerTextView);
				// refitText(spinnerItemViewHolder.nextSpinnerTextView);
			}
				break;
			default:
				break;
			}
			return convertView;
		}

		/**
		 * change the text size to fit the textView
		 * 
		 * @param textView
		 */
		private void refitText(TextView textView) {
			String textString = textView.getText().toString();
			int textWidth = DEFAULT_TEXTVIEW_WIDTH;
			if (textWidth > 0) {
				float trySize = DEFAULT_MAX_TEXT_SIZE;
				Paint testPaint = textView.getPaint();
				testPaint.setTextSize(trySize);
				while ((trySize > DEFAULT_MIN_TEXT_SIZE)
						&& (testPaint.measureText(textString) > textWidth)) {
					trySize -= 1;
					if (trySize <= DEFAULT_MIN_TEXT_SIZE) {
						trySize = DEFAULT_MIN_TEXT_SIZE;
						break;
					}
					testPaint.setTextSize(trySize);
				}
				textView.setTextSize(trySize);
			}
		}

		private class SpinnerItemViewHolder {
			TextView titleTextView;
			TextView prevSpinnerTextView;
			TextView selectSpinnerTextView;
			TextView nextSpinnerTextView;

			public void setEnable(boolean enabled) {
				titleTextView.setEnabled(enabled);
				prevSpinnerTextView.setEnabled(enabled);
				selectSpinnerTextView.setEnabled(enabled);
				nextSpinnerTextView.setEnabled(enabled);
			}
		}

		private class SeekbarItemViewHolder {
			TextView titleTextView;
			SeekBar seekBar;
			TextView seekBarTextView;

			public void setEnable(boolean enabled) {
				titleTextView.setEnabled(enabled);
				seekBar.setEnabled(enabled);
				seekBarTextView.setEnabled(enabled);
			}
		}
	}

}
