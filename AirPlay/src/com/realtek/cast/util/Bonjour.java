
package com.realtek.cast.util;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

abstract public class Bonjour {
	
	private static final int MSG_START = 0;
	private static final int MSG_REFRESH = 1;
	private static final int MSG_STOP = 2;
	
	abstract protected ServiceInfo onCreateServiceInfo(byte[] hwAddr, String name, int port);
	
//	private final List<JmDNS> mListDns = new ArrayList<JmDNS>();
	private final Map<InetAddress, JmDNS> mAddr2Dns = new HashMap<InetAddress, JmDNS>();
	private final String name;
	private final int port;
	private final HandlerThread mThread;
	private final Handler mHandler;
	
	public Bonjour(String name, int port) {
		this.name = name;
		this.port = port;
		mThread = new HandlerThread(String.format("Bonjour:%s:%d", name, port));
		mThread.start();
		mHandler = new BonjourHandler(mThread.getLooper());
	}
	
	private final class BonjourHandler extends Handler {

		public BonjourHandler(Looper looper) {
			super(looper);
        }

		@Override
        public void handleMessage(Message msg) {
	        super.handleMessage(msg);
	        switch(msg.what) {
	        	case MSG_START:
	        		internalRefresh();
	        		break;
	        	case MSG_REFRESH:
	        		internalRefresh();
	        		break;
	        	case MSG_STOP:
	        		internalStop();
	        		break;
				default:
					break;
	        }
        }
		
	}
	
	public void start() {
		mHandler.obtainMessage(MSG_START).sendToTarget();
	}

	public void stopBonjour() {
		mHandler.obtainMessage(MSG_STOP).sendToTarget();
	}

	public void refresh() {
		mHandler.obtainMessage(MSG_REFRESH).sendToTarget();
	}
	
	private void internalRefresh() {
		try{
			List<InetAddress> addresss = new ArrayList<InetAddress>();
			
			Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			while (networks.hasMoreElements()) {
				NetworkInterface ni = networks.nextElement();
				if (ni.isLoopback()) {
					continue;
				}
				if (ni.isPointToPoint()) {
					continue;
				}
				if (!ni.isUp()) {
					continue;
				}
				
				byte[] mac = ni.getHardwareAddress();
				if (mac == null) {
					continue;
				}
				
				Enumeration<InetAddress> addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					InetAddress addr = addrs.nextElement();
					// Support only ipv4 for now
					if (!(addr instanceof Inet4Address)) {
						continue;
					}
					addresss.add(addr);
					JmDNS dns = mAddr2Dns.get(addr);
					if (dns == null) {
						ServiceInfo info = onCreateServiceInfo(mac, name, port);
						dns= JmDNS.create(addr);
						dns.registerService(info);
						mAddr2Dns.put(addr, dns);
					}
				}
			}
			
			// Remove old
			Set<InetAddress> dnsAddrs = mAddr2Dns.keySet();
			Iterator<InetAddress> it = dnsAddrs.iterator();
			while (it.hasNext()) {
				InetAddress a = it.next();
				if (!addresss.contains(a)) {
					mAddr2Dns.get(a).close();
					it.remove();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Zeroconf registration
    }
	
	public void internalStop() {
		Collection<JmDNS> dnss = mAddr2Dns.values();
		for (JmDNS d : dnss) {
			d.unregisterAllServices();
			try {
				d.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mAddr2Dns.clear();
    }
}
