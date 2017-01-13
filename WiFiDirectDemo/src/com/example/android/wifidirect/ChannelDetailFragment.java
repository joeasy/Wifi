/*

 */

package com.example.android.wifidirect;

import android.app.Fragment;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView.OnEditorActionListener; 
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;


public class ChannelDetailFragment extends Fragment{
	View mContentView = null;
	
	private static final String[] lChannel={"null","1","6","11"};
	private static final String[] opChannel={"null","1","2","3","4","5","6","7","8","9","10","11","36"};
	private static final String[] opChannelArray={"null","2412","2417","2422","2427","2432","2437","2442","2447","2452","2457","2462","5180"};
	private static final String[] goIntend={"null","0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15"};
	private TextView deviceNameView ;
	private TextView cmdView ;
	private TextView persistentView ;
	private TextView lChannelView ;
	private TextView opChannelView ;
	private TextView goIntendView ;
	private EditText deviceNameEdit;
	private EditText cmdEdit;
	private Button persistentBtn;
	private Spinner lChannelSpinner;
	private Spinner opChannelSpinner;
	private Spinner goIntendSpinner;
	private ArrayAdapter<String> lChannelAdapter;
	private ArrayAdapter<String> opChannelAdapter;
	private ArrayAdapter<String> goIntendAdapter;
	private boolean firstCtreat = false;
	private static final boolean DEBUG = false;

	private int mListenChannel;
    private int mOperatingChannel;
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.channel_detail, null);

		// Device Name
        deviceNameView = (TextView)mContentView.findViewById(R.id.device_name);
		deviceNameView.setText("Device Name:");
		deviceNameEdit = (EditText)mContentView.findViewById(R.id.edit01);
		deviceNameEdit.setOnEditorActionListener(new OnEditorActionListener() {  
			@Override  
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {  
				if(6 == actionId) {
					String str = v.getText().toString();
					((ChannelActionListener)getActivity()).setDeviceName(str);
					Toast.makeText(getActivity(), "Setting Device Name :" + v.getText().toString(), Toast.LENGTH_SHORT).show();  
				}
				return false;  
			}  
		});  

		// cmd
        cmdView = (TextView)mContentView.findViewById(R.id.cmd);
		cmdView.setText("CMD:");
		cmdEdit = (EditText)mContentView.findViewById(R.id.edit02);
		cmdEdit.setOnEditorActionListener(new OnEditorActionListener() {  
			@Override  
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {  
				if(6 == actionId) {
					String str = v.getText().toString();
					((ChannelActionListener) getActivity()).cmd(str, new ActionListener() {
						@Override
						public void onSuccess() {
							Log.d("lynn", "cmd exec cmd onSuccess");
						}
						@Override
						public void onFailure(int reason) {
							Log.e("lynn", "cmd exec cmd onFailure");
						}
					});
				}
				return false;  
			}  
		});  

		// persistent
        persistentView = (TextView)mContentView.findViewById(R.id.persistent);
		persistentView.setText("ONLY FOR 5.1.22: auth ON/OFF:");
		persistentBtn = (Button)mContentView.findViewById(R.id.btn01);
		persistentBtn.setText("OFF");
		persistentBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "SET_AUTH "; 
				if(((Button)v).getText().equals("OFF")) {
					str += "on";
					((Button)v).setText("ON");
				} else {
					str += "off";
					((Button)v).setText("OFF");
				}
				((ChannelActionListener) getActivity()).cmd(str, new ActionListener() {
						@Override
						public void onSuccess() {
						Log.d("lynn", "auth exec cmd onSuccess");
					}
						@Override
						public void onFailure(int reason) {
						Log.e("lynn", "auth exec cmd onFailure");
					}
				});
			}
		});
        
        // Listen Channel
        lChannelView = (TextView)mContentView.findViewById(R.id.listen_channel);
        lChannelSpinner = (Spinner)mContentView.findViewById(R.id.Spinner01);
        
        lChannelAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,lChannel);
        lChannelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lChannelSpinner.setAdapter(lChannelAdapter);
        lChannelSpinner.setOnItemSelectedListener(new SpinnerSelectedListener01());
        lChannelSpinner.setVisibility(View.VISIBLE);
        
        //Op Channel
        opChannelView = (TextView)mContentView.findViewById(R.id.op_channel);
        opChannelSpinner = (Spinner)mContentView.findViewById(R.id.Spinner02);
        
        opChannelAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,opChannel);
        opChannelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        opChannelSpinner.setAdapter(opChannelAdapter);
        opChannelSpinner.setOnItemSelectedListener(new SpinnerSelectedListener02());
        opChannelSpinner.setVisibility(View.VISIBLE);
        
        //Go Intend
        goIntendView = (TextView)mContentView.findViewById(R.id.go_intend);
        goIntendSpinner = (Spinner)mContentView.findViewById(R.id.Spinner03);
        
        goIntendAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,goIntend);
        goIntendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goIntendSpinner.setAdapter(goIntendAdapter);
        goIntendSpinner.setSelection(8,true);
        goIntendView.setText("Go Intend: 7");
        firstCtreat = true;
        goIntendSpinner.setOnItemSelectedListener(new SpinnerSelectedListener03());
        goIntendSpinner.setVisibility(View.VISIBLE);
        
        return mContentView;
    }
	
	class SpinnerSelectedListener01 implements OnItemSelectedListener{

		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			lChannelView.setText("Listen Channel: "+lChannel[arg2]);
			
			if(lChannel[arg2] == "null")
				return;
			
			Toast.makeText(getActivity(), "Listen Channel: "+lChannel[arg2],
                    Toast.LENGTH_SHORT).show();
			
			mListenChannel = Integer.parseInt(lChannel[arg2]);
			((ChannelActionListener) getActivity()).setWifiP2pChannels(mListenChannel, mOperatingChannel);
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}
	
	class SpinnerSelectedListener02 implements OnItemSelectedListener{

		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			opChannelView.setText("Op Channel: "+opChannel[arg2]);
			
			if(opChannel[arg2] == "null")
				return;
			
			Toast.makeText(getActivity(), "Op Channel: "+opChannel[arg2],
            		Toast.LENGTH_SHORT).show();
			
			int num = Integer.parseInt(opChannel[arg2]);
			String str = null;
			String strValue = null;
			if(num <= 11)
				strValue = opChannelArray[num];
			else if(num == 36)
				strValue = opChannelArray[12];
			str = "OP_CHANNEL " + strValue;
			
			mOperatingChannel = Integer.parseInt(strValue);
			((ChannelActionListener) getActivity()).setWifiP2pChannels(mListenChannel, mOperatingChannel);
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}
	
	class SpinnerSelectedListener03 implements OnItemSelectedListener{

		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			goIntendView.setText("Go Intend: "+goIntend[arg2]);
			
			if(goIntend[arg2] == "null" || firstCtreat == true) {
				firstCtreat = false;
				return;
			}

			Toast.makeText(getActivity(), "Go Intend: "+goIntend[arg2],
            		Toast.LENGTH_SHORT).show();
			
			((ChannelActionListener) getActivity()).setGroupOwnerIntent(Integer.parseInt(goIntend[arg2]));
			
			String str = "SET p2p_go_intent " + goIntend[arg2];
			((ChannelActionListener) getActivity()).cmd(str, new ActionListener() {
				@Override
				public void onSuccess() {
					Log.d("lynn", "go intend exec cmd onSuccess");
				}
				@Override
				public void onFailure(int reason) {
					Log.e("lynn", "go intend exec cmd onFailure");
				}
			});
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}

	/**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface ChannelActionListener {
    	
    	void setGroupOwnerIntent(int num);
    	
        void cmd(String str, ActionListener listener);

		void setDeviceName(String str);

		void setWifiP2pChannels(final int lc, final int oc);
    }

}
