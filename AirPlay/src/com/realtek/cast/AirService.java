
package com.realtek.cast;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import com.realtek.cast.airplay.AirPlayServer;
import com.realtek.cast.airtunes.AirTunesServer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class AirService extends Service {
	
	public static final String TAG = "AirService";

	public static final boolean VERBOSE = true;
	
	public static boolean hasStarted() {
	    return sInstance != null;
    }
	
	public static void refreshService(Context context) {
		if (sInstance == null) {
			if (AppPreference.getInstance(context).isAirPlayEnabled()) {
				startService(context);
			}
			return;
		}
		
		sInstance.mAirPlay.refreshBonjour();
	}
	
	public static void startService(Context context) {
		Intent intent = new Intent(context, AirService.class);
		context.getApplicationContext().startService(intent);
    }

	public static void stopService(Context context) {
		Intent intent = new Intent(context, AirService.class);
	    context.getApplicationContext().stopService(intent);
    }
	
	private static AirService sInstance;

	private WifiManager.MulticastLock mMulticastLock;
	private PrivateKey mKey;
	
	private AirTunesServer mAirTunes;
	private AirPlayServer mAirPlay;
	
	@Override
    public IBinder onBind(Intent intent) {
	    return null;
    }

	@Override
    public void onCreate() {
	    super.onCreate();
	    sInstance = this;
	    
	    // init key
        try {
        	Resources resources = getResources();  
        	InputStream is = resources.openRawResource(R.raw.key);
        	byte[] b = new byte[4096];
            int read;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((read = is.read(b, 0, b.length)) > 0) {
                out.write(b, 0, read);
            }
        	mKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(out.toByteArray()));
        } catch (Exception e) {
        	e.printStackTrace();
        	stopSelf();
        	return;
        }
	    
        WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
        mMulticastLock = wifi.createMulticastLock(TAG);
        mMulticastLock.setReferenceCounted(true);
        mMulticastLock.acquire();
        
        String serviceName = getDeviceName();
        mAirTunes = new AirTunesServer(serviceName, mKey);
        mAirTunes.start();
        
        mAirPlay = new AirPlayServer(serviceName, mKey);
        mAirPlay.start();

        Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification n = new Notification.Builder(this)
		.setSmallIcon(R.drawable.ic_stat_cast)
		.setContentTitle(getString(R.string.airplay))
		.setContentText(serviceName)
		.setContentIntent(pi)
		.setOngoing(true)
		.setAutoCancel(false)
		.build();
		startForeground(CastApplication.NOTIFICATION_ID, n);
    }

	private String getDeviceName() {
		return AppPreference.getInstance(this).getAirPlayServiceName();
    }

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	    return START_STICKY;
    }

	@Override
    public void onDestroy() {
	    super.onDestroy();
	    if (mAirTunes != null) {
	    	mAirTunes.close();
	    }
	    
	    if (mAirPlay != null) {
	    	mAirPlay.close();
	    }
	    
        if (mMulticastLock != null) {
            mMulticastLock.release();
        }
        
        sInstance = null;
    }

}
